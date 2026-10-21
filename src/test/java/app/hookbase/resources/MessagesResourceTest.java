package app.hookbase.resources;

import static app.hookbase.resources.OutboundTestSupport.cursorPage;
import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.Page;
import app.hookbase.RequestOptions;
import app.hookbase.models.MessageAttempt;
import app.hookbase.models.MessageExport;
import app.hookbase.models.MessageReplayResult;
import app.hookbase.models.MessageStatsSummary;
import app.hookbase.models.OutboundMessage;
import app.hookbase.models.SendMessageResponse;
import app.hookbase.params.ExportMessagesParams;
import app.hookbase.params.ListMessagesParams;
import app.hookbase.params.SendMessageParams;
import app.hookbase.testing.MockServer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MessagesResourceTest {

    static final String SEND_OK = """
            {"data":{"eventId":"evt_1","messagesQueued":2,"endpoints":[{"id":"ep_1","url":"https://a.example/hook"},
             {"id":"ep_2","url":"https://b.example/hook"}]}}""";

    static final String LIST_ITEM = """
            {"id":"om_1","eventId":"evt_1","eventType":"order.created","applicationId":"app_1","endpointId":"ep_1",
             "status":"failed","attempts":2,"maxAttempts":5,"lastResponseStatus":500,"lastErrorMessage":"HTTP 500",
             "createdAt":"2026-10-17T00:00:00Z","completedAt":null,"endpointUrl":"https://a.example","applicationName":"Acme"}""";

    private MockServer server;
    private MessagesResource messages;

    @BeforeEach
    void start() {
        server = new MockServer();
        messages = new MessagesResource(OutboundTestSupport.transport(server));
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void sendPostsToSendEventWithAGeneratedKeyAndBindsTheResponse() {
        server.enqueue(201, SEND_OK);
        SendMessageResponse res = messages.send("app_1", new SendMessageParams().eventType("order.created").payload(Map.of("a", 1)));

        MockServer.Request req = server.last();
        assertThat(req.method()).isEqualTo("POST");
        assertThat(req.path()).isEqualTo("/api/send-event");
        String key = req.body().get("idempotencyKey").asText();
        assertThat(key).matches("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
        assertThat(req.header("Idempotency-Key")).isEqualTo(key);
        assertThat(req.body()).isEqualTo(MockServer.json(
                "{\"eventType\":\"order.created\",\"payload\":{\"a\":1},\"applicationId\":\"app_1\",\"idempotencyKey\":\"" + key + "\"}"));
        assertThat(res.eventId()).isEqualTo("evt_1");
        assertThat(res.messagesQueued()).isEqualTo(2);
        assertThat(res.endpoints()).extracting(e -> e.id() + " " + e.url())
                .containsExactly("ep_1 https://a.example/hook", "ep_2 https://b.example/hook");
    }

    @Test
    void sendFallsBackToTheEndpointCountForMessagesQueued() {
        server.enqueue(201, "{\"data\":{\"eventId\":\"evt_1\",\"endpoints\":[{\"id\":\"ep_1\",\"url\":\"u\"}]}}");
        assertThat(messages.send("app_1", new SendMessageParams().eventType("x").payload(Map.of())).messagesQueued()).isEqualTo(1);
    }

    @Test
    void listSendsApplicationIdAndFiltersAndBindsFields() {
        server.enqueue(200, cursorPage(LIST_ITEM, "c2"));
        Page<OutboundMessage> page = messages.list("app_1",
                new ListMessagesParams().endpointId("ep_1").status("dlq").eventType("a.b").limit(10).extra("offset", 3));
        assertThat(server.last().path()).isEqualTo("/api/outbound-messages");
        assertThat(server.last().query()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "applicationId", "app_1", "endpointId", "ep_1", "status", "dlq", "eventType", "a.b", "limit", "10"));
        OutboundMessage m = page.data().getFirst();
        assertThat(m.eventId()).isEqualTo("evt_1");
        assertThat(m.applicationName()).isEqualTo("Acme");
        assertThat(m.lastErrorMessage()).isEqualTo("HTTP 500");
        assertThat(m.attempts()).isEqualTo(2);
        assertThat(m.lastResponseStatus()).isEqualTo(500);
        assertThat(m.completedAt()).isNull();
        assertThat(m.payload()).isNull();
        assertThat(page.cursor()).isEqualTo("c2");
    }

    @Test
    void listAllFollowsCursorsWithDefaultLimit100() {
        server.enqueue(200, cursorPage(LIST_ITEM, "c/1+="));
        server.enqueue(200, cursorPage(LIST_ITEM.replace("om_1", "om_2"), null));
        assertThat(messages.listAll("app_1").stream().map(OutboundMessage::id).toList()).containsExactly("om_1", "om_2");
        assertThat(server.requests().get(0).query()).containsEntry("limit", "100").containsEntry("applicationId", "app_1").doesNotContainKey("cursor");
        assertThat(server.requests().get(1).query()).containsEntry("cursor", "c/1+=");
    }

    @Test
    void retryPostsToReplay() {
        String replay = "{\"data\":{\"originalMessageId\":\"msg_1\",\"newMessageId\":\"msg_2\",\"status\":\"queued\"}}";
        server.enqueue(201, replay);
        MessageReplayResult r = messages.retry("app_1", "msg_1");
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/outbound-messages/msg_1/replay");
        assertThat(r).isEqualTo(new MessageReplayResult("msg_1", "msg_2", "queued"));
    }

    @Test
    void getStatsSummary() {
        server.enqueue(200, "{\"data\":{\"pending\":1,\"processing\":2,\"success\":3,\"failed\":4,\"awaitingRetry\":5,\"exhausted\":6,\"dlq\":7,\"total\":28}}");
        MessageStatsSummary s = messages.getStatsSummary();
        assertThat(server.last().path()).isEqualTo("/api/outbound-messages/stats/summary");
        assertThat(s).isEqualTo(new MessageStatsSummary(1, 2, 3, 4, 5, 6, 7, 28));
    }

    @Test
    void exportJsonAndCsv() {
        server.enqueue(200, """
                {"exportedAt":"2026-10-17T00:00:00Z","organization":"acme","count":1,
                 "messages":[{"id":"msg_1","eventId":"evt_1","attempts":2,"maxAttempts":5}]}""");
        MessageExport export = messages.export(new ExportMessagesParams().type("messages").startDate("2026-10-01").limit(10));
        assertThat(server.last().path()).isEqualTo("/api/outbound-messages/export");
        assertThat(server.last().query()).containsExactlyInAnyOrderEntriesOf(Map.of("type", "messages", "startDate", "2026-10-01", "limit", "10"));
        assertThat(export.count()).isEqualTo(1);
        assertThat(export.organization()).isEqualTo("acme");
        assertThat(export.messages().getFirst().eventId()).isEqualTo("evt_1");
        assertThat(export.events()).isNull();

        server.enqueue(200, "{\"count\":1,\"events\":[{\"id\":\"evt_1\",\"eventType\":\"order.created\",\"endpointsTargeted\":3}]}");
        assertThat(messages.export().events().getFirst().endpointsTargeted()).isEqualTo(3);
        assertThat(server.last().query()).isEmpty();

        String csv = "id,eventType\nevt_1,\"order.created\"";
        server.enqueue(200, csv, Map.of("Content-Type", "text/csv"));
        assertThat(messages.exportCsv(new ExportMessagesParams().type("events"))).isEqualTo(csv);
        assertThat(server.last().query()).containsExactlyInAnyOrderEntriesOf(Map.of("format", "csv", "type", "events"));
    }

    @Test
    void sendWithRequestOptionsKeepsTimeoutAndRetries() {
        server.enqueue(500, "{\"error\":\"boom\"}").enqueue(201, SEND_OK);
        messages.send("app_1", new SendMessageParams().eventType("x").payload(Map.of()),
                RequestOptions.builder().retries(1).idempotencyKey("k1").build());
        assertThat(server.requests()).hasSize(2);
        assertThat(server.requests()).allSatisfy(r -> assertThat(r.header("Idempotency-Key")).isEqualTo("k1"));
    }
}
