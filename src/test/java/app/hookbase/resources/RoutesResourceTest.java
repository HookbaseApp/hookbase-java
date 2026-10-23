package app.hookbase.resources;

import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.Page;
import app.hookbase.models.CircuitResetResult;
import app.hookbase.models.CircuitStatusInfo;
import app.hookbase.models.Route;
import app.hookbase.params.CircuitBreakerConfigParams;
import app.hookbase.params.CreateRouteParams;
import app.hookbase.params.ImportParams;
import app.hookbase.params.ListRoutesParams;
import app.hookbase.params.UpdateRouteParams;
import app.hookbase.testing.MockServer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoutesResourceTest {

    static final String ROUTE = """
            {"id":"rt_1","organizationId":"org_1","name":"GitHub to Slack","sourceId":"src_1","destinationId":"dst_1",
             "filterId":null,"filterConditions":[{"field":"$.action","operator":"equals","value":"opened"}],
             "filterLogic":"and","transformId":null,"schemaId":null,"priority":0,"isActive":true,
             "circuitState":"closed","circuitOpenedAt":null,"circuitCooldownSeconds":60,"circuitFailureThreshold":5,
             "circuitProbeSuccessThreshold":2,"notifyOnFailure":true,"notifyOnSuccess":false,"notifyOnRecovery":true,
             "notifyEmails":"ops@example.com","failureThreshold":3,"failoverDestinationIds":["dst_2"],
             "failoverAfterAttempts":2,"expectedResponse":null,"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-02T00:00:00Z"}""";

    private MockServer server;
    private RoutesResource routes;

    @BeforeEach
    void start() {
        server = new MockServer();
        routes = server.client().routes();
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    @SuppressWarnings("deprecation") // the filters are still sent, though the API ignores them
    void listSendsFiltersAndBindsRoutes() {
        server.enqueue(200, "{\"routes\":[" + ROUTE + "],\"pagination\":{\"total\":1,\"page\":1,\"pageSize\":20}}");
        Page<Route> page = routes.list(new ListRoutesParams().sourceId("src_1").destinationId("dst_1").isActive(true));
        assertThat(server.last().method()).isEqualTo("GET");
        assertThat(server.last().path()).isEqualTo("/api/routes");
        assertThat(server.last().query()).containsExactlyInAnyOrderEntriesOf(
                Map.of("sourceId", "src_1", "destinationId", "dst_1", "isActive", "true"));
        Route r = page.data().getFirst();
        assertThat(r.filterConditions().get(0).get("operator").asText()).isEqualTo("equals");
        assertThat(r.failoverDestinationIds()).containsExactly("dst_2");
        assertThat(r.circuitCooldownSeconds()).isEqualTo(60);
        assertThat(r.notifyOnRecovery()).isTrue();
    }

    @Test
    void listAllWalksPages() {
        server.enqueue(200, "{\"routes\":[{\"id\":\"r1\"},{\"id\":\"r2\"}],\"pagination\":{\"total\":3,\"page\":1,\"pageSize\":2}}");
        server.enqueue(200, "{\"routes\":[{\"id\":\"r3\"}],\"pagination\":{\"total\":3,\"page\":2,\"pageSize\":2}}");
        assertThat(routes.listAll(new ListRoutesParams().pageSize(2)).stream().map(Route::id).toList()).containsExactly("r1", "r2", "r3");
        assertThat(server.requests().get(1).query()).containsEntry("page", "2").containsEntry("pageSize", "2");
    }

    @Test
    void getCreateUpdateDelete() {
        server.enqueue(200, "{\"route\":" + ROUTE + "}");
        assertThat(routes.get("rt_1").sourceId()).isEqualTo("src_1");
        assertThat(server.last().path()).isEqualTo("/api/routes/rt_1");

        server.enqueue(201, "{\"route\":{\"id\":\"rt_new\",\"name\":\"New Route\"}}");
        Route created = routes.create(new CreateRouteParams().name("New Route").sourceId("src_1").destinationId("dst_1")
                .filterConditions(List.of(Map.of("field", "$.a", "operator", "exists"))).filterLogic("and").priority(1)
                .failoverDestinationIds(List.of("dst_2")).failoverAfterAttempts(3));
        assertThat(created.name()).isEqualTo("New Route");
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/routes");
        assertThat(server.last().body()).isEqualTo(MockServer.json("""
                {"name":"New Route","sourceId":"src_1","destinationId":"dst_1",
                 "filterConditions":[{"field":"$.a","operator":"exists"}],"filterLogic":"and","priority":1,
                 "failoverDestinationIds":["dst_2"],"failoverAfterAttempts":3}"""));

        server.enqueue(200, "{\"success\":true}");
        routes.update("rt_1", new UpdateRouteParams().isActive(false).transformId(null).failureThreshold(null));
        assertThat(server.last().method()).isEqualTo("PATCH");
        assertThat(server.last().path()).isEqualTo("/api/routes/rt_1");
        assertThat(server.last().rawBody()).isEqualTo("{\"isActive\":false,\"transformId\":null,\"failureThreshold\":null}");

        server.enqueue(204, null);
        routes.delete("rt_1");
        assertThat(server.last().method()).isEqualTo("DELETE");
        assertThat(server.last().path()).isEqualTo("/api/routes/rt_1");
    }

    @Test
    void bulkDeleteAndBulkUpdate() {
        server.enqueue(200, "{\"success\":true,\"deleted\":2}");
        assertThat(routes.bulkDelete(List.of("rt_1", "rt_2")).deleted()).isEqualTo(2);
        assertThat(server.last().method()).isEqualTo("DELETE");
        assertThat(server.last().path()).isEqualTo("/api/routes/bulk");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"ids\":[\"rt_1\",\"rt_2\"]}"));

        server.enqueue(200, "{\"success\":true,\"updated\":2}");
        assertThat(routes.bulkUpdate(List.of("rt_1", "rt_2"), false).updated()).isEqualTo(2);
        assertThat(server.last().method()).isEqualTo("PATCH");
        assertThat(server.last().path()).isEqualTo("/api/routes/bulk");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"ids\":[\"rt_1\",\"rt_2\"],\"isActive\":false}"));
    }

    @Test
    void exportAndImport() {
        server.enqueue(200, "{\"routes\":[{\"name\":\"r\"}]}");
        routes.export(List.of("rt_1", "rt_2"));
        assertThat(server.last().path()).isEqualTo("/api/routes/export");
        assertThat(server.last().query()).containsExactlyEntriesOf(Map.of("ids", "rt_1,rt_2"));

        server.enqueue(200, "{\"success\":true,\"imported\":[\"r\"],\"skipped\":[],\"errors\":[{\"name\":\"q\",\"error\":\"Source not found: x\"}]}");
        var result = routes.importRoutes(List.of(Map.of("name", "r")), new ImportParams().validateOnly(true));
        assertThat(server.last().path()).isEqualTo("/api/routes/import");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"routes\":[{\"name\":\"r\"}],\"validateOnly\":true,\"conflictStrategy\":\"skip\"}"));
        assertThat(result.imported()).containsExactly("r");
        assertThat(result.errors().getFirst().error()).isEqualTo("Source not found: x");
    }

    @Test
    void circuitBreaker() {
        server.enqueue(200, """
                {"circuitState":"open","circuitOpenedAt":"2024-01-01T00:00:00Z","cooldownSeconds":60,"probeAttempts":0,
                 "probeSuccessThreshold":2,"failureThreshold":5,"consecutiveFailures":5,"timeUntilProbeSeconds":42}""");
        CircuitStatusInfo status = routes.getCircuitStatus("rt_1");
        assertThat(server.last().method()).isEqualTo("GET");
        assertThat(server.last().path()).isEqualTo("/api/routes/rt_1/circuit-status");
        assertThat(status.circuitState()).isEqualTo("open");
        assertThat(status.consecutiveFailures()).isEqualTo(5);
        assertThat(status.timeUntilProbeSeconds()).isEqualTo(42);

        server.enqueue(200, "{\"success\":true,\"circuitState\":\"closed\",\"previousState\":\"open\"}");
        CircuitResetResult reset = routes.resetCircuit("rt_1");
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/routes/rt_1/reset-circuit");
        assertThat(reset.circuitState()).isEqualTo("closed");
        assertThat(reset.previousState()).isEqualTo("open");

        server.enqueue(200, "{\"success\":true}");
        routes.updateCircuitConfig("rt_1", new CircuitBreakerConfigParams().circuitCooldownSeconds(120).circuitFailureThreshold(10));
        assertThat(server.last().method()).isEqualTo("PATCH");
        assertThat(server.last().path()).isEqualTo("/api/routes/rt_1/circuit-config");
        assertThat(server.last().rawBody()).isEqualTo("{\"circuitCooldownSeconds\":120,\"circuitFailureThreshold\":10}");
    }
}
