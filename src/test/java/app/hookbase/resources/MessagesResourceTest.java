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
    void aliasesCallTheSameRoutes() {
        server.otherwise(200, cursorPage(LIST_ITEM, null));
        messages.listOutbound("app_1", "ignored", new ListMessagesParams().status("failed"));
        assertThat(server.last().query()).containsExactlyInAnyOrderEntriesOf(Map.of("applicationId", "app_1", "status", "failed"));
        messages.listAllOutbound("app_1");
        assertThat(server.last().path()).isEqualTo("/api/outbound-messages");

        server.enqueue(200, "{\"data\":" + LIST_ITEM + "}");
        assertThat(messages.getOutbound("app_1", "om_1").id()).isEqualTo("om_1");
        assertThat(server.last().path()).isEqualTo("/api/outbound-messages/om_1");
    }

    @Test
    void getBindsTheDetailFields() {
        server.enqueue(200, """
                {"data":{"id":"om_1","eventId":"evt_1","status":"success","subscriptionId":"sub_1","eventTypeId":"et_1",
                 "lastErrorType":null,"payloadKey":"outbound/x.json","payloadSizeBytes":12,"payloadHash":"abc",
                 "metadata":{"replayedFrom":"om_0"},"priority":2,"completedAt":"2026-10-17T00:00:01Z",
                 "endpointDescription":"primary","payload":{"orderId":1}}}""");
        OutboundMessage m = messages.get("app_1", "om/1");
        assertThat(server.last().path()).isEqualTo("/api/outbound-messages/om%2F1");
        assertThat(m.subscriptionId()).isEqualTo("sub_1");
        assertThat(m.payload().get("orderId").asInt()).isEqualTo(1);
        assertThat(m.metadata().get("replayedFrom").asText()).isEqualTo("om_0");
        assertThat(m.priority()).isEqualTo(2);
        assertThat(m.payloadSizeBytes()).isEqualTo(12L);
        assertThat(m.endpointDescription()).isEqualTo("primary");
    }

    @Test
    void listAttemptsBindsTheApiFieldNames() {
        server.enqueue(200, """
                {"data":[{"id":"att_1","messageId":"om_1","attemptNumber":1,"status":"failed","responseStatus":500,
                 "responseHeaders":{"content-type":"text/plain"},"responseBody":"nope","responseTimeMs":123,"totalTimeMs":130,
                 "errorType":"http_error","errorMessage":"HTTP 500","errorCode":null,"requestUrl":"https://a.example",
                 "requestHeaders":{"webhook-id":"evt_1"},"requestBodySize":42,"triggeredBy":"system","triggeredByUser":null,
                 "createdAt":"2026-10-17T00:00:00Z","completedAt":"2026-10-17T00:00:00.130Z"}]}""");
        List<MessageAttempt> attempts = messages.listAttempts("app_1", "om_1");
        assertThat(server.last().path()).isEqualTo("/api/outbound-messages/om_1/attempts");
        MessageAttempt a = attempts.getFirst();
        assertThat(a.messageId()).isEqualTo("om_1");
        assertThat(a.responseTimeMs()).isEqualTo(123);
        assertThat(a.errorMessage()).isEqualTo("HTTP 500");
        assertThat(a.responseHeaders()).containsEntry("content-type", "text/plain");
        assertThat(a.requestHeaders()).containsEntry("webhook-id", "evt_1");
        assertThat(a.triggeredByUser()).isNull();

        server.enqueue(200, "{\"data\":[]}");
        assertThat(messages.listAttempts("app_1", "om_1")).isEmpty();
    }

    @Test
    void retryAndResendPostToReplay() {
        String replay = "{\"data\":{\"originalMessageId\":\"msg_1\",\"newMessageId\":\"msg_2\",\"status\":\"queued\"}}";
        server.enqueue(201, replay).enqueue(201, replay);
        MessageReplayResult r = messages.retry("app_1", "msg_1");
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/outbound-messages/msg_1/replay");
        assertThat(r).isEqualTo(new MessageReplayResult("msg_1", "msg_2", "queued"));
        assertThat(messages.resend("app_1", "msg_1").newMessageId()).isEqualTo("msg_2");
        assertThat(server.last().path()).isEqualTo("/api/outbound-messages/msg_1/replay");
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
