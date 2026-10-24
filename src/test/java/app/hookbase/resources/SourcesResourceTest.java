package app.hookbase.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.hookbase.Page;
import app.hookbase.errors.NotFoundException;
import app.hookbase.models.BulkDeleteResult;
import app.hookbase.models.ImportResult;
import app.hookbase.models.PendingVerification;
import app.hookbase.models.Source;
import app.hookbase.models.SourceWithSecret;
import app.hookbase.params.CreateSourceParams;
import app.hookbase.params.ImportParams;
import app.hookbase.params.ListSourcesParams;
import app.hookbase.params.UpdateSourceParams;
import app.hookbase.testing.MockServer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SourcesResourceTest {

    static final String SOURCE = """
            {"id":"src_1","organizationId":"org_1","name":"GitHub","slug":"github","provider":"github",
             "description":null,"hasSigningSecret":true,"signingSecretLast4":"c123","verifiesWithoutSecret":false,
             "awaitingSigningSecret":false,"rejectInvalidSignatures":true,"rateLimitPerMinute":600,"isActive":true,
             "customDomainId":null,"ipFilterMode":"allowlist","ipAllowlist":["203.0.113.0/24"],"ipDenylist":null,
             "encryptFields":[],"maskFields":["$.card"],"dedupEnabled":true,"dedupStrategy":"provider_id",
             "dedupWindowHours":24,"dedupCustomHeader":null,"transientMode":false,"allowedMethods":["POST"],
             "eventCount":12,"routeCount":2,"ingestUrl":"https://api.hookbase.app/ingest/acme/github",
             "createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-02T00:00:00Z","someFutureField":1}""";

    private MockServer server;
    private SourcesResource sources;

    @BeforeEach
    void start() {
        server = new MockServer();
        sources = server.client().sources();
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    @SuppressWarnings("deprecation") // the filters are still sent, though the API ignores them
    void listSendsFiltersAndMapsNumberedPagination() {
        server.enqueue(200, "{\"sources\":[" + SOURCE + "," + SOURCE + "],\"pagination\":{\"total\":5,\"page\":1,\"pageSize\":2}}");
        Page<Source> page = sources.list(new ListSourcesParams().page(1).pageSize(2).search("git").provider("github").isActive(true));

        MockServer.Request req = server.last();
        assertThat(req.method()).isEqualTo("GET");
        assertThat(req.path()).isEqualTo("/api/sources");
        assertThat(req.query()).containsExactlyInAnyOrderEntriesOf(
                Map.of("page", "1", "pageSize", "2", "search", "git", "provider", "github", "isActive", "true"));
        assertThat(page.data()).hasSize(2);
        assertThat(page.total()).isEqualTo(5);
        assertThat(page.hasMore()).isTrue();
        assertThat(page.limit()).isEqualTo(2);
        assertThat(page.offset()).isZero();

        Source s = page.data().getFirst();
        assertThat(s.hasSigningSecret()).isTrue();
        assertThat(s.signingSecretLast4()).isEqualTo("c123");
        assertThat(s.rateLimitPerMinute()).isEqualTo(600);
        assertThat(s.ipAllowlist()).containsExactly("203.0.113.0/24");
        assertThat(s.ipDenylist()).isEmpty();
        assertThat(s.dedupWindowHours()).isEqualTo(24);
        assertThat(s.allowedMethods()).containsExactly("POST");
        assertThat(s.eventCount()).isEqualTo(12);
        assertThat(s.ingestUrl()).endsWith("/ingest/acme/github");
    }

    @Test
    void listWithoutParamsSendsNoQuery() {
        server.enqueue(200, "{\"sources\":[],\"pagination\":{\"total\":0,\"page\":1,\"pageSize\":20}}");
        Page<Source> page = sources.list();
        assertThat(server.last().query()).isEmpty();
        assertThat(page.hasMore()).isFalse();
        assertThat(page.limit()).isEqualTo(20);
    }

    @Test
    @SuppressWarnings("deprecation")
    void listAllWalksPagesKeepingFiltersAndPageSize() {
        server.enqueue(200, "{\"sources\":[{\"id\":\"src_1\"},{\"id\":\"src_2\"}],\"pagination\":{\"total\":3,\"page\":1,\"pageSize\":2}}");
        server.enqueue(200, "{\"sources\":[{\"id\":\"src_3\"}],\"pagination\":{\"total\":3,\"page\":2,\"pageSize\":2}}");
        List<String> ids = sources.listAll(new ListSourcesParams().pageSize(2).provider("stripe")).stream().map(Source::id).toList();

        assertThat(ids).containsExactly("src_1", "src_2", "src_3");
        assertThat(server.requests()).hasSize(2);
        assertThat(server.requests().get(0).query()).containsExactlyInAnyOrderEntriesOf(
                Map.of("page", "1", "pageSize", "2", "provider", "stripe"));
        assertThat(server.requests().get(1).query()).containsEntry("page", "2").containsEntry("pageSize", "2");
    }

    @Test
    void listAllStartsFromACallerSetPage() {
        server.enqueue(200, "{\"sources\":[{\"id\":\"src_9\"}],\"pagination\":{\"total\":9,\"page\":3,\"pageSize\":4}}");
        assertThat(sources.listAll(new ListSourcesParams().page(3).pageSize(4)).stream().toList()).hasSize(1);
        assertThat(server.last().query()).containsEntry("page", "3");
    }

    @Test
    void getUnwrapsSourceAndEncodesTheId() {
        server.enqueue(200, "{\"source\":" + SOURCE + "}");
        assertThat(sources.get("my source").slug()).isEqualTo("github");
        assertThat(server.last().method()).isEqualTo("GET");
        assertThat(server.last().path()).isEqualTo("/api/sources/my%20source");
    }

    @Test
    void createSendsExactBodyAndReturnsTheSecret() {
        server.enqueue(201, "{\"source\":{\"id\":\"src_new\",\"name\":\"New Source\",\"signingSecret\":\"whsec_abc123\"}}");
        SourceWithSecret created = sources.create(new CreateSourceParams().name("New Source").slug("new-source")
                .provider("github").rejectInvalidSignatures(true).allowedMethods(List.of("POST")));

        assertThat(created.signingSecret()).isEqualTo("whsec_abc123");
        assertThat(created.toSource().id()).isEqualTo("src_new");
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/sources");
        assertThat(server.last().body()).isEqualTo(MockServer.json(
                "{\"name\":\"New Source\",\"slug\":\"new-source\",\"provider\":\"github\",\"rejectInvalidSignatures\":true,\"allowedMethods\":[\"POST\"]}"));
    }

    @Test
    void updateAndDelete() {
        server.enqueue(200, "{\"success\":true}");
        sources.update("src_1", new UpdateSourceParams().name("Updated").isActive(false).description(null));
        assertThat(server.last().method()).isEqualTo("PATCH");
        assertThat(server.last().path()).isEqualTo("/api/sources/src_1");
        assertThat(server.last().rawBody()).isEqualTo("{\"name\":\"Updated\",\"isActive\":false,\"description\":null}");

        server.enqueue(204, null);
        sources.delete("src_1");
        assertThat(server.last().method()).isEqualTo("DELETE");
        assertThat(server.last().path()).isEqualTo("/api/sources/src_1");
        assertThat(server.last().rawBody()).isNull();
    }

    @Test
    void secretsAndVerifyTokens() {
        server.enqueue(200, "{\"signingSecret\":\"whsec_new123\"}");
        assertThat(sources.rotateSecret("src_1")).isEqualTo("whsec_new123");
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/sources/src_1/rotate-secret");

        server.enqueue(200, "{\"signingSecret\":\"whsec_abc123\"}");
        assertThat(sources.revealSecret("src_1")).isEqualTo("whsec_abc123");
        assertThat(server.last().method()).isEqualTo("GET");
        assertThat(server.last().path()).isEqualTo("/api/sources/src_1/reveal-secret");

        server.enqueue(200, "{\"verifyToken\":\"vt_1\"}");
        assertThat(sources.verifyToken("src_wa")).isEqualTo("vt_1");
        assertThat(server.last().method()).isEqualTo("GET");
        assertThat(server.last().path()).isEqualTo("/api/sources/src_wa/verify-token");

        server.enqueue(200, "{\"verifyToken\":\"vt_2\"}");
        assertThat(sources.rotateVerifyToken("src_wa")).isEqualTo("vt_2");
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/sources/src_wa/rotate-verify-token");
    }

    @Test
    void pendingVerification() {
        server.enqueue(200, "{\"provider\":\"notion\",\"token\":\"secret_abc\",\"receivedAt\":\"2026-10-11T12:00:00Z\",\"clientIp\":null}");
        PendingVerification pending = sources.getPendingVerification("notion");
        assertThat(pending).isEqualTo(new PendingVerification("notion", "secret_abc", "2026-10-11T12:00:00Z", null));
        assertThat(server.last().path()).isEqualTo("/api/sources/notion/pending-verification");

        server.enqueue(200, "{\"success\":true}");
        sources.adoptPendingVerification("notion");
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/sources/notion/pending-verification/adopt");

        server.enqueue(404, "{\"error\":\"No verification token is waiting\"}");
        assertThatThrownBy(() -> sources.getPendingVerification("notion")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void exportJoinsIdsAndOmitsThemWhenEmpty() {
        server.enqueue(200, "{\"version\":\"1.0\",\"sources\":[{\"slug\":\"github\"}]}");
        JsonNode exported = sources.export(List.of("src_1", "src_2"));
        assertThat(server.last().path()).isEqualTo("/api/sources/export");
        assertThat(server.last().query()).containsExactlyEntriesOf(Map.of("ids", "src_1,src_2"));
        assertThat(exported.get("sources").get(0).get("slug").asText()).isEqualTo("github");

        server.enqueue(200, "{\"sources\":[]}");
        sources.export();
        assertThat(server.last().query()).isEmpty();
        server.enqueue(200, "{\"sources\":[]}");
        sources.export(List.of());
        assertThat(server.last().query()).isEmpty();
    }

    @Test
    void importSendsItemsAndOptionsAndBindsTheApiShape() {
        server.enqueue(200, """
                {"success":true,"summary":{"total":2,"imported":1,"skipped":1,"overwritten":0,"failed":0},
                 "details":{"imported":["GitHub"],"skipped":["Stripe"],"overwritten":[],"failed":[]}}""");
        ImportResult result = sources.importSources(List.of(Map.of("name", "GitHub", "slug", "github")),
                new ImportParams().conflictStrategy("rename").validateOnly(false));
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/sources/import");
        assertThat(server.last().body()).isEqualTo(MockServer.json(
                "{\"sources\":[{\"name\":\"GitHub\",\"slug\":\"github\"}],\"conflictStrategy\":\"rename\",\"validateOnly\":false}"));
        assertThat(result.success()).isTrue();
        assertThat(result.summary().imported()).isEqualTo(1);
        assertThat(result.details().skipped()).containsExactly("Stripe");

        server.enqueue(200, """
                {"success":true,"validateOnly":true,"valid":false,"summary":{"total":1,"toCreate":0,"toOverwrite":0,"toSkip":0,"errors":1},
                 "validationResults":[{"index":0,"name":"X","slug":"x","status":"error","errors":["bad slug"],"warnings":[]}]}""");
        ImportResult validated = sources.importSources(MockServer.json("[{\"name\":\"X\"}]"));
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"sources\":[{\"name\":\"X\"}],\"conflictStrategy\":\"skip\"}"));
        assertThat(validated.valid()).isFalse();
        assertThat(validated.validateOnly()).isTrue();
        assertThat(validated.validationResults().getFirst().errors()).containsExactly("bad slug");
    }

    @Test
    void bulkDeleteSendsIdsInTheBody() {
        server.enqueue(200, "{\"success\":true,\"deleted\":3}");
        BulkDeleteResult result = sources.bulkDelete(List.of("src_1", "src_2", "src_3"));
        assertThat(result.deleted()).isEqualTo(3);
        assertThat(server.last().method()).isEqualTo("DELETE");
        assertThat(server.last().path()).isEqualTo("/api/sources/bulk");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"ids\":[\"src_1\",\"src_2\",\"src_3\"]}"));
    }
}
