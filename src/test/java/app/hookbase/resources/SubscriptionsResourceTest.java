package app.hookbase.resources;

import static app.hookbase.resources.OutboundTestSupport.cursorPage;
import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.Page;
import app.hookbase.models.Subscription;
import app.hookbase.models.SubscriptionBulkResult;
import app.hookbase.params.CreateSubscriptionParams;
import app.hookbase.params.ListSubscriptionsParams;
import app.hookbase.params.UpdateSubscriptionParams;
import app.hookbase.testing.MockServer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriptionsResourceTest {

    static final String LIST_ITEM = """
            {"id":"sub_1","endpointId":"ep_1","eventTypeId":"et_1","filterExpression":null,
             "labelFilters":{"tier":["gold"]},"labelFilterMode":"any","transformId":null,"priorityOverride":1,
             "isEnabled":true,"createdAt":"2026-10-17T00:00:00Z","createdBy":"u1","apiKeyId":null,
             "endpointUrl":"https://a.example","eventTypeName":"order.created","eventTypeDisplayName":"Order Created",
             "applicationId":"app_1","applicationName":"Acme","eventVersion":"latest","eventVersionDeprecated":false,
             "eventVersionSunsetAt":null}""";

    private MockServer server;
    private SubscriptionsResource subscriptions;

    @BeforeEach
    void start() {
        server = new MockServer();
        subscriptions = new SubscriptionsResource(OutboundTestSupport.transport(server));
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void listSendsApplicationIdAndFiltersAndBindsFlatFields() {
        server.enqueue(200, cursorPage(LIST_ITEM, null));
        Page<Subscription> page = subscriptions.list("app_1",
                new ListSubscriptionsParams().endpointId("ep_1").eventTypeId("et_1").isEnabled(true).limit(5).cursor("c1"));
        assertThat(server.last().path()).isEqualTo("/api/webhook-subscriptions");
        assertThat(server.last().query()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "applicationId", "app_1", "endpointId", "ep_1", "eventTypeId", "et_1", "isEnabled", "true", "limit", "5", "cursor", "c1"));
        Subscription sub = page.data().getFirst();
        assertThat(sub.eventTypeName()).isEqualTo("order.created");
        assertThat(sub.labelFilters().get("tier").get(0).asText()).isEqualTo("gold");
        assertThat(sub.priorityOverride()).isEqualTo(1);
        assertThat(sub.applicationName()).isEqualTo("Acme");
        assertThat(sub.eventVersion()).isEqualTo("latest");
        assertThat(sub.endpoint()).isNull();
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    void listAllFollowsCursorsWithDefaultLimit100() {
        server.enqueue(200, cursorPage(LIST_ITEM, "next"));
        server.enqueue(200, cursorPage(LIST_ITEM.replace("sub_1", "sub_2"), null));
        assertThat(subscriptions.listAll("app_1").stream().map(Subscription::id).toList()).containsExactly("sub_1", "sub_2");
        assertThat(server.requests().get(0).query()).containsEntry("limit", "100").containsEntry("applicationId", "app_1");
        assertThat(server.requests().get(1).query()).containsEntry("cursor", "next");
    }

    @Test
    void getBindsTheNestedObjects() {
        server.enqueue(200, """
                {"data":{"id":"sub_1","endpointId":"ep_1","eventTypeId":"et_1","filterExpression":"$.amount > 10",
                 "priorityOverride":1,"isEnabled":true,"endpoint":{"id":"ep_1","url":"https://a.example"},
                 "eventType":{"id":"et_1","name":"order.created","displayName":"Order Created"},
                 "application":{"id":"app_1","name":"Acme"}}}""");
        Subscription sub = subscriptions.get("app_1", "sub_1");
        assertThat(server.last().path()).isEqualTo("/api/webhook-subscriptions/sub_1");
        assertThat(sub.eventType().name()).isEqualTo("order.created");
        assertThat(sub.eventType().displayName()).isEqualTo("Order Created");
        assertThat(sub.endpoint().url()).isEqualTo("https://a.example");
        assertThat(sub.application().id()).isEqualTo("app_1");
        assertThat(sub.filterExpression()).isEqualTo("$.amount > 10");
    }

    @Test
    void createUpdateDeleteEnableDisableBodies() {
        String sub = "{\"data\":{\"id\":\"s\",\"endpointId\":\"ep_1\",\"eventTypeId\":\"et_1\"}}";
        server.enqueue(201, sub);
        subscriptions.create("app_1", new CreateSubscriptionParams().endpointId("ep_1").eventTypeId("et_1")
                .labelFilters(Map.of("tier", "gold")).labelFilterMode("any").priorityOverride(0));
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/webhook-subscriptions");
        assertThat(server.last().body()).isEqualTo(MockServer.json("""
                {"endpointId":"ep_1","eventTypeId":"et_1","labelFilters":{"tier":"gold"},"labelFilterMode":"any","priorityOverride":0}"""));

        server.enqueue(200, sub);
        subscriptions.update("app_1", "s", new UpdateSubscriptionParams().filterExpression("$.a").isEnabled(false).transformId(null));
        assertThat(server.last().method()).isEqualTo("PATCH");
        assertThat(server.last().path()).isEqualTo("/api/webhook-subscriptions/s");
        assertThat(server.last().rawBody()).isEqualTo("{\"filterExpression\":\"$.a\",\"isEnabled\":false,\"transformId\":null}");

        server.enqueue(200, sub).enqueue(200, sub);
        subscriptions.enable("app_1", "s");
        assertThat(server.last().rawBody()).isEqualTo("{\"isEnabled\":true}");
        subscriptions.disable("app_1", "s");
        assertThat(server.last().rawBody()).isEqualTo("{\"isEnabled\":false}");

        server.enqueue(200, "{\"success\":true}");
        subscriptions.delete("app_1", "s");
        assertThat(server.last().method()).isEqualTo("DELETE");
        assertThat(server.last().path()).isEqualTo("/api/webhook-subscriptions/s");
    }

    @Test
    void bulkSubscribeAndSubscribeToMany() {
        server.enqueue(201, """
                {"created":2,"skipped":1,"subscriptions":[{"id":"sub_1","endpointId":"ep_1","eventTypeId":"et_1","labelFilterMode":"all"},
                 {"id":"sub_2","endpointId":"ep_1","eventTypeId":"et_2"}]}""");
        SubscriptionBulkResult result = subscriptions.bulkSubscribe("ep_1", List.of("et_1", "et_2", "et_3"));
        assertThat(server.last().path()).isEqualTo("/api/webhook-subscriptions/bulk");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"endpointId\":\"ep_1\",\"eventTypeIds\":[\"et_1\",\"et_2\",\"et_3\"]}"));
        assertThat(result.created()).isEqualTo(2);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.subscriptions()).extracting(Subscription::id).containsExactly("sub_1", "sub_2");
        assertThat(result.subscriptions().getFirst().labelFilterMode()).isEqualTo("all");

        server.enqueue(201, "{\"created\":1,\"skipped\":0,\"subscriptions\":[{\"id\":\"sub_9\"}]}");
        assertThat(subscriptions.subscribeToMany("app_1", "ep_1", List.of("et_9"))).extracting(Subscription::id).containsExactly("sub_9");
    }

    @Test
    void unsubscribeAllDeletesOnEveryPage() {
        server.enqueue(200, cursorPage("{\"id\":\"sub_1\"}", "next"));
        server.enqueue(200, cursorPage("{\"id\":\"sub_2\"}", null));
        server.otherwise(204, null);
        subscriptions.unsubscribeAll("app_1", "ep_1");
        assertThat(server.requests().getFirst().query()).containsEntry("endpointId", "ep_1").containsEntry("applicationId", "app_1");
        assertThat(server.requests().stream().filter(r -> r.method().equals("DELETE")).map(MockServer.Request::path).toList())
                .containsExactly("/api/webhook-subscriptions/sub_1", "/api/webhook-subscriptions/sub_2");
    }
}
