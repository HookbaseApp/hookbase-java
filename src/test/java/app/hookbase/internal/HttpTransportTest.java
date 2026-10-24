package app.hookbase.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.hookbase.RequestOptions;
import app.hookbase.errors.AuthenticationException;
import app.hookbase.errors.ConflictException;
import app.hookbase.errors.ForbiddenException;
import app.hookbase.errors.HookbaseApiException;
import app.hookbase.errors.HookbaseNetworkException;
import app.hookbase.errors.HookbaseTimeoutException;
import app.hookbase.errors.NotFoundException;
import app.hookbase.errors.RateLimitException;
import app.hookbase.errors.ValidationException;
import app.hookbase.testing.MockServer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HttpTransportTest {

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

    private HttpTransport transport(String baseUrl, int retries, Duration timeout) {
        return new HttpTransport("whr_secret", baseUrl, timeout, retries, HttpClient.newHttpClient(), false, sleeps::add);
    }

    private HttpTransport transport() {
        return transport(server.url(), 3, Duration.ofSeconds(5));
    }

    @Test
    void sendsAuthUserAgentAndJsonBody() {
        server.enqueue(200, "{\"ok\":true}");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "x");
        body.put("cleared", null);
        JsonNode res = transport().request("POST", "/api/things", Map.of("a", "b c"), body, null);

        assertThat(res.get("ok").asBoolean()).isTrue();
        MockServer.Request req = server.last();
        assertThat(req.method()).isEqualTo("POST");
        assertThat(req.path()).isEqualTo("/api/things");
        assertThat(req.query()).containsEntry("a", "b c");
        assertThat(req.header("Authorization")).isEqualTo("Bearer whr_secret");
        assertThat(req.header("User-Agent")).startsWith("hookbase-java/" + SdkVersion.VERSION);
        assertThat(req.header("Content-Type")).isEqualTo("application/json");
        // An explicit null is sent, so an update can clear a field.
        assertThat(req.rawBody()).isEqualTo("{\"name\":\"x\",\"cleared\":null}");
    }

    @Test
    void stripsTrailingSlashFromBaseUrl() {
        transport(server.url() + "//", 0, Duration.ofSeconds(5)).request("GET", "/api/x", null, null, null);
        assertThat(server.last().path()).isEqualTo("/api/x");
    }

    @Test
    void omitsNullQueryValuesAndJoinsCollections() {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("skip", null);
        q.put("ids", List.of("a", "b"));
        q.put("limit", 10);
        transport().request("GET", "/api/x", q, null, null);
        assertThat(server.last().query()).containsExactly(Map.entry("ids", "a,b"), Map.entry("limit", "10"));
    }

    @Test
    void returns204AsNullAndCsvAsText() {
        server.enqueue(204, null);
        assertThat(transport().request("DELETE", "/api/x", null, null, null)).isNull();
        server.enqueue(200, "id,name\n1,a\n", Map.of("Content-Type", "text/csv"));
        JsonNode csv = transport().request("GET", "/api/x/export", null, null, null);
        assertThat(csv.isTextual()).isTrue();
        assertThat(csv.asText()).isEqualTo("id,name\n1,a\n");
    }

    @Test
    void sendsIdempotencyKeyHeader() {
        transport().request("POST", "/api/x", null, Map.of(), RequestOptions.builder().idempotencyKey("k1").build());
        assertThat(server.last().header("Idempotency-Key")).isEqualTo("k1");
    }

    @ParameterizedTest
    @CsvSource({
        "401, app.hookbase.errors.AuthenticationException",
        "403, app.hookbase.errors.ForbiddenException",
        "404, app.hookbase.errors.NotFoundException",
        "409, app.hookbase.errors.ConflictException",
        "400, app.hookbase.errors.ValidationException",
        "422, app.hookbase.errors.ValidationException",
        "418, app.hookbase.errors.HookbaseApiException",
    })
    void mapsStatusToExceptionWithoutRetrying(int status, String className) throws Exception {
        server.enqueue(status, "{\"error\":\"nope " + status + "\"}", Map.of("x-request-id", "req_1"));
        assertThatThrownBy(() -> transport().request("GET", "/api/x", null, null, null))
                .isExactlyInstanceOf(Class.forName(className))
                .hasMessage("nope " + status)
                .satisfies(e -> {
                    HookbaseApiException api = (HookbaseApiException) e;
                    assertThat(api.status()).isEqualTo(status);
                    assertThat(api.requestId()).isEqualTo("req_1");
                });
        assertThat(server.requests()).hasSize(1);
        assertThat(sleeps).isEmpty();
    }

    @Test
    void readsFieldErrorsAndCode() {
        server.enqueue(400, "{\"error\":\"Invalid\",\"code\":\"bad\",\"details\":{\"fieldErrors\":{\"url\":[\"Invalid url\"]}}}");
        assertThatThrownBy(() -> transport().request("POST", "/api/x", null, Map.of(), null))
                .isInstanceOfSatisfying(ValidationException.class, e -> {
                    assertThat(e.fieldErrors()).containsEntry("url", List.of("Invalid url"));
                    assertThat(e.details()).containsKey("fieldErrors");
                });
        server.enqueue(500, "{\"error\":{\"message\":\"nested\",\"code\":\"boom\"}}");
        assertThatThrownBy(() -> transport(server.url(), 0, Duration.ofSeconds(5)).request("GET", "/api/x", null, null, null))
                .isInstanceOfSatisfying(HookbaseApiException.class, e -> {
                    assertThat(e.getMessage()).isEqualTo("nested");
                    assertThat(e.code()).isEqualTo("boom");
                });
    }

    @Test
    void fallsBackToDefaultMessageForNonJsonError() {
        server.enqueue(502, "<html>bad gateway</html>", Map.of("Content-Type", "text/html"));
        assertThatThrownBy(() -> transport(server.url(), 0, Duration.ofSeconds(5)).request("GET", "/api/x", null, null, null))
                .isExactlyInstanceOf(HookbaseApiException.class)
                .hasMessage("API error: 502");
    }

    @Test
    void retriesGetOn503WithBackoff() {
        server.enqueue(503, "{}").enqueue(503, "{}").enqueue(200, "{\"ok\":1}");
        assertThat(transport().request("GET", "/api/x", null, null, null).get("ok").asInt()).isEqualTo(1);
        assertThat(server.requests()).hasSize(3);
        assertThat(sleeps).hasSize(2);
        assertThat(sleeps.get(0)).isBetween(Duration.ofMillis(1000), Duration.ofMillis(2000));
        assertThat(sleeps.get(1)).isBetween(Duration.ofMillis(2000), Duration.ofMillis(3000));
    }

    @Test
    void givesUpAfterMaxRetries() {
        server.otherwise(500, "{\"error\":\"down\"}");
        assertThatThrownBy(() -> transport().request("GET", "/api/x", null, null, null))
                .isInstanceOf(HookbaseApiException.class).hasMessage("down");
        assertThat(server.requests()).hasSize(4);
    }

    @Test
    void doesNotRetryOtherServerErrors() {
        server.enqueue(501, "{}");
        assertThatThrownBy(() -> transport().request("GET", "/api/x", null, null, null)).isInstanceOf(HookbaseApiException.class);
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void postWithoutIdempotencyKeyIsNotRetriedOn5xx() {
        server.enqueue(503, "{}");
        assertThatThrownBy(() -> transport().request("POST", "/api/x", null, Map.of(), null)).isInstanceOf(HookbaseApiException.class);
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void postWithIdempotencyKeyIsRetriedButExplicitRetriesAloneIsNot() {
        server.enqueue(503, "{}").enqueue(200, "{}");
        transport().request("POST", "/api/send-event", null, Map.of(), RequestOptions.builder().idempotencyKey("k").build());
        assertThat(server.requests()).hasSize(2);

        server.enqueue(503, "{}").enqueue(200, "{}");
        assertThatThrownBy(() -> transport().request("POST", "/api/x", null, Map.of(), RequestOptions.builder().retries(1).build()))
                .isInstanceOf(HookbaseApiException.class);
        assertThat(server.requests()).hasSize(3);
    }

    @Test
    void idempotencyKeyDoesNotMakeOtherPostsRetryable() {
        // Only send-event deduplicates on the key; a retried rotate-secret would rotate twice.
        server.enqueue(502, "{}");
        assertThatThrownBy(() -> transport().request("POST", "/api/sources/s1/rotate-secret", null, Map.of(),
                        RequestOptions.builder().idempotencyKey("k").build()))
                .isInstanceOf(HookbaseApiException.class);
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void rateLimitIsRetriedAfterRetryAfterEvenForPost() {
        server.enqueue(429, "{\"error\":\"slow down\"}", Map.of("Retry-After", "7")).enqueue(200, "{}");
        transport().request("POST", "/api/x", null, Map.of(), null);
        assertThat(server.requests()).hasSize(2);
        assertThat(sleeps).containsExactly(Duration.ofSeconds(7));
    }

    @Test
    void rateLimitExceptionCarriesRetryAfterWhenRetriesRunOut() {
        server.enqueue(429, "{\"error\":\"slow down\"}", Map.of("Retry-After", "5"));
        assertThatThrownBy(() -> transport(server.url(), 0, Duration.ofSeconds(5)).request("GET", "/api/x", null, null, null))
                .isInstanceOfSatisfying(RateLimitException.class, e -> {
                    assertThat(e.getMessage()).isEqualTo("slow down");
                    assertThat(e.retryAfter()).isEqualTo(Duration.ofSeconds(5));
                });
    }

    @Test
    void quotaRateLimitWithoutRetryAfterIsNotRetried() {
        server.enqueue(429, "{\"error\":\"Monthly limit exceeded\"}");
        assertThatThrownBy(() -> transport().request("GET", "/api/x", null, null, null))
                .isInstanceOfSatisfying(RateLimitException.class, e -> assertThat(e.retryAfter()).isNull());
        assertThat(server.requests()).hasSize(1);
        assertThat(sleeps).isEmpty();
    }

    @Test
    void retryAfterLongerThanTheCapIsNotWaitedFor() {
        server.enqueue(429, "{}", Map.of("Retry-After", "9223372036854775807"));
        assertThatThrownBy(() -> transport().request("GET", "/api/x", null, null, null)).isInstanceOf(RateLimitException.class);
        assertThat(server.requests()).hasSize(1);
        assertThat(sleeps).isEmpty();
    }

    @Test
    void connectFailureIsRetriedEvenForPost() throws IOException {
        int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        HttpTransport t = transport("http://127.0.0.1:" + port, 2, Duration.ofSeconds(2));
        assertThatThrownBy(() -> t.request("POST", "/api/x", null, Map.of(), null)).isInstanceOf(HookbaseNetworkException.class);
        assertThat(sleeps).hasSize(2);
    }

    @Test
    void timeoutIsReportedAndNotRetriedForPost() throws IOException {
        try (ServerSocket silent = new ServerSocket(0)) {
            // Accepts the connection (backlog) but never answers.
            HttpTransport t = transport("http://127.0.0.1:" + silent.getLocalPort(), 3, Duration.ofMillis(300));
            assertThatThrownBy(() -> t.request("POST", "/api/x", null, Map.of(), null))
                    .isInstanceOf(HookbaseTimeoutException.class)
                    .hasMessageContaining("timed out after 300ms");
            assertThat(sleeps).isEmpty();
        }
    }

    @Test
    void parsesRetryAfterForms() {
        assertThat(HttpTransport.parseRetryAfter(null)).isNull();
        assertThat(HttpTransport.parseRetryAfter("3")).isEqualTo(Duration.ofSeconds(3));
        assertThat(HttpTransport.parseRetryAfter("garbage")).isNull();
        assertThat(HttpTransport.parseRetryAfter("-5")).isNull();
        assertThat(HttpTransport.parseRetryAfter("99999999999999999999")).isNull();
        assertThat(HttpTransport.parseRetryAfter("999999999")).isEqualTo(Duration.ofDays(1));
        assertThat(HttpTransport.parseRetryAfter("Wed, 21 Oct 2015 07:28:00 GMT")).isEqualTo(Duration.ZERO);
    }

    @Test
    void encodesPathSegments() {
        assertThat(HttpTransport.segment("a/b c")).isEqualTo("a%2Fb%20c");
        assertThatThrownBy(() -> HttpTransport.segment("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HttpTransport.segment("..")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HttpTransport.segment(".")).isInstanceOf(IllegalArgumentException.class);
        assertThat(HttpTransport.segment("...")).isEqualTo("...");
    }

    @Test
    void rejectsMissingApiKey() {
        assertThatThrownBy(() -> new HttpTransport(" ", "http://x", Duration.ofSeconds(1), 0, HttpClient.newHttpClient(), false, d -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("API key is required");
    }

    @SuppressWarnings("unused")
    private static final Class<?>[] REFERENCED = {AuthenticationException.class, ForbiddenException.class, NotFoundException.class, ConflictException.class};
}
