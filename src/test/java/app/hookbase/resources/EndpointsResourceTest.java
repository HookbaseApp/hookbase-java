package app.hookbase.resources;

import static app.hookbase.resources.OutboundTestSupport.cursorPage;
import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.Page;
import app.hookbase.models.Endpoint;
import app.hookbase.models.EndpointHeader;
import app.hookbase.models.EndpointRotateSecretResult;
import app.hookbase.models.EndpointStats;
import app.hookbase.params.CreateEndpointParams;
import app.hookbase.params.ListEndpointsParams;
import app.hookbase.params.RotateEndpointSecretParams;
import app.hookbase.params.TestEndpointParams;
import app.hookbase.params.UpdateEndpointParams;
import app.hookbase.testing.MockServer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EndpointsResourceTest {

    /** Every key the API's formatEndpoint returns, populated with non-default values. */
    static final String ENDPOINT = """
            {"id":"ep_1","applicationId":"app_1","url":"https://customer.example.com/hooks","description":"Primary",
             "secretPrefix":"whsec_abcdef...","hasSecret":true,"secretVersion":2,
             "headers":[{"name":"X-Tenant","value":"acme"}],"timeoutSeconds":45,"isDisabled":false,
             "disabledAt":null,"disabledReason":null,"rateLimitPerSecond":25,"successStatusCodes":[200,201,"2xx"],
             "backoffType":"linear","retryDelays":[5,30,300],"ipAllowlistNotes":"egress from 203.0.113.0/24",
             "useStaticIp":true,"circuitState":"closed","circuitOpenedAt":null,"circuitFailureCount":1,
             "circuitFailureThreshold":7,"circuitSuccessThreshold":3,"circuitCooldownSeconds":120,
             "totalMessages":900,"totalSuccesses":880,"totalFailures":20,"avgResponseTimeMs":143,
             "lastSuccessAt":"2026-01-05T00:00:00Z","lastFailureAt":"2026-01-04T00:00:00Z","lastResponseStatus":200,
             "isVerified":true,"verifiedAt":"2026-01-01T00:00:00Z","createdAt":"2026-01-01T00:00:00Z",
             "updatedAt":"2026-01-06T00:00:00Z","createdBy":"u1","apiKeyId":null,"someFutureField":1}""";

    private MockServer server;
    private EndpointsResource endpoints;

    @BeforeEach
    void start() {
        server = new MockServer();
        endpoints = new EndpointsResource(OutboundTestSupport.transport(server));
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void listSendsApplicationIdAndCursorAndDropsOffset() {
        server.enqueue(200, cursorPage(ENDPOINT.replace("\"apiKeyId\":null", "\"apiKeyId\":null,\"subscriptionCount\":3"), "c2"));
        Page<Endpoint> page = endpoints.list("app_1", new ListEndpointsParams().limit(10).cursor("c1").isDisabled(false).extra("offset", 50));

        MockServer.Request req = server.last();
        assertThat(req.method()).isEqualTo("GET");
        assertThat(req.path()).isEqualTo("/api/webhook-endpoints");
        assertThat(req.query()).containsExactlyInAnyOrderEntriesOf(
                Map.of("limit", "10", "cursor", "c1", "isDisabled", "false", "applicationId", "app_1"));
        assertThat(page.hasMore()).isTrue();
        assertThat(page.cursor()).isEqualTo("c2");
        Endpoint ep = page.data().getFirst();
        assertThat(ep.subscriptionCount()).isEqualTo(3);
        assertThat(ep.headers()).containsExactly(new EndpointHeader("X-Tenant", "acme"));
    }

    @Test
    void listAllFollowsCursorsWithDefaultLimit100() {
        server.enqueue(200, cursorPage(ENDPOINT, "c/1+="));
        server.enqueue(200, cursorPage(ENDPOINT.replace("ep_1", "ep_2"), null));
        List<Endpoint> all = endpoints.listAll("app_1").stream().toList();
        assertThat(all).extracting(Endpoint::id).containsExactly("ep_1", "ep_2");
        assertThat(server.requests().get(0).query()).containsEntry("limit", "100").containsEntry("applicationId", "app_1").doesNotContainKey("cursor");
        assertThat(server.requests().get(1).query()).containsEntry("cursor", "c/1+=");
    }

    @Test
    void getBindsEveryField() {
        server.enqueue(200, "{\"data\":" + ENDPOINT + "}");
        Endpoint ep = endpoints.get("app_1", "ep/1");

        assertThat(server.last().path()).isEqualTo("/api/webhook-endpoints/ep%2F1");
        assertThat(ep.secretPrefix()).isEqualTo("whsec_abcdef...");
        assertThat(ep.secretVersion()).isEqualTo(2);
        assertThat(ep.rateLimitPerSecond()).isEqualTo(25);
        assertThat(ep.successStatusCodes()).containsExactly("200", "201", "2xx");
        assertThat(ep.backoffType()).isEqualTo("linear");
        assertThat(ep.retryDelays()).containsExactly(5, 30, 300);
        assertThat(ep.ipAllowlistNotes()).isEqualTo("egress from 203.0.113.0/24");
        assertThat(ep.useStaticIp()).isTrue();
        assertThat(ep.circuitFailureThreshold()).isEqualTo(7);
        assertThat(ep.circuitCooldownSeconds()).isEqualTo(120);
        assertThat(ep.avgResponseTimeMs()).isEqualTo(143.0);
        assertThat(ep.lastResponseStatus()).isEqualTo(200);
        assertThat(ep.isVerified()).isTrue();
        assertThat(ep.apiKeyId()).isNull();
        assertThat(ep.secret()).isNull();
        assertThat(ep.subscriptionCount()).isNull();
    }

    @Test
    void headerMapKeepsArrivalOrderAndEmptyIsEmpty() {
        server.enqueue(200, "{\"data\":" + ENDPOINT.replace("[{\"name\":\"X-Tenant\",\"value\":\"acme\"}]",
                "[{\"name\":\"X-Tenant\",\"value\":\"acme\"},{\"name\":\"X-Env\",\"value\":\"prod\"}]") + "}");
        Endpoint ep = endpoints.get("app_1", "ep_1");
        assertThat(ep.headerMap()).containsExactly(Map.entry("X-Tenant", "acme"), Map.entry("X-Env", "prod"));

        server.enqueue(200, "{\"data\":{\"id\":\"ep_1\",\"headers\":[]}}");
        assertThat(endpoints.get("app_1", "ep_1").headers()).isEmpty();
        server.enqueue(200, "{\"data\":{\"id\":\"ep_1\"}}");
        assertThat(endpoints.get("app_1", "ep_1").headerMap()).isEmpty();
    }

    @Test
    void createSendsApplicationIdAndReturnsTheSecret() {
        server.enqueue(201, "{\"data\":" + ENDPOINT.replace("\"apiKeyId\":null", "\"apiKeyId\":null,\"secret\":\"whsec_plaintext\"") + "}");
        Endpoint created = endpoints.create("app_1", new CreateEndpointParams().url("https://example.com/hook").timeoutSeconds(10));

        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/webhook-endpoints");
        assertThat(server.last().body()).isEqualTo(MockServer.json(
                "{\"url\":\"https://example.com/hook\",\"timeoutSeconds\":10,\"applicationId\":\"app_1\"}"));
        assertThat(created.secret()).isEqualTo("whsec_plaintext");
        assertThat(created.headerMap()).containsEntry("X-Tenant", "acme");
    }

    @Test
    void updateDeleteEnableDisable() {
        server.enqueue(200, "{\"data\":" + ENDPOINT + "}");
        Endpoint updated = endpoints.update("app_1", "ep_1", new UpdateEndpointParams().retryDelays(List.of(10)).description(null));
        assertThat(server.last().method()).isEqualTo("PATCH");
        assertThat(server.last().path()).isEqualTo("/api/webhook-endpoints/ep_1");
        assertThat(server.last().rawBody()).isEqualTo("{\"retryDelays\":[10],\"description\":null}");
        assertThat(updated.retryDelays()).containsExactly(5, 30, 300);

        server.enqueue(204, null);
        endpoints.delete("app_1", "ep_1");
        assertThat(server.last().method()).isEqualTo("DELETE");
        assertThat(server.last().path()).isEqualTo("/api/webhook-endpoints/ep_1");
        assertThat(server.last().rawBody()).isNull();

        server.enqueue(200, "{\"data\":" + ENDPOINT + "}");
        endpoints.disable("app_1", "ep_1");
        assertThat(server.last().rawBody()).isEqualTo("{\"isDisabled\":true}");
        server.enqueue(200, "{\"data\":" + ENDPOINT + "}");
        endpoints.enable("app_1", "ep_1");
        assertThat(server.last().rawBody()).isEqualTo("{\"isDisabled\":false}");
    }

    @Test
    void rotateSecretWithAndWithoutGracePeriod() {
        String rotated = "{\"secret\":\"whsec_new\",\"previousSecretExpiresAt\":\"2026-10-17T13:00:00Z\",\"secretVersion\":3}";
        server.enqueue(200, rotated).enqueue(200, rotated);

        EndpointRotateSecretResult res = endpoints.rotateSecret("app_1", "ep_1", new RotateEndpointSecretParams().gracePeriodSeconds(0));
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/webhook-endpoints/ep_1/rotate-secret");
        assertThat(server.last().rawBody()).isEqualTo("{\"gracePeriodSeconds\":0}");
        assertThat(res).isEqualTo(new EndpointRotateSecretResult("whsec_new", "2026-10-17T13:00:00Z", 3));

        endpoints.rotateSecret("app_1", "ep_1");
        assertThat(server.last().rawBody()).isNull();
    }

    @Test
    void getStatsComputesFromTheCounters() {
        server.enqueue(200, "{\"data\":" + ENDPOINT + "}");
        EndpointStats stats = endpoints.getStats("app_1", "ep_1");
        assertThat(stats.averageLatency()).isEqualTo(143.0);
        assertThat(stats.totalMessages()).isEqualTo(900);
        assertThat(stats.successRate()).isCloseTo(880.0 / 900 * 100, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(stats.recentFailures()).isZero();

        server.enqueue(200, "{\"data\":{\"id\":\"ep_1\",\"totalMessages\":0,\"avgResponseTimeMs\":null}}");
        EndpointStats empty = endpoints.getStats("app_1", "ep_1");
        assertThat(empty.successRate()).isZero();
        assertThat(empty.averageLatency()).isZero();
    }

    @Test
    void recoverCircuitResetsThenGets() {
        server.enqueue(200, "{\"success\":true}").enqueue(200, "{\"data\":" + ENDPOINT + "}");
        Endpoint ep = endpoints.recoverCircuit("app_1", "ep_1");
        assertThat(server.requests()).extracting(r -> r.method() + " " + r.path()).containsExactly(
                "POST /api/webhook-endpoints/ep_1/reset-circuit", "GET /api/webhook-endpoints/ep_1");
        assertThat(ep.circuitState()).isEqualTo("closed");
    }

    @Test
    void testSendsEventTypeAndPayloadOrNoBody() {
        server.enqueue(200, "{\"success\":true,\"statusCode\":200}").enqueue(200, "{\"success\":true}");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("x", 1);
        JsonNode result = endpoints.test("app_1", "ep_1", new TestEndpointParams().eventType("order.created").payload(payload));
        assertThat(server.last().path()).isEqualTo("/api/webhook-endpoints/ep_1/test");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"eventType\":\"order.created\",\"payload\":{\"x\":1}}"));
        assertThat(result.get("statusCode").asInt()).isEqualTo(200);

        endpoints.test("app_1", "ep_1");
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().rawBody()).isNull();
    }
}
