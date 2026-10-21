package app.hookbase.resources;

import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.Hookbase;
import app.hookbase.Page;
import app.hookbase.models.Application;
import app.hookbase.params.CreateApplicationParams;
import app.hookbase.params.ListApplicationsParams;
import app.hookbase.params.UpdateApplicationParams;
import app.hookbase.testing.MockServer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationsResourceTest {

    private static final String APP = """
            {"id":"app_1","name":"Acme","organizationId":"org_1","externalId":"cust_1","metadata":{"tier":"gold"},
             "rateLimitPerSecond":null,"isDisabled":false,"totalEndpoints":2,"totalMessagesSent":10,"totalMessagesFailed":1,
             "createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-02T00:00:00Z","someFutureField":true}""";

    private MockServer server;
    private Hookbase client;

    @BeforeEach
    void start() {
        server = new MockServer();
        client = server.client();
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void listSendsCursorQueryAndDropsOffset() {
        server.enqueue(200, "{\"data\":[" + APP + "],\"pagination\":{\"hasMore\":true,\"nextCursor\":\"c2\"}}");
        Page<Application> page = client.applications().list(new ListApplicationsParams().limit(10).cursor("c1").search("ac").extra("offset", 20));

        MockServer.Request req = server.last();
        assertThat(req.method()).isEqualTo("GET");
        assertThat(req.path()).isEqualTo("/api/webhook-applications");
        assertThat(req.query()).containsExactlyInAnyOrderEntriesOf(Map.of("limit", "10", "cursor", "c1", "search", "ac"));
        assertThat(page.hasMore()).isTrue();
        assertThat(page.cursor()).isEqualTo("c2");
        Application app = page.data().getFirst();
        assertThat(app.externalId()).isEqualTo("cust_1");
        assertThat(app.metadata().get("tier").asText()).isEqualTo("gold");
        assertThat(app.rateLimitPerSecond()).isNull();
        assertThat(app.totalEndpoints()).isEqualTo(2);
    }

    @Test
    void listAllFollowsCursorsWithDefaultLimit100() {
        server.enqueue(200, "{\"data\":[" + APP + "],\"pagination\":{\"hasMore\":true,\"nextCursor\":\"c2\"}}");
        server.enqueue(200, "{\"data\":[" + APP + "],\"pagination\":{\"hasMore\":false,\"nextCursor\":null}}");
        List<Application> all = client.applications().listAll().stream().toList();
        assertThat(all).hasSize(2);
        assertThat(server.requests().get(0).query()).containsEntry("limit", "100").doesNotContainKey("cursor");
        assertThat(server.requests().get(1).query()).containsEntry("cursor", "c2");
    }

    @Test
    void getAndGetByUidEncodePath() {
        server.enqueue(200, "{\"data\":" + APP + "}").enqueue(200, "{\"data\":" + APP + "}");
        assertThat(client.applications().get("app_1").id()).isEqualTo("app_1");
        assertThat(server.last().path()).isEqualTo("/api/webhook-applications/app_1");
        client.applications().getByUid("cust/1");
        assertThat(server.last().path()).isEqualTo("/api/webhook-applications/by-external-id/cust%2F1");
    }

    @Test
    void createUpdateDeleteAndUpsertBodies() {
        server.enqueue(201, "{\"data\":" + APP + "}");
        client.applications().create(new CreateApplicationParams().name("Acme").externalId("cust_1").metadata(Map.of("a", 1)));
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"name\":\"Acme\",\"externalId\":\"cust_1\",\"metadata\":{\"a\":1}}"));

        server.enqueue(200, "{\"data\":" + APP + "}");
        client.applications().update("app_1", new UpdateApplicationParams().metadata(null));
        assertThat(server.last().method()).isEqualTo("PATCH");
        assertThat(server.last().rawBody()).isEqualTo("{\"metadata\":null}");

        server.enqueue(204, null);
        client.applications().delete("app_1");
        assertThat(server.last().method()).isEqualTo("DELETE");

        server.enqueue(200, "{\"data\":" + APP + ",\"created\":true}");
        client.applications().getOrCreate("cust_9", new CreateApplicationParams().name("Nine").externalId("ignored"));
        assertThat(server.last().method()).isEqualTo("PUT");
        assertThat(server.last().path()).isEqualTo("/api/webhook-applications/upsert");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"name\":\"Nine\",\"externalId\":\"cust_9\"}"));
    }
}
