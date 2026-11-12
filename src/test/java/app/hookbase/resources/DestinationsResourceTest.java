package app.hookbase.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.hookbase.Page;
import app.hookbase.errors.HookbaseException;
import app.hookbase.models.BridgeConfig;
import app.hookbase.models.Destination;
import app.hookbase.models.DestinationFieldMapping;
import app.hookbase.models.DestinationSecret;
import app.hookbase.models.DestinationTestResult;
import app.hookbase.models.DestinationThrottle;
import app.hookbase.models.ImportResult;
import app.hookbase.params.CreateDestinationParams;
import app.hookbase.params.ImportParams;
import app.hookbase.params.ListDestinationsParams;
import app.hookbase.params.RotateDestinationSecretParams;
import app.hookbase.params.UpdateDestinationParams;
import app.hookbase.testing.MockServer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DestinationsResourceTest {

    static final String DEST = """
            {"id":"dst_1","organizationId":"org_1","name":"My Endpoint","slug":"my-endpoint","type":"http",
             "url":"https://example.com","method":"POST","headers":{"X-Api-Key":"k"},"authType":"bearer",
             "authConfig":{"token":"***"},"timeoutMs":30000,"throttle":{"mode":"rate","rateLimit":10,"rateUnit":"second"},
             "isActive":true,"useStaticIp":false,"config":null,"fieldMapping":null,"batchSize":null,"batchWindowSeconds":null,
             "deliveryCount":7,"routeCount":1,"successCount":6,"failureCount":1,
             "createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-02T00:00:00Z"}""";

    private MockServer server;
    private DestinationsResource destinations;

    @BeforeEach
    void start() {
        server = new MockServer();
        destinations = server.client().destinations();
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    @SuppressWarnings("deprecation") // the filters are still sent, though the API ignores them
    void listSendsFiltersAndBindsDestinations() {
        server.enqueue(200, "{\"destinations\":[" + DEST + "],\"pagination\":{\"total\":1,\"page\":1,\"pageSize\":20}}");
        Page<Destination> page = destinations.list(new ListDestinationsParams().search("my").isActive(true));
        assertThat(server.last().method()).isEqualTo("GET");
        assertThat(server.last().path()).isEqualTo("/api/destinations");
        assertThat(server.last().query()).containsExactlyInAnyOrderEntriesOf(Map.of("search", "my", "isActive", "true"));
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.hasMore()).isFalse();
        Destination d = page.data().getFirst();
        assertThat(d.headers()).containsEntry("X-Api-Key", "k");
        assertThat(d.throttle()).isEqualTo(DestinationThrottle.rate(10, "second"));
        assertThat(d.timeoutMs()).isEqualTo(30000);
        assertThat(d.deliveryCount()).isEqualTo(7);
        assertThat(d.successCount()).isEqualTo(6L);
        assertThat(d.fieldMapping()).isNull();
    }

    @Test
    void listAllWalksPages() {
        server.enqueue(200, "{\"destinations\":[{\"id\":\"d1\"}],\"pagination\":{\"total\":2,\"page\":1,\"pageSize\":1}}");
        server.enqueue(200, "{\"destinations\":[{\"id\":\"d2\"}],\"pagination\":{\"total\":2,\"page\":2,\"pageSize\":1}}");
        assertThat(destinations.listAll(new ListDestinationsParams().pageSize(1)).stream().map(Destination::id).toList())
                .containsExactly("d1", "d2");
        assertThat(server.requests().get(0).query()).containsEntry("page", "1").containsEntry("pageSize", "1");
        assertThat(server.requests().get(1).query()).containsEntry("page", "2").containsEntry("pageSize", "1");
    }

    @Test
    void getUnwrapsDestinationAndBindsWarehouseFields() {
        server.enqueue(200, """
                {"destination":{"id":"dst_gcs","name":"GCS Dest","type":"gcs","url":"",
                 "config":{"bucket":"events","projectId":"proj-1","serviceAccountKey":"{}"},
                 "fieldMapping":[{"source":"$.payload","target":"data","type":"json"},
                                 {"source":"$.timestamp","target":"received_at","type":"timestamp","default":"now()"}]}}""");
        Destination d = destinations.get("dst_gcs");
        assertThat(server.last().path()).isEqualTo("/api/destinations/dst_gcs");
        assertThat(d.type()).isEqualTo("gcs");
        assertThat(d.config().get("bucket").asText()).isEqualTo("events");
        assertThat(d.fieldMapping()).hasSize(2);
        assertThat(d.fieldMapping().get(1).defaultValue()).isEqualTo("now()");
    }

    @Test
    void createSendsExactBodyWithAnExplicitSlug() {
        server.enqueue(201, "{\"destination\":" + DEST + "}");
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Api-Key", "k");
        Destination created = destinations.create(new CreateDestinationParams().name("My Endpoint").slug("custom")
                .url("https://example.com").method("POST").headers(headers).authType("bearer")
                .authConfig(Map.of("token", "t")).timeoutMs(5000).throttle(DestinationThrottle.rate(10, "second")));
        assertThat(created.name()).isEqualTo("My Endpoint");
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/destinations");
        assertThat(server.last().rawBody()).isEqualTo("{\"name\":\"My Endpoint\",\"slug\":\"custom\",\"url\":\"https://example.com\","
                + "\"method\":\"POST\",\"headers\":{\"X-Api-Key\":\"k\"},\"authType\":\"bearer\",\"authConfig\":{\"token\":\"t\"},"
                + "\"timeoutMs\":5000,\"throttle\":{\"mode\":\"rate\",\"rateLimit\":10,\"rateUnit\":\"second\"}}");
    }

    @Test
    void createDerivesTheSlugFromTheName() {
        server.enqueue(201, "{\"destination\":{\"id\":\"dst_new\",\"name\":\"New Dest\"}}");
        destinations.create(new CreateDestinationParams().name("New Dest").url("https://example.com/hook"));
        assertThat(server.last().body()).isEqualTo(MockServer.json(
                "{\"name\":\"New Dest\",\"url\":\"https://example.com/hook\",\"slug\":\"new-dest\"}"));
    }

    @Test
    void createWarehouseDestinationSendsConfigAndFieldMapping() {
        server.enqueue(201, """
                {"destination":{"id":"dst_s3","name":"S3 Warehouse","type":"s3",
                 "config":{"bucket":"my-bucket","region":"us-east-1"},
                 "fieldMapping":[{"source":"$.id","target":"event_id","type":"string"}],"batchSize":100}}""");
        Destination created = destinations.create(new CreateDestinationParams().name("S3 Warehouse").type("s3")
                .config(Map.of("bucket", "my-bucket"))
                .fieldMapping(List.of(new DestinationFieldMapping("$.id", "event_id", "string")))
                .batchSize(100).batchWindowSeconds(30).useStaticIp(false));
        assertThat(server.last().body()).isEqualTo(MockServer.json("""
                {"name":"S3 Warehouse","type":"s3","config":{"bucket":"my-bucket"},
                 "fieldMapping":[{"source":"$.id","target":"event_id","type":"string"}],
                 "batchSize":100,"batchWindowSeconds":30,"useStaticIp":false,"slug":"s3-warehouse"}"""));
        assertThat(created.type()).isEqualTo("s3");
        assertThat(created.fieldMapping().getFirst().target()).isEqualTo("event_id");
        assertThat(created.batchSize()).isEqualTo(100);
    }

    @Test
    void createWithNothingToDeriveFailsBeforeAnyRequest() {
        assertThatThrownBy(() -> destinations.create(new CreateDestinationParams().name("☃☃☃").url("https://example.com")))
                .isExactlyInstanceOf(HookbaseException.class)
                .hasMessageContaining("pass `slug` explicitly");
        assertThat(server.requests()).isEmpty();
    }

    @Test
    void updateSendsOnlyTheSetFieldsIncludingNulls() {
        server.enqueue(200, "{\"success\":true}");
        destinations.update("dst_s3", new UpdateDestinationParams().config(Map.of("bucket", "new-bucket"))
                .fieldMapping(List.of(new DestinationFieldMapping("$.body", "payload", "json"))).throttle(null).isActive(false));
        assertThat(server.last().method()).isEqualTo("PATCH");
        assertThat(server.last().path()).isEqualTo("/api/destinations/dst_s3");
        assertThat(server.last().rawBody()).isEqualTo("{\"config\":{\"bucket\":\"new-bucket\"},"
                + "\"fieldMapping\":[{\"source\":\"$.body\",\"target\":\"payload\",\"type\":\"json\"}],\"throttle\":null,\"isActive\":false}");
    }

    @Test
    void deleteExportImportAndBulkDelete() {
        server.enqueue(204, null);
        destinations.delete("dst_1");
        assertThat(server.last().method()).isEqualTo("DELETE");
        assertThat(server.last().path()).isEqualTo("/api/destinations/dst_1");

        server.enqueue(200, "{\"destinations\":[{\"slug\":\"a\"}]}");
        assertThat(destinations.export(List.of("dst_1")).get("destinations")).hasSize(1);
        assertThat(server.last().path()).isEqualTo("/api/destinations/export");
        assertThat(server.last().query()).containsExactlyEntriesOf(Map.of("ids", "dst_1"));

        server.enqueue(200, "{\"success\":true,\"summary\":{\"total\":1,\"imported\":1}}");
        ImportResult imported = destinations.importDestinations(List.of(Map.of("slug", "a")), new ImportParams().conflictStrategy("overwrite"));
        assertThat(server.last().path()).isEqualTo("/api/destinations/import");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"destinations\":[{\"slug\":\"a\"}],\"conflictStrategy\":\"overwrite\"}"));
        assertThat(imported.summary().total()).isEqualTo(1);

        server.enqueue(200, "{\"success\":true,\"deleted\":2}");
        assertThat(destinations.bulkDelete(List.of("d1", "d2")).deleted()).isEqualTo(2);
        assertThat(server.last().method()).isEqualTo("DELETE");
        assertThat(server.last().path()).isEqualTo("/api/destinations/bulk");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"ids\":[\"d1\",\"d2\"]}"));
    }

    @Test
    void testBindsTheApiNames() {
        server.enqueue(200, "{\"success\":true,\"status\":200,\"latencyMs\":150,\"responseBody\":\"OK\"}");
        DestinationTestResult ok = destinations.test("dst_1");
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/destinations/dst_1/test");
        assertThat(ok).isEqualTo(new DestinationTestResult(true, 200, 150, "OK", null));

        server.enqueue(200, "{\"success\":false,\"error\":\"Network error\"}");
        DestinationTestResult failed = destinations.test("dst_1");
        assertThat(failed.success()).isFalse();
        assertThat(failed.error()).isEqualTo("Network error");
        assertThat(failed.status()).isNull();
    }

    @Test
    void createReturnsTheSigningSecretOnceAndReadsCarryOnlyTheFlag() {
        server.enqueue(201, "{\"destination\":{\"id\":\"dst_9\",\"type\":\"http\",\"hasSigningSecret\":true,"
                + "\"signingSecret\":\"whsec_abc\"}}");
        Destination created = destinations.create(new CreateDestinationParams().name("Signed").url("https://example.com"));
        assertThat(created.hasSigningSecret()).isTrue();
        assertThat(created.signingSecret()).isEqualTo("whsec_abc");
        assertThat(created.toString()).doesNotContain("whsec_abc");

        server.enqueue(200, "{\"destination\":" + DEST.replace("\"isActive\":true", "\"isActive\":true,\"hasSigningSecret\":true") + "}");
        Destination read = destinations.get("dst_1");
        assertThat(read.hasSigningSecret()).isTrue();
        assertThat(read.signingSecret()).isNull();
    }

    @Test
    void revealSecretGetsTheSecret() {
        server.enqueue(200, "{\"signingSecret\":\"whsec_abc\",\"previousSecretExpiresAt\":null}");
        DestinationSecret secret = destinations.revealSecret("my dest");
        assertThat(server.last().method()).isEqualTo("GET");
        assertThat(server.last().path()).isEqualTo("/api/destinations/my%20dest/reveal-secret");
        assertThat(secret.signingSecret()).isEqualTo("whsec_abc");
        assertThat(secret.previousSecretExpiresAt()).isNull();
        assertThat(secret.toString()).doesNotContain("whsec_abc");
    }

    @Test
    void rotateSecretSendsTheGracePeriodOnlyWhenSet() {
        server.enqueue(200, "{\"signingSecret\":\"whsec_new\",\"previousSecretExpiresAt\":\"2026-11-11T01:00:00Z\"}");
        server.enqueue(200, "{\"signingSecret\":\"whsec_newer\",\"previousSecretExpiresAt\":null}");

        DestinationSecret rotated = destinations.rotateSecret("dst_1");
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/destinations/dst_1/rotate-secret");
        assertThat(server.last().rawBody()).isNullOrEmpty();
        assertThat(rotated.signingSecret()).isEqualTo("whsec_new");
        assertThat(rotated.previousSecretExpiresAt()).isEqualTo("2026-11-11T01:00:00Z");

        DestinationSecret now = destinations.rotateSecret("dst_1", new RotateDestinationSecretParams().gracePeriodSeconds(0));
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"gracePeriodSeconds\":0}"));
        assertThat(now.previousSecretExpiresAt()).isNull();
    }

    @Test
    void rotateSecretIsNotRetriedAfterA5xx() {
        server.enqueue(503, "{\"error\":\"unavailable\"}");
        server.enqueue(200, "{\"signingSecret\":\"whsec_new\",\"previousSecretExpiresAt\":null}");
        assertThatThrownBy(() -> destinations.rotateSecret("dst_1")).isInstanceOf(HookbaseException.class);
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void testWithAPayloadSendsItAndBindsABridgeDryRun() {
        server.enqueue(200, "{\"success\":true,\"dryRun\":true,\"eventType\":\"shop.order.created\","
                + "\"eventTypeExists\":true,\"matchingEndpoints\":3}");
        DestinationTestResult dry = destinations.test("dst_b", Map.of("type", "order.created"));
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/destinations/dst_b/test");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"payload\":{\"type\":\"order.created\"}}"));
        assertThat(dry).isEqualTo(new DestinationTestResult(true, null, null, null, null, true, "shop.order.created", true, 3));

        server.enqueue(200, "{\"success\":false,\"dryRun\":true,\"eventType\":null,\"eventTypeExists\":false,"
                + "\"matchingEndpoints\":0,\"error\":\"No event type at type\"}");
        DestinationTestResult missing = destinations.test("dst_b", Map.of());
        assertThat(missing.success()).isFalse();
        assertThat(missing.eventType()).isNull();
        assertThat(missing.error()).isEqualTo("No event type at type");

        // Without a payload no body is sent, and a plain test has no dry-run fields.
        server.enqueue(200, "{\"success\":true,\"status\":200,\"latencyMs\":5,\"responseBody\":\"OK\"}");
        DestinationTestResult plain = destinations.test("dst_1", (Object) null);
        assertThat(server.last().rawBody()).isNullOrEmpty();
        assertThat(plain.dryRun()).isFalse();
        assertThat(plain.eventTypeExists()).isNull();
        assertThat(plain.matchingEndpoints()).isNull();
    }

    @Test
    void createABridgeWithABridgeConfig() {
        server.enqueue(201, "{\"destination\":{\"id\":\"dst_b\",\"type\":\"bridge\",\"url\":\"\",\"hasSigningSecret\":false,"
                + "\"signingSecret\":null,\"config\":{\"eventTypeSource\":\"field\",\"eventTypeField\":\"type\","
                + "\"eventTypePrefix\":\"shop.\"}}}");
        Destination bridge = destinations.create(new CreateDestinationParams().name("Orders").type("bridge")
                .config(BridgeConfig.field("type").withPrefix("shop.").toMap()));
        assertThat(server.last().body()).isEqualTo(MockServer.json("""
                {"name":"Orders","type":"bridge","config":{"eventTypeSource":"field","eventTypeField":"type",
                 "eventTypePrefix":"shop."},"slug":"orders"}"""));
        assertThat(bridge.type()).isEqualTo("bridge");
        assertThat(bridge.hasSigningSecret()).isFalse();
        assertThat(bridge.signingSecret()).isNull();
        assertThat(bridge.config().get("eventTypePrefix").asText()).isEqualTo("shop.");
    }

    @Test
    void bridgeConfigLeavesUnsetFieldsOut() {
        assertThat(BridgeConfig.fixed("order.created").toMap()).containsExactly(
                Map.entry("eventTypeSource", "fixed"), Map.entry("eventType", "order.created"));
        assertThat(BridgeConfig.fixed("order.created").withApplicationId("app_1").withAllowUnverifiedSources(true).toMap())
                .containsExactly(Map.entry("eventTypeSource", "fixed"), Map.entry("eventType", "order.created"),
                        Map.entry("applicationId", "app_1"), Map.entry("allowUnverifiedSources", true));
    }
}
