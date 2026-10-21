package app.hookbase.resources;

import static app.hookbase.resources.OutboundTestSupport.cursorPage;
import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.Page;
import app.hookbase.models.DlqBulkDeleteResult;
import app.hookbase.models.DlqBulkRetryResult;
import app.hookbase.models.DlqMessage;
import app.hookbase.models.DlqStats;
import app.hookbase.models.MessageReplayResult;
import app.hookbase.params.ListDlqParams;
import app.hookbase.testing.MockServer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DlqResourceTest {

    static final String ITEM = """
            {"id":"m1","eventId":"evt_1","eventType":"order.created","applicationId":"app_1","endpointId":"ep_1",
             "status":"dlq","attempts":5,"maxAttempts":5,"lastResponseStatus":500,"lastErrorType":"http_error",
             "lastErrorMessage":"HTTP 500","dlqReason":"max_retries_exceeded","dlqMovedAt":"2026-10-01T00:00:00Z",
             "dlqRetryCount":1,"dlqLastRetryAt":"2026-10-02T00:00:00Z","createdAt":"2026-09-30T00:00:00Z",
             "completedAt":"2026-10-01T00:00:00Z","endpointUrl":"https://a.example","applicationName":"Acme"}""";

    private MockServer server;
    private DlqResource dlq;

    @BeforeEach
    void start() {
        server = new MockServer();
        dlq = new DlqResource(OutboundTestSupport.transport(server));
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void listSendsEveryFilterAndBindsFields() {
        server.enqueue(200, cursorPage(ITEM, "2026-10-01T00:00:00Z"));
        Page<DlqMessage> page = dlq.list(new ListDlqParams().limit(2).cursor("c1").endpointId("ep_1").applicationId("app_1")
                .reason("max_retries_exceeded").eventType("order.created").startDate("2026-09-01").endDate("2026-10-31"));
        assertThat(server.last().method()).isEqualTo("GET");
        assertThat(server.last().path()).isEqualTo("/api/outbound-messages/dlq/messages");
        assertThat(server.last().query()).containsExactlyInAnyOrderEntriesOf(Map.of("limit", "2", "cursor", "c1",
                "endpointId", "ep_1", "applicationId", "app_1", "reason", "max_retries_exceeded",
                "eventType", "order.created", "startDate", "2026-09-01", "endDate", "2026-10-31"));
        assertThat(page.hasMore()).isTrue();
        assertThat(page.cursor()).isEqualTo("2026-10-01T00:00:00Z");
        DlqMessage m = page.data().getFirst();
        assertThat(m.eventId()).isEqualTo("evt_1");
        assertThat(m.lastErrorType()).isEqualTo("http_error");
        assertThat(m.dlqRetryCount()).isEqualTo(1);
        assertThat(m.dlqLastRetryAt()).isEqualTo("2026-10-02T00:00:00Z");
        assertThat(m.endpointUrl()).isEqualTo("https://a.example");
    }

    @Test
    void lastPageHasANullCursor() {
        server.enqueue(200, cursorPage(ITEM, null));
        Page<DlqMessage> page = dlq.list();
        assertThat(page.hasMore()).isFalse();
        assertThat(page.cursor()).isNull();
    }

    @Test
    void listAllFollowsCursorsAndKeepsTheCallersLimit() {
        server.enqueue(200, cursorPage("{\"id\":\"msg_1\"},{\"id\":\"msg_2\"}", "cursor_1"));
        server.enqueue(200, cursorPage("{\"id\":\"msg_3\"}", null));
        assertThat(dlq.listAll(new ListDlqParams().limit(2)).stream().map(DlqMessage::id).toList())
                .containsExactly("msg_1", "msg_2", "msg_3");
        assertThat(server.requests().get(0).query()).containsEntry("limit", "2").doesNotContainKey("cursor");
        assertThat(server.requests().get(1).query()).containsEntry("cursor", "cursor_1");

        server.enqueue(200, cursorPage("{\"id\":\"msg_1\"}", null));
        dlq.listAll().stream().toList();
        assertThat(server.last().query()).isEmpty();
    }

    @Test
    void getStatsBindsByEndpointAndByEventType() {
        server.enqueue(200, """
                {"data":{"total":42,"byReason":{"exhausted":30,"circuit_open":12},
                 "byEndpoint":[{"endpointId":"ep_1","endpointUrl":"https://failing.com/hook","count":25}],
                 "byEventType":[{"eventType":"order.created","count":40}]}}""");
        DlqStats s = dlq.getStats();
        assertThat(server.last().path()).isEqualTo("/api/outbound-messages/dlq/stats");
        assertThat(s.total()).isEqualTo(42);
        assertThat(s.byReason()).containsEntry("exhausted", 30L);
        assertThat(s.byEndpoint().getFirst().endpointUrl()).isEqualTo("https://failing.com/hook");
        assertThat(s.byEndpoint().getFirst().count()).isEqualTo(25);
        assertThat(s.byEventType().getFirst().eventType()).isEqualTo("order.created");

        server.enqueue(200, "{\"data\":{\"total\":0}}");
        DlqStats empty = dlq.getStats();
        assertThat(empty.byEndpoint()).isEmpty();
        assertThat(empty.byEventType()).isEmpty();
        assertThat(empty.byReason()).isEmpty();
    }

    @Test
    void retryAndRetryBulk() {
        server.enqueue(201, "{\"data\":{\"originalMessageId\":\"msg_1\",\"newMessageId\":\"msg_retry_1\",\"status\":\"queued\"}}");
        MessageReplayResult r = dlq.retry("msg_1");
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/outbound-messages/dlq/msg_1/retry");
        assertThat(r.newMessageId()).isEqualTo("msg_retry_1");

        server.enqueue(201, """
                {"data":{"queued":2,"skipped":1,"messages":[{"originalMessageId":"msg_1","newMessageId":"msg_r1"},
                 {"originalMessageId":"msg_2","newMessageId":"msg_r2"}]}}""");
        DlqBulkRetryResult bulk = dlq.retryBulk(List.of("msg_1", "msg_2", "msg_3"));
        assertThat(server.last().path()).isEqualTo("/api/outbound-messages/dlq/retry-bulk");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"messageIds\":[\"msg_1\",\"msg_2\",\"msg_3\"]}"));
        assertThat(bulk.queued()).isEqualTo(2);
        assertThat(bulk.skipped()).isEqualTo(1);
        assertThat(bulk.messages()).extracting(m -> m.originalMessageId() + ">" + m.newMessageId())
                .containsExactly("msg_1>msg_r1", "msg_2>msg_r2");
    }

    @Test
    void deleteAndDeleteBulk() {
        server.enqueue(200, "{\"success\":true}");
        dlq.delete("msg_1");
        assertThat(server.last().method()).isEqualTo("DELETE");
        assertThat(server.last().path()).isEqualTo("/api/outbound-messages/dlq/msg_1");
        assertThat(server.last().rawBody()).isNull();

        server.enqueue(200, "{\"data\":{\"deleted\":3}}");
        DlqBulkDeleteResult d = dlq.deleteBulk(List.of("msg_1", "msg_2", "msg_3"));
        assertThat(server.last().method()).isEqualTo("DELETE");
        assertThat(server.last().path()).isEqualTo("/api/outbound-messages/dlq/bulk");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"messageIds\":[\"msg_1\",\"msg_2\",\"msg_3\"]}"));
        assertThat(d.deleted()).isEqualTo(3);
    }
}
