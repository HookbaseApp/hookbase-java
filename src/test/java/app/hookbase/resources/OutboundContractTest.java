package app.hookbase.resources;

import static app.hookbase.resources.OutboundTestSupport.cursorPage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.hookbase.Page;
import app.hookbase.RequestOptions;
import app.hookbase.errors.HookbaseApiException;
import app.hookbase.errors.RateLimitException;
import app.hookbase.models.Endpoint;
import app.hookbase.models.OutboundMessage;
import app.hookbase.models.SubscriptionBulkResult;
import app.hookbase.params.CreateEndpointParams;
import app.hookbase.params.CreateEventTypeParams;
import app.hookbase.params.ListMessagesParams;
import app.hookbase.params.SendMessageParams;
import app.hookbase.params.UpdateEndpointParams;
import app.hookbase.models.EndpointHeader;
import app.hookbase.testing.MockServer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The outbound surface checked against the API routes it calls, ported from the Node, Go and Python
 * outbound contract tests. Every mocked response is the shape those routes build.
 */
class OutboundContractTest {

    static final String SEND_OK = MessagesResourceTest.SEND_OK;
    static final String UUID_V4 = "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$";

    private MockServer server;

    @BeforeEach
    void start() {
        server = new MockServer();
    }

    @AfterEach
    void stop() {
        server.close();
    }

    private MessagesResource messages(int retries) {
        return new MessagesResource(OutboundTestSupport.transport(server, retries));
    }

    private static SendMessageParams send() {
        return new SendMessageParams().eventType("x").payload(Map.of());
    }

    // ---- messages.send: targeting ----

    @Test
    void sendsEndpointIdsPlusTheSingularWhenThereIsOneTarget() {
        server.enqueue(201, SEND_OK);
        messages(0).send("app_1", send().endpointIds(List.of("ep_1")));
        JsonNode body = server.last().body();
        assertThat(body.get("endpointIds")).isEqualTo(MockServer.json("[\"ep_1\"]"));
        assertThat(body.get("endpointId").asText()).isEqualTo("ep_1");
    }

    @Test
    void sendsOnlyEndpointIdsForSeveralTargetsMergingAndDeduplicating() {
        server.enqueue(201, SEND_OK);
        messages(0).send("app_1", send().endpointIds(List.of("ep_1", "ep_2", "ep_1")).endpointId("ep_3"));
        JsonNode body = server.last().body();
        assertThat(body.get("endpointIds")).isEqualTo(MockServer.json("[\"ep_1\",\"ep_2\",\"ep_3\"]"));
        assertThat(body.has("endpointId")).isFalse();

        server.enqueue(201, SEND_OK);
        messages(0).send("app_1", send().endpointIds(List.of("ep_1")).endpointId("ep_1"));
        assertThat(server.last().body().get("endpointIds")).isEqualTo(MockServer.json("[\"ep_1\"]"));
        assertThat(server.last().body().get("endpointId").asText()).isEqualTo("ep_1");
    }

    @Test
    void sendsNoTargetingFieldsWhenNoneAreGivenAndNeverAnEventId() {
        server.enqueue(201, SEND_OK);
        messages(0).send("app_1", send().endpointIds(List.of()).extra("eventId", "evt_mine"));
        JsonNode body = server.last().body();
        assertThat(body.has("endpointIds")).isFalse();
        assertThat(body.has("endpointId")).isFalse();
        assertThat(body.has("eventId")).isFalse();
    }

    @Test
    void sendsLabelsPriorityAndMetadata() {
        server.enqueue(201, SEND_OK);
        messages(0).send("app_1", new SendMessageParams().eventType("order.created").payload(Map.of("id", 1))
                .labels(Map.of("tier", "gold")).priority(2).metadata(Map.of("k", "v")).idempotencyKey("k1"));
        assertThat(server.last().body()).isEqualTo(MockServer.json("""
                {"eventType":"order.created","payload":{"id":1},"labels":{"tier":"gold"},"priority":2,
                 "metadata":{"k":"v"},"idempotencyKey":"k1","applicationId":"app_1"}"""));
    }

    @Test
    void omitsAnEmptyApplicationId() {
        server.enqueue(201, SEND_OK).enqueue(201, SEND_OK);
        messages(0).send("", send());
        assertThat(server.last().body().has("applicationId")).isFalse();
        messages(0).send(null, send());
        assertThat(server.last().body().has("applicationId")).isFalse();
    }

    // ---- messages.send: idempotency key ----

    @Test
    void putsTheCallerKeyInBothTheBodyAndTheHeader() {
        server.enqueue(201, SEND_OK);
        messages(0).send("app_1", send().idempotencyKey("order-42"));
        assertThat(server.last().body().get("idempotencyKey").asText()).isEqualTo("order-42");
        assertThat(server.last().header("Idempotency-Key")).isEqualTo("order-42");
    }

    @Test
    void prefersTheParamsKeyOverTheRequestOption() {
        server.enqueue(201, SEND_OK).enqueue(201, SEND_OK);
        messages(0).send("app_1", send().idempotencyKey("body_key"), RequestOptions.builder().idempotencyKey("option_key").build());
        assertThat(server.last().body().get("idempotencyKey").asText()).isEqualTo("body_key");
        assertThat(server.last().header("Idempotency-Key")).isEqualTo("body_key");

        messages(0).send("app_1", send(), RequestOptions.builder().idempotencyKey("option_key").build());
        assertThat(server.last().body().get("idempotencyKey").asText()).isEqualTo("option_key");
        assertThat(server.last().header("Idempotency-Key")).isEqualTo("option_key");
    }

    @Test
    void generatesOneKeyPerCallAndReusesItOnEveryRetry() {
        server.enqueue(503, "{\"error\":\"unavailable\"}").enqueue(502, "{\"error\":\"bad gateway\"}").enqueue(201, SEND_OK);
        MessagesResource retrying = messages(2);
        retrying.send("app_1", send());

        assertThat(server.requests()).hasSize(3);
        List<String> keys = server.requests().stream().map(r -> r.body().get("idempotencyKey").asText()).toList();
        List<String> headerKeys = server.requests().stream().map(r -> r.header("Idempotency-Key")).toList();
        assertThat(keys.getFirst()).matches(UUID_V4);
        assertThat(keys).containsOnly(keys.getFirst());
        assertThat(headerKeys).isEqualTo(keys);

        server.enqueue(201, SEND_OK);
        retrying.send("app_1", send());
        assertThat(server.last().body().get("idempotencyKey").asText()).isNotEqualTo(keys.getFirst());
    }

    @Test
    void returnsTheApiFields() {
        server.enqueue(201, SEND_OK);
        var res = messages(0).send("app_1", send());
        assertThat(res.eventId()).isEqualTo("evt_1");
        assertThat(res.messagesQueued()).isEqualTo(2);
        assertThat(res.endpoints()).extracting(e -> e.id()).containsExactly("ep_1", "ep_2");
    }

    // ---- retry policy ----

    @Test
    void doesNotRetryAPostWithoutAnIdempotencyKeyAfterA5xx() {
        server.enqueue(502, "{\"error\":\"boom\"}");
        EndpointsResource endpoints = new EndpointsResource(OutboundTestSupport.transport(server, 3));
        assertThatThrownBy(() -> endpoints.rotateSecret("app_1", "ep_1"))
                .isInstanceOfSatisfying(HookbaseApiException.class, e -> assertThat(e.status()).isEqualTo(502));
        assertThat(server.requests()).hasSize(1);

        server.enqueue(500, "{\"error\":\"boom\"}");
        assertThatThrownBy(() -> messages(3).retry("app_1", "msg_1")).isInstanceOf(HookbaseApiException.class);
        assertThat(server.requests()).hasSize(2);
    }

    @Test
    void retriesAPostWithoutAKeyOn429() {
        server.enqueue(429, "{\"error\":\"slow down\"}", Map.of("Retry-After", "0", "Content-Type", "application/json"))
                .enqueue(201, "{\"data\":{\"originalMessageId\":\"a\",\"newMessageId\":\"b\",\"status\":\"queued\"}}");
        assertThat(messages(1).retry("app_1", "a").newMessageId()).isEqualTo("b");
        assertThat(server.requests()).hasSize(2);
    }

    @Test
    void still429sOutWhenRetriesAreExhausted() {
        server.otherwise(429, "{\"error\":\"slow down\"}");
        assertThatThrownBy(() -> messages(0).retry("app_1", "a")).isInstanceOf(RateLimitException.class);
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void doesNotRetryAPostOnA5xxEvenWhenTheCallerPassesRetries() {
        server.enqueue(503, "{\"error\":\"boom\"}")
                .enqueue(201, "{\"data\":{\"originalMessageId\":\"a\",\"newMessageId\":\"b\",\"status\":\"queued\"}}");
        DlqResource dlq = new DlqResource(OutboundTestSupport.transport(server, 0));
        assertThatThrownBy(() -> dlq.retry("a", RequestOptions.builder().retries(1).build()))
                .isInstanceOf(HookbaseApiException.class);
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void stillRetriesGetsAfterA5xx() {
        server.enqueue(500, "{\"error\":\"boom\"}").enqueue(200, "{\"data\":{\"id\":\"ep_1\",\"headers\":[]}}");
        EndpointsResource endpoints = new EndpointsResource(OutboundTestSupport.transport(server, 1));
        assertThat(endpoints.get("app_1", "ep_1").id()).isEqualTo("ep_1");
        assertThat(server.requests()).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(ints = {409, 413})
    void doesNotRetryA4xx(int status) {
        server.enqueue(status, "{\"error\":\"Payload too large\"}");
        assertThatThrownBy(() -> messages(3).send("app_1", send()))
                .isInstanceOfSatisfying(HookbaseApiException.class, e -> assertThat(e.status()).isEqualTo(status));
        assertThat(server.requests()).hasSize(1);
    }

    // ---- pagination ----

    @Test
    void listReachesPageTwoByPassingTheCursorBackVerbatim() {
        String opaque = "eyJ0cyI6IjIwMjYtMTAtMTdUMDA6MDA6MDAuMDAwMDAxWiIsImlkIjoibXNnXzIifQ==";
        server.enqueue(200, cursorPage("{\"id\":\"msg_1\"}", opaque));
        server.enqueue(200, cursorPage("{\"id\":\"msg_2\"}", null));
        MessagesResource m = messages(0);
        Page<OutboundMessage> one = m.list("app_1", new ListMessagesParams().limit(1));
        Page<OutboundMessage> two = m.list("app_1", new ListMessagesParams().limit(1).cursor(one.cursor()));
        assertThat(server.last().query()).containsEntry("cursor", opaque).doesNotContainKey("offset");
        assertThat(two.data()).extracting(OutboundMessage::id).containsExactly("msg_2");
        assertThat(two.hasMore()).isFalse();
    }

    @Test
    void listsSendACursorNeverAnOffset() {
        server.otherwise(200, cursorPage("", null));
        new EndpointsResource(OutboundTestSupport.transport(server)).list("app_1", new app.hookbase.params.ListEndpointsParams().extra("offset", 20));
        new SubscriptionsResource(OutboundTestSupport.transport(server)).list("app_1", new app.hookbase.params.ListSubscriptionsParams().extra("offset", 20));
        new EventTypesResource(OutboundTestSupport.transport(server)).list(new app.hookbase.params.ListEventTypesParams().extra("offset", 20));
        new DlqResource(OutboundTestSupport.transport(server)).list(new app.hookbase.params.ListDlqParams().extra("offset", 20));
        messages(0).list("app_1", new ListMessagesParams().extra("offset", 20));
        assertThat(server.requests()).hasSize(5).allSatisfy(r -> assertThat(r.query()).doesNotContainKey("offset"));
    }

    @Test
    void listAllStartsFromTheCallersCursor() {
        server.enqueue(200, cursorPage("{\"id\":\"msg_5\"}", null));
        assertThat(messages(0).listAll("app_1", new ListMessagesParams().cursor("start")).stream().map(OutboundMessage::id).toList())
                .containsExactly("msg_5");
        assertThat(server.last().query()).containsEntry("cursor", "start");
    }

    @Test
    void stopsWhenHasMoreIsTrueButNoCursorCameBack() {
        server.enqueue(200, "{\"data\":[{\"id\":\"msg_1\"}],\"pagination\":{\"hasMore\":true,\"nextCursor\":null}}");
        server.otherwise(200, cursorPage("{\"id\":\"loop\"}", null));
        assertThat(messages(0).listAll("app_1").stream().map(OutboundMessage::id).toList()).containsExactly("msg_1");
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void getByNameFindsAnExactNamePastTheFirstPageOfSearchResults() {
        server.enqueue(200, cursorPage("{\"id\":\"et_1\",\"name\":\"order.created.v2\"},{\"id\":\"et_2\",\"name\":\"order.created.old\"}", "p2"));
        server.enqueue(200, cursorPage("{\"id\":\"et_3\",\"name\":\"order.created\"}", null));
        EventTypesResource eventTypes = new EventTypesResource(OutboundTestSupport.transport(server));
        assertThat(eventTypes.getByName("order.created").id()).isEqualTo("et_3");
        assertThat(server.requests()).hasSize(2);
        assertThat(server.last().query()).containsEntry("cursor", "p2").containsEntry("search", "order.created");
    }

    @Test
    void unsubscribeAllDeletesSubscriptionsOnEveryPage() {
        server.enqueue(200, cursorPage("{\"id\":\"sub_1\"},{\"id\":\"sub_2\"}", "p2"));
        server.enqueue(200, cursorPage("{\"id\":\"sub_3\"}", null));
        server.otherwise(200, "{\"success\":true}");
        new SubscriptionsResource(OutboundTestSupport.transport(server)).unsubscribeAll("app_1", "ep_1");
        assertThat(server.requests().stream().filter(r -> r.method().equals("DELETE")).map(MockServer.Request::path))
                .containsExactly("/api/webhook-subscriptions/sub_1", "/api/webhook-subscriptions/sub_2", "/api/webhook-subscriptions/sub_3");
    }

    // ---- field mappings ----

    @ParameterizedTest
    @ValueSource(strings = {"pending", "processing", "success", "failed", "exhausted", "dlq"})
    void acceptsEveryStatusTheApiUses(String status) {
        server.enqueue(200, cursorPage("{\"id\":\"m\",\"status\":\"" + status + "\"}", null));
        assertThat(messages(0).list("app_1", new ListMessagesParams().status(status)).data().getFirst().status()).isEqualTo(status);
        assertThat(server.last().query()).containsEntry("status", status);
    }

    @Test
    void messageAttemptsDecodeHeadersSentAsJsonText() {
        server.enqueue(200, "{\"data\":[{\"id\":\"a\",\"messageId\":\"m\",\"attemptNumber\":1,"
                + "\"responseHeaders\":\"{\\\"content-type\\\":\\\"text/plain\\\"}\",\"requestHeaders\":null}]}");
        var attempt = messages(0).listAttempts("app_1", "m").getFirst();
        assertThat(attempt.responseHeaders()).containsEntry("content-type", "text/plain");
        assertThat(attempt.requestHeaders()).isNull();
    }

    @Test
    void messageMetadataDecodesFromJsonText() {
        server.enqueue(200, "{\"data\":{\"id\":\"m\",\"metadata\":\"{\\\"replayedFrom\\\":\\\"om_0\\\"}\"}}");
        assertThat(messages(0).get("app_1", "m").metadata().get("replayedFrom").asText()).isEqualTo("om_0");
    }

    @Test
    void reportsAwaitingRetryInTheStatsSummary() {
        server.enqueue(200, "{\"data\":{\"pending\":0,\"processing\":0,\"success\":0,\"failed\":0,\"awaitingRetry\":3,\"exhausted\":0,\"dlq\":0,\"total\":3}}");
        assertThat(messages(0).getStatsSummary().awaitingRetry()).isEqualTo(3);
    }

    @Test
    void returnsCsvAsTextAndJsonAsTheParsedObject() {
        server.enqueue(200, "id,status\nmsg_1,success\n", Map.of("Content-Type", "text/csv; charset=utf-8"));
        assertThat(messages(0).exportCsv()).isEqualTo("id,status\nmsg_1,success\n");
        assertThat(server.last().query()).containsEntry("format", "csv");

        server.enqueue(200, "{\"exportedAt\":\"t\",\"organization\":\"acme\",\"count\":0,\"events\":[]}");
        var export = messages(0).export(new app.hookbase.params.ExportMessagesParams().extra("format", "csv"));
        assertThat(export.events()).isEmpty();
        assertThat(server.last().query()).doesNotContainKey("format");
    }

    @Test
    void returnsAnEmptyListWhenEveryEventTypeWasAlreadySubscribed() {
        server.otherwise(200, "{\"created\":0,\"skipped\":2}");
        SubscriptionsResource subs = new SubscriptionsResource(OutboundTestSupport.transport(server));
        assertThat(subs.subscribeToMany("app_1", "ep_1", List.of("et_1", "et_2"))).isEmpty();
        SubscriptionBulkResult full = subs.bulkSubscribe("ep_1", List.of("et_1", "et_2"));
        assertThat(full.subscriptions()).isEmpty();
        assertThat(full.skipped()).isEqualTo(2);
    }

    @Test
    void rotateSecretSendsAZeroGracePeriod() {
        server.enqueue(200, "{\"secret\":\"whsec_n\",\"previousSecretExpiresAt\":null,\"secretVersion\":2}");
        var res = new EndpointsResource(OutboundTestSupport.transport(server))
                .rotateSecret("app_1", "ep_1", new app.hookbase.params.RotateEndpointSecretParams().gracePeriodSeconds(0));
        assertThat(server.last().rawBody()).isEqualTo("{\"gracePeriodSeconds\":0}");
        assertThat(res.previousSecretExpiresAt()).isNull();
        assertThat(res.secretVersion()).isEqualTo(2);
    }

    // ---- event types ----

    @Test
    void sendsTheNewerCreateFieldsWithExamplePayloadAsAJsonString() {
        server.enqueue(201, "{\"data\":{\"id\":\"et_1\",\"name\":\"order.created\"}}");
        new EventTypesResource(OutboundTestSupport.transport(server)).create(new CreateEventTypeParams().name("order.created")
                .examplePayload(Map.of("id", 1)).documentationUrl("https://docs.example/order").defaultPriority(1).isEnabled(false));
        assertThat(server.last().body()).isEqualTo(MockServer.json("""
                {"name":"order.created","examplePayload":"{\\"id\\":1}","documentationUrl":"https://docs.example/order",
                 "defaultPriority":1,"isEnabled":false}"""));
    }

    // ---- endpoint wire format ----

    @Test
    void createSendsEveryAcceptedFieldUnderItsExactApiName() {
        server.enqueue(201, "{\"data\":{\"id\":\"ep_1\"}}");
        new EndpointsResource(OutboundTestSupport.transport(server)).create("app_1", new CreateEndpointParams()
                .url("https://example.com/hook").description("d").headers(List.of(new EndpointHeader("X-A", "1")))
                .timeoutSeconds(30).rateLimitPerSecond(0).successStatusCodes(List.of(200, "2xx")).backoffType("fixed")
                .retryDelays(List.of(1, 2)).ipAllowlistNotes("n").useStaticIp(true).circuitFailureThreshold(5)
                .circuitSuccessThreshold(2).circuitCooldownSeconds(60));
        assertThat(server.last().body()).isEqualTo(MockServer.json("""
                {"url":"https://example.com/hook","description":"d","headers":[{"name":"X-A","value":"1"}],
                 "timeoutSeconds":30,"rateLimitPerSecond":0,"successStatusCodes":[200,"2xx"],"backoffType":"fixed",
                 "retryDelays":[1,2],"ipAllowlistNotes":"n","useStaticIp":true,"circuitFailureThreshold":5,
                 "circuitSuccessThreshold":2,"circuitCooldownSeconds":60,"applicationId":"app_1"}"""));
    }

    @Test
    void convertsAHeaderMapToNameValuePairsInInsertionOrder() {
        server.otherwise(200, "{\"data\":{\"id\":\"ep_1\"}}");
        EndpointsResource endpoints = new EndpointsResource(OutboundTestSupport.transport(server));
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Zeta", "z");
        headers.put("X-Alpha", "a");
        endpoints.create("app_1", new CreateEndpointParams().url("https://x").headers(headers));
        assertThat(server.last().rawBody()).contains("\"headers\":[{\"name\":\"X-Zeta\",\"value\":\"z\"},{\"name\":\"X-Alpha\",\"value\":\"a\"}]");

        endpoints.update("app_1", "ep_1", new UpdateEndpointParams().headers(Map.of()));
        assertThat(server.last().rawBody()).isEqualTo("{\"headers\":[]}");
    }

    @Test
    void updateSendsNullsToClear() {
        server.enqueue(200, "{\"data\":{\"id\":\"ep_1\"}}");
        new EndpointsResource(OutboundTestSupport.transport(server)).update("app_1", "ep_1",
                new UpdateEndpointParams().description(null).ipAllowlistNotes(null).isDisabled(true).disabledReason("maintenance"));
        assertThat(server.last().rawBody())
                .isEqualTo("{\"description\":null,\"ipAllowlistNotes\":null,\"isDisabled\":true,\"disabledReason\":\"maintenance\"}");
    }

    @Test
    void bindsSuccessStatusCodesWithNumbersAndRanges() {
        server.enqueue(200, "{\"data\":{\"id\":\"ep_1\",\"successStatusCodes\":[200,201,\"2xx\"],\"headers\":null}}");
        Endpoint ep = new EndpointsResource(OutboundTestSupport.transport(server)).get("app_1", "ep_1");
        assertThat(ep.successStatusCodes()).containsExactly("200", "201", "2xx");
        assertThat(ep.headers()).isEmpty();
    }
}
