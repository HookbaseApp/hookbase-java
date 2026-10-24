package app.hookbase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.hookbase.testing.MockServer;
import app.hookbase.testing.TestClients;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** The client itself: construction, each resource accessor's wiring, and raw requests. */
class HookbaseClientTest {

    private MockServer server;

    @BeforeEach
    void start() {
        server = new MockServer();
    }

    @AfterEach
    void stop() {
        server.close();
    }

    /** One cheap call per accessor, so a resource wired to the wrong transport or path shows up. */
    static Stream<Arguments> accessors() {
        return Stream.of(
                call("applications", h -> h.applications().list(), "/api/webhook-applications"),
                call("endpoints", h -> h.endpoints().list("app_1"), "/api/webhook-endpoints"),
                call("eventTypes", h -> h.eventTypes().list(), "/api/event-types"),
                call("subscriptions", h -> h.subscriptions().list("app_1"), "/api/webhook-subscriptions"),
                call("messages", h -> h.messages().list("app_1"), "/api/outbound-messages"),
                call("portalTokens", h -> h.portalTokens().list("app_1"), "/api/portal/webhook-applications/app_1/tokens"),
                call("dlq", h -> h.dlq().list(), "/api/outbound-messages/dlq/messages"),
                call("sources", h -> h.sources().list(), "/api/sources"),
                call("destinations", h -> h.destinations().list(), "/api/destinations"),
                call("routes", h -> h.routes().list(), "/api/routes"),
                call("events", h -> h.events().list(), "/api/events"),
                call("deliveries", h -> h.deliveries().list(), "/api/deliveries"),
                call("transforms", h -> h.transforms().list(), "/api/transforms"),
                call("filters", h -> h.filters().list(), "/api/filters"),
                call("schemas", h -> h.schemas().list(), "/api/schemas"),
                call("providers", h -> h.providers().list(), "/api/catalog/providers"),
                call("analytics", h -> h.analytics().overview(), "/api/analytics/overview"),
                call("apiKeys", h -> h.apiKeys().list(), "/api/api-keys"),
                call("tunnels", h -> h.tunnels().list(), "/api/tunnels"),
                call("cronJobs", h -> h.cronJobs().list(), "/api/cron"));
    }

    private static Arguments call(String name, Consumer<Hookbase> call, String path) {
        return Arguments.of(name, call, path);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("accessors")
    void everyAccessorIsWiredToTheClient(String name, Consumer<Hookbase> call, String path) {
        Hookbase hookbase = server.client();
        call.accept(hookbase);
        MockServer.Request req = server.last();
        assertThat(req.method()).isEqualTo("GET");
        assertThat(req.path()).isEqualTo(path);
        assertThat(req.header("Authorization")).isEqualTo("Bearer whr_test");
    }

    @Test
    void accessorsReturnTheSameInstanceEachTime() {
        Hookbase hookbase = server.client();
        assertThat(hookbase.sources()).isSameAs(hookbase.sources());
        assertThat(hookbase.messages()).isSameAs(hookbase.messages());
    }

    @Test
    void rawRequestAddsALeadingSlashAndUsesTheSamePipeline() {
        server.enqueue(200, "{\"ok\":true}");
        JsonNode res = server.client().request("GET", "api/features", Map.of("a", "1"), null, null);
        assertThat(res.get("ok").asBoolean()).isTrue();
        assertThat(server.last().path()).isEqualTo("/api/features");
        assertThat(server.last().query()).containsEntry("a", "1");

        server.enqueue(404, "{\"error\":\"nope\"}");
        assertThatThrownBy(() -> server.client().request("DELETE", "/api/x", null, null, null))
                .isInstanceOf(app.hookbase.errors.NotFoundException.class)
                .hasMessage("nope");
        assertThatThrownBy(() -> server.client().request(null, "/x", null, null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> server.client().request("GET", null, null, null, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void builderAcceptsACustomHttpClientThatDoesNotFollowRedirects() {
        HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        Hookbase hookbase = Hookbase.builder().apiKey("whr_x").baseUrl(server.url()).httpClient(http).debug(true)
                .timeout(Duration.ofSeconds(2)).maxRetries(0).build();
        hookbase.sources().list();
        assertThat(server.last().header("Authorization")).isEqualTo("Bearer whr_x");
    }

    @Test
    void redirectsAreNotFollowedSoTheKeyNeverLeavesTheHost() {
        server.enqueue(302, "", Map.of("Location", "https://elsewhere.example/steal"));
        assertThatThrownBy(() -> TestClients.noSleep(server.url(), 0).sources().list())
                .isInstanceOf(app.hookbase.errors.HookbaseApiException.class)
                .satisfies(e -> assertThat(((app.hookbase.errors.HookbaseApiException) e).status()).isEqualTo(302));
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void constructorTakesJustAKeyAndRejectsAMissingOne() {
        assertThat(new Hookbase("whr_x")).isNotNull();
        assertThatThrownBy(() -> new Hookbase(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Hookbase("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Hookbase.builder().apiKey("whr_x").baseUrl("http://[bad").build())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Hookbase.builder().apiKey("whr_x").maxRetries(-1).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void versionIsTheSdkVersion() {
        assertThat(Hookbase.VERSION).isEqualTo(app.hookbase.internal.SdkVersion.VERSION).isNotBlank();
    }
}
