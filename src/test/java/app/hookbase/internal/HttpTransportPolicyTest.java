package app.hookbase.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.hookbase.RequestOptions;
import app.hookbase.errors.HookbaseApiException;
import app.hookbase.errors.HookbaseNetworkException;
import app.hookbase.errors.HookbaseTimeoutException;
import app.hookbase.errors.RateLimitException;
import app.hookbase.errors.ValidationException;
import app.hookbase.testing.MockServer;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The retry policy shared with the other SDKs, edge by edge: which failures are retried for which
 * methods, how long each wait is, and what the caller sees when the budget runs out.
 */
class HttpTransportPolicyTest {

    private MockServer server;
    private final List<Duration> sleeps = new ArrayList<>();

    @BeforeEach
    void start() {
        server = new MockServer();
    }

    @AfterEach
    void stop() {
        server.close();
    }

    private HttpTransport transport(String baseUrl, int retries, Duration timeout, boolean debug) {
        return new HttpTransport("whr_secret", baseUrl, timeout, retries, HttpClient.newHttpClient(), debug, sleeps::add);
    }

    private HttpTransport transport(int retries) {
        return transport(server.url(), retries, Duration.ofSeconds(5), false);
    }

    // ---- 5xx backoff ----------------------------------------------------------------------------

    @Test
    void backoffDoublesFromOneSecondAndCapsAtTenWithUnderASecondOfJitter() {
        server.otherwise(503, "{\"error\":\"unavailable\"}");
        assertThatThrownBy(() -> transport(5).request("GET", "/api/x", null, null, null))
                .isInstanceOf(HookbaseApiException.class);

        assertThat(sleeps).hasSize(5);
        long[] base = {1000, 2000, 4000, 8000, 10_000};
        for (int i = 0; i < base.length; i++) {
            assertThat(sleeps.get(i).toMillis()).as("wait %d", i).isBetween(base[i], base[i] + 999);
        }
        assertThat(server.requests()).hasSize(6);
    }

    @Test
    void everyNonPostMethodIsRetriedOn5xx() {
        for (String method : List.of("GET", "PUT", "PATCH", "DELETE")) {
            sleeps.clear();
            server.enqueue(502, "").enqueue(200, "{\"ok\":true}");
            assertThat(transport(3).request(method, "/api/x", null, method.equals("GET") ? null : Map.of(), null)
                    .get("ok").asBoolean()).isTrue();
            assertThat(sleeps).as(method).hasSize(1);
        }
    }

    @Test
    void v1EventsPostWithIdempotencyKeyIsRetriedOn5xx() {
        server.enqueue(500, "{}").enqueue(200, "{\"ok\":true}");
        transport(3).request("POST", "/v1/events", null, Map.of(), new RequestOptions(null, null, "key-1"));
        assertThat(server.requests()).hasSize(2);
        assertThat(server.requests()).allSatisfy(r -> assertThat(r.header("Idempotency-Key")).isEqualTo("key-1"));
    }

    @Test
    void explicitZeroRetriesDisablesRetryingForThatCall() {
        server.otherwise(503, "{}");
        assertThatThrownBy(() -> transport(3).request("GET", "/api/x", null, null, new RequestOptions(null, 0, null)))
                .isInstanceOf(HookbaseApiException.class);
        assertThat(server.requests()).hasSize(1);
        assertThat(sleeps).isEmpty();
    }

    // ---- 429 ------------------------------------------------------------------------------------

    @Test
    void retryAfterZeroIsRetriedAtOnceAndSharesTheBudgetWith5xx() {
        server.enqueue(429, "{\"error\":\"slow down\"}", Map.of("Retry-After", "0"))
                .enqueue(503, "{}")
                .enqueue(200, "{\"ok\":true}");
        transport(2).request("GET", "/api/things", null, null, new RequestOptions(null, 2, null));
        assertThat(sleeps.getFirst()).isEqualTo(Duration.ZERO);
        assertThat(sleeps).hasSize(2);
    }

    @Test
    void retryAfterAsAnHttpDateIsWaitedFor() {
        String inFive = java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(
                java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).plusSeconds(5));
        server.enqueue(429, "{}", Map.of("Retry-After", inFive)).enqueue(200, "{}");
        transport(1).request("GET", "/api/x", null, null, null);
        assertThat(sleeps).hasSize(1);
        assertThat(sleeps.getFirst()).isBetween(Duration.ofSeconds(3), Duration.ofSeconds(5));
    }

    @Test
    void rateLimitWithoutRetryAfterCarriesNullAndTheRequestId() {
        server.enqueue(429, "{\"error\":\"Monthly limit exceeded\",\"current\":10,\"max\":10}", Map.of("x-request-id", "req_9"));
        assertThatThrownBy(() -> transport(3).request("GET", "/api/x", null, null, null))
                .isInstanceOfSatisfying(RateLimitException.class, e -> {
                    assertThat(e.retryAfter()).isNull();
                    assertThat(e.status()).isEqualTo(429);
                    assertThat(e.requestId()).isEqualTo("req_9");
                    assertThat(e.getMessage()).isEqualTo("Monthly limit exceeded");
                });
        assertThat(sleeps).isEmpty();
    }

    // ---- timeouts and dropped connections -----------------------------------------------------

    @Test
    void timeoutIsRetriedForGet() throws IOException {
        try (ServerSocket silent = new ServerSocket(0)) {
            HttpTransport t = transport("http://127.0.0.1:" + silent.getLocalPort(), 2, Duration.ofMillis(200), false);
            assertThatThrownBy(() -> t.request("GET", "/api/x", null, null, null)).isInstanceOf(HookbaseTimeoutException.class);
            assertThat(sleeps).hasSize(2);
        }
    }

    @Test
    void perRequestTimeoutOverridesTheClientDefault() throws IOException {
        try (ServerSocket silent = new ServerSocket(0)) {
            HttpTransport t = transport("http://127.0.0.1:" + silent.getLocalPort(), 0, Duration.ofSeconds(30), false);
            long start = System.nanoTime();
            assertThatThrownBy(() -> t.request("GET", "/api/x", null, null, new RequestOptions(Duration.ofMillis(200), null, null)))
                    .isInstanceOf(HookbaseTimeoutException.class)
                    .hasMessageContaining("200ms");
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(10));
        }
    }

    @Test
    void droppedConnectionIsRetriedForGetButNotForPost() throws Exception {
        ServerSocket hangUp = new ServerSocket(0);
        try {
            AtomicInteger accepted = new AtomicInteger();
            Thread acceptor = Thread.ofVirtual().start(() -> {
                while (!hangUp.isClosed()) {
                    try (Socket s = hangUp.accept()) {
                        accepted.incrementAndGet();
                        s.getInputStream().read(); // let the request start, then hang up without answering
                    } catch (IOException e) {
                        return;
                    }
                }
            });
            String url = "http://127.0.0.1:" + hangUp.getLocalPort();

            assertThatThrownBy(() -> transport(url, 2, Duration.ofSeconds(5), false).request("GET", "/api/x", null, null, null))
                    .isInstanceOf(HookbaseNetworkException.class);
            assertThat(sleeps).hasSize(2);

            sleeps.clear();
            assertThatThrownBy(() -> transport(url, 2, Duration.ofSeconds(5), false).request("POST", "/api/x", null, Map.of(), null))
                    .isInstanceOf(HookbaseNetworkException.class);
            assertThat(sleeps).isEmpty();
            hangUp.close();
            acceptor.join(2000);
        } finally {
            hangUp.close();
        }
    }

    @Test
    void interruptDuringBackoffStopsRetryingAndKeepsTheFlag() {
        server.otherwise(503, "{}");
        HttpTransport t = new HttpTransport("whr_secret", server.url(), Duration.ofSeconds(5), 3, HttpClient.newHttpClient(), false,
                d -> { throw new InterruptedException(); });
        try {
            assertThatThrownBy(() -> t.request("GET", "/api/x", null, null, null))
                    .isInstanceOf(HookbaseNetworkException.class)
                    .hasMessageContaining("Interrupted");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
        assertThat(server.requests()).hasSize(1);
    }

    // ---- response handling ----------------------------------------------------------------------

    @Test
    void nestedErrorObjectSuppliesMessageCodeAndDetails() {
        server.enqueue(500, "{\"error\":{\"message\":\"boom\",\"code\":\"internal\",\"details\":{\"traceId\":\"t1\"}}}");
        assertThatThrownBy(() -> transport(0).request("GET", "/api/x", null, null, null))
                .isInstanceOfSatisfying(HookbaseApiException.class, e -> {
                    assertThat(e.getMessage()).isEqualTo("boom");
                    assertThat(e.code()).isEqualTo("internal");
                    assertThat(e.details()).containsEntry("traceId", "t1");
                });
    }

    @Test
    void unprocessableEntityIsAValidationErrorWithFieldErrors() {
        server.enqueue(422, "{\"error\":\"Invalid input\",\"details\":{\"fieldErrors\":{\"url\":[\"Invalid url\"],\"name\":\"Required\"}}}");
        assertThatThrownBy(() -> transport(3).request("POST", "/api/x", null, Map.of(), null))
                .isInstanceOfSatisfying(ValidationException.class, e -> {
                    assertThat(e.status()).isEqualTo(422);
                    assertThat(e.fieldErrors()).containsEntry("url", List.of("Invalid url")).containsEntry("name", List.of("Required"));
                });
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void malformedJsonSuccessBodyIsAnInvalidResponseError() {
        server.enqueue(200, "{not json");
        assertThatThrownBy(() -> transport(3).request("GET", "/api/x", null, null, null))
                .isInstanceOfSatisfying(HookbaseApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("invalid_response");
                    assertThat(e.status()).isEqualTo(200);
                });
        // Not a retryable status, so it isn't resent.
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void debugLoggingNamesMethodAndUrlButNeverTheKeyOrBody() {
        Logger logger = Logger.getLogger("app.hookbase");
        List<String> lines = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                String msg = record.getMessage();
                Object[] params = record.getParameters();
                lines.add(params == null ? msg : java.text.MessageFormat.format(msg, params));
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        Level previous = logger.getLevel();
        logger.setLevel(Level.ALL);
        logger.addHandler(handler);
        try {
            server.enqueue(503, "{}").enqueue(200, "{}");
            transport(server.url(), 1, Duration.ofSeconds(5), true)
                    .request("PATCH", "/api/things/1", null, Map.of("signingSecret", "whsec_topsecret"), null);
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(previous);
        }
        assertThat(lines).anySatisfy(l -> assertThat(l).contains("PATCH").contains("/api/things/1"));
        assertThat(lines).anySatisfy(l -> assertThat(l).contains("retrying"));
        assertThat(String.join("\n", lines)).doesNotContain("whr_secret").doesNotContain("whsec_topsecret");
    }

    @Test
    void rejectsInvalidConstruction() {
        HttpClient c = HttpClient.newHttpClient();
        assertThatThrownBy(() -> new HttpTransport(" ", server.url(), Duration.ofSeconds(1), 0, c, false, d -> {}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HttpTransport("whr_x", server.url(), Duration.ofSeconds(1), -1, c, false, d -> {}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HttpTransport("whr_x", server.url(), Duration.ZERO, 0, c, false, d -> {}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
