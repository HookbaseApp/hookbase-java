package app.hookbase.resources;

import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.Page;
import app.hookbase.models.Event;
import app.hookbase.models.EventDebugInfo;
import app.hookbase.models.EventDetail;
import app.hookbase.params.ExportEventsParams;
import app.hookbase.params.ListEventsParams;
import app.hookbase.testing.MockServer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventsResourceTest {

    static final String EVENT = """
            {"id":"evt_1","organizationId":"org_1","sourceId":"src_1","eventType":"push","payloadHash":"h",
             "signatureValid":true,"receivedAt":"2026-01-01T00:00:00Z","ipAddress":"1.2.3.4","sourceName":"GitHub",
             "sourceSlug":"github","status":"delivered","method":"POST",
             "deliveryStats":{"total":2,"delivered":2,"failed":0,"pending":0}}""";

    private MockServer server;
    private EventsResource events;

    @BeforeEach
    void start() {
        server = new MockServer();
        events = new EventsResource(DataAdminTestSupport.transport(server));
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void listSendsFiltersAndReadsKeysetCursor() {
        server.enqueue(200, "{\"events\":[" + EVENT + "],\"total\":null,\"limit\":10,\"hasMore\":true,\"nextCursor\":\"c2\"}");
        Page<Event> page = events.list(new ListEventsParams().limit(10).cursor("c1").sourceId("github").eventType("push")
                .search("x").fromDate("2026-01-01").toDate("2026-01-31").signatureValid(false).status("failed")
                .extra("offset", 20));

        MockServer.Request req = server.last();
        assertThat(req.method()).isEqualTo("GET");
        assertThat(req.path()).isEqualTo("/api/events");
        assertThat(req.query()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "limit", "10", "cursor", "c1", "sourceId", "github", "eventType", "push", "search", "x",
                "fromDate", "2026-01-01", "toDate", "2026-01-31", "signatureValid", "0", "status", "failed"));
        assertThat(page.hasMore()).isTrue();
        assertThat(page.cursor()).isEqualTo("c2");
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.limit()).isEqualTo(10);
        Event e = page.data().getFirst();
        assertThat(e.signatureValid()).isTrue();
        assertThat(e.sourceSlug()).isEqualTo("github");
        assertThat(e.deliveryStats().delivered()).isEqualTo(2);
    }

    @Test
    void listReportsTotalWhenIncluded() {
        server.enqueue(200, "{\"events\":[" + EVENT + "],\"total\":42,\"limit\":50,\"hasMore\":false,\"nextCursor\":null}");
        Page<Event> page = events.list(new ListEventsParams().includeTotal(true));
        assertThat(server.last().query()).containsEntry("includeTotal", "1");
        assertThat(page.total()).isEqualTo(42);
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void listAllFollowsCursorsWithDefaultLimit100() {
        server.enqueue(200, "{\"events\":[" + EVENT + "],\"hasMore\":true,\"nextCursor\":\"c2\"}");
        server.enqueue(200, "{\"events\":[" + EVENT + "],\"hasMore\":false,\"nextCursor\":null}");
        List<Event> all = events.listAll(new ListEventsParams().status("failed")).stream().toList();
        assertThat(all).hasSize(2);
        assertThat(server.requests().get(0).query()).containsEntry("limit", "100").containsEntry("status", "failed").doesNotContainKey("cursor");
        assertThat(server.requests().get(1).query()).containsEntry("cursor", "c2");
    }

    @Test
    void getFoldsTopLevelPayloadAndDeliveriesIntoTheEvent() {
        server.enqueue(200, """
                {"event":{"id":"evt_1","sourceId":"src_1","eventType":"push","headers":"{\\"x-github-event\\":\\"push\\"}",
                          "signatureValid":null,"status":"delivered"},
                 "payload":{"ref":"main"},"transient":false,
                 "deliveries":[{"id":"del_1","status":"delivered","attemptCount":1,"maxAttempts":5,"responseStatus":200,
                                "destinationName":"Slack","destinationUrl":"https://hooks.slack.com/x"}]}""");
        EventDetail detail = events.get("evt/1");
        assertThat(server.last().path()).isEqualTo("/api/events/evt%2F1");
        assertThat(detail.id()).isEqualTo("evt_1");
        assertThat(detail.payload().get("ref").asText()).isEqualTo("main");
        assertThat(detail.headers().get("x-github-event").asText()).isEqualTo("push");
        assertThat(detail.signatureValid()).isNull();
        assertThat(detail.isTransient()).isFalse();
        assertThat(detail.deliveries()).hasSize(1);
        assertThat(detail.deliveries().getFirst().responseStatus()).isEqualTo(200);
        assertThat(detail.deliveries().getFirst().destinationUrl()).isEqualTo("https://hooks.slack.com/x");
    }

    @Test
    void getWithoutDeliveriesGivesEmptyList() {
        server.enqueue(200, "{\"event\":{\"id\":\"evt_1\"},\"payload\":null,\"transient\":true}");
        EventDetail detail = events.get("evt_1");
        assertThat(detail.deliveries()).isEmpty();
        assertThat(detail.isTransient()).isTrue();
    }

    @Test
    void debugReadsNestedCurlCommand() {
        server.enqueue(200, """
                {"event":{"id":"evt_1","provider":"github","signatureValid":false,"signatureIssue":"unsigned"},
                 "request":{"headers":{"content-type":"application/json"},"payloadSize":12,"contentType":"application/json"},
                 "payload":{"a":1},"transient":false,"rawPayload":"{\\"a\\":1}",
                 "deliveries":[{"id":"del_1","clusterCount":3}],"timing":{"totalMs":5},"filterEvaluations":[],
                 "recentConfigChanges":{"sinceDays":7,"entries":[]},
                 "debug":{"curlCommand":"curl -X POST https://api.hookbase.app/ingest/acme/github"}}""");
        EventDebugInfo info = events.debug("evt_1");
        assertThat(server.last().path()).isEqualTo("/api/events/evt_1/debug");
        assertThat(info.curlCommand()).startsWith("curl -X POST");
        assertThat(info.event().signatureIssue()).isEqualTo("unsigned");
        assertThat(info.request().payloadSize()).isEqualTo(12L);
        assertThat(info.request().headers()).containsEntry("content-type", "application/json");
        assertThat(info.rawPayload()).isEqualTo("{\"a\":1}");
        assertThat(info.deliveries().get(0).get("clusterCount").asInt()).isEqualTo(3);
        assertThat(info.recentConfigChanges().get("sinceDays").asInt()).isEqualTo(7);
    }

    @Test
    void exportJsonAndCsv() {
        server.enqueue(200, "{\"events\":[{\"id\":\"evt_1\"}],\"exportedAt\":\"2026-01-01T00:00:00Z\",\"count\":1}");
        JsonNode json = events.export(new ExportEventsParams().sourceId("src_1").status("failed"));
        assertThat(server.last().path()).isEqualTo("/api/events/export");
        assertThat(server.last().query()).containsExactlyInAnyOrderEntriesOf(Map.of("sourceId", "src_1", "status", "failed"));
        assertThat(json.get("count").asInt()).isEqualTo(1);

        server.enqueue(200, "id,source\nevt_1,GitHub", Map.of("Content-Type", "text/csv"));
        String csv = events.exportCsv(new ExportEventsParams().eventType("push"));
        assertThat(server.last().query()).containsExactlyInAnyOrderEntriesOf(Map.of("eventType", "push", "format", "csv"));
        assertThat(csv).isEqualTo("id,source\nevt_1,GitHub");

        server.enqueue(200, "id\n", Map.of("Content-Type", "text/csv"));
        assertThat(events.export(new ExportEventsParams().format("csv")).asText()).isEqualTo("id\n");
    }
}
