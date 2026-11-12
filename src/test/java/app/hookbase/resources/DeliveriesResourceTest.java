package app.hookbase.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.hookbase.Page;
import app.hookbase.errors.HookbaseException;
import app.hookbase.models.Delivery;
import app.hookbase.models.DeliveryBulkReplayResult;
import app.hookbase.models.DeliveryReplayResult;
import app.hookbase.models.ReplayTransformOverride;
import app.hookbase.params.ListDeliveriesParams;
import app.hookbase.params.ReplayDeliveryParams;
import app.hookbase.testing.MockServer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeliveriesResourceTest {

    static final String DELIVERY = """
            {"id":"del_1","organizationId":"org_1","eventId":"evt_1","routeId":"rt_1","destinationId":"dst_1",
             "status":"failed","attemptCount":3,"maxAttempts":5,"nextRetryAt":null,"responseStatus":502,
             "responseHeaders":"{\\"retry-after\\":\\"30\\"}","responseBody":"bad gateway","latencyMs":120,
             "errorMessage":"HTTP 502","deliveredAt":null,"createdAt":"2026-01-01T00:00:00Z",
             "updatedAt":"2026-01-01T00:01:00Z","destinationName":"Slack","eventType":"push"}""";

    private MockServer server;
    private DeliveriesResource deliveries;

    @BeforeEach
    void start() {
        server = new MockServer();
        deliveries = new DeliveriesResource(DataAdminTestSupport.transport(server));
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void listSendsOffsetPagingAndFilters() {
        server.enqueue(200, "{\"deliveries\":[" + DELIVERY + "],\"limit\":1,\"offset\":10}");
        Page<Delivery> page = deliveries.list(new ListDeliveriesParams().limit(1).offset(10).eventId("evt_1")
                .routeId("rt_1").destinationId("dst_1").status("failed"));
        MockServer.Request req = server.last();
        assertThat(req.method()).isEqualTo("GET");
        assertThat(req.path()).isEqualTo("/api/deliveries");
        assertThat(req.query()).containsExactlyInAnyOrderEntriesOf(Map.of("limit", "1", "offset", "10", "eventId", "evt_1",
                "routeId", "rt_1", "destinationId", "dst_1", "status", "failed"));
        assertThat(page.offset()).isEqualTo(10);
        assertThat(page.limit()).isEqualTo(1);
        assertThat(page.hasMore()).isTrue();
        Delivery d = page.data().getFirst();
        assertThat(d.attemptCount()).isEqualTo(3);
        assertThat(d.responseStatus()).isEqualTo(502);
        assertThat(d.latencyMs()).isEqualTo(120L);
        assertThat(d.responseHeaders().get("retry-after").asText()).isEqualTo("30");
        assertThat(d.errorMessage()).isEqualTo("HTTP 502");
    }

    @Test
    void shortPageHasNoMore() {
        server.enqueue(200, "{\"deliveries\":[" + DELIVERY + "],\"limit\":50,\"offset\":0}");
        assertThat(deliveries.list().hasMore()).isFalse();
    }

    @Test
    void listAllStepsOffsetByLimitFromCallersOffset() {
        server.enqueue(200, "{\"deliveries\":[" + DELIVERY + "," + DELIVERY + "],\"limit\":2,\"offset\":4}");
        server.enqueue(200, "{\"deliveries\":[" + DELIVERY + "],\"limit\":2,\"offset\":6}");
        List<Delivery> all = deliveries.listAll(new ListDeliveriesParams().limit(2).offset(4).status("failed")).stream().toList();
        assertThat(all).hasSize(3);
        assertThat(server.requests()).hasSize(2);
        assertThat(server.requests().get(0).query()).containsEntry("offset", "4").containsEntry("limit", "2").containsEntry("status", "failed");
        assertThat(server.requests().get(1).query()).containsEntry("offset", "6");
    }

    @Test
    void listAllFollowsTheServersClampedLimitWithoutSkippingRows() {
        // The API caps limit at 100 and echoes the cap; the next offset must follow the rows returned.
        String hundred = String.join(",", java.util.Collections.nCopies(100, DELIVERY));
        server.enqueue(200, "{\"deliveries\":[" + hundred + "],\"limit\":100,\"offset\":0}");
        server.enqueue(200, "{\"deliveries\":[" + DELIVERY + "],\"limit\":100,\"offset\":100}");
        assertThat(deliveries.listAll(new ListDeliveriesParams().limit(200)).stream().count()).isEqualTo(101);
        assertThat(server.requests().get(1).query()).containsEntry("offset", "100");
    }

    @Test
    void listAllDefaultsToLimit50FromZero() {
        server.enqueue(200, "{\"deliveries\":[],\"limit\":50,\"offset\":0}");
        assertThat(deliveries.listAll().stream().toList()).isEmpty();
        assertThat(server.last().query()).containsEntry("limit", "50").containsEntry("offset", "0");
    }

    @Test
    void getUnwrapsDelivery() {
        server.enqueue(200, "{\"delivery\":{\"id\":\"del_1\",\"status\":\"delivered\",\"responseStatus\":200,\"destinationUrl\":\"https://x\"}}");
        Delivery d = deliveries.get("del_1");
        assertThat(server.last().path()).isEqualTo("/api/deliveries/del_1");
        assertThat(d.destinationUrl()).isEqualTo("https://x");
    }

    @Test
    void replayAndBulkReplays() {
        server.enqueue(200, "{\"deliveryId\":\"del_new\",\"message\":\"Replay queued\",\"modified\":false,\"destinationOverridden\":false,"
                + "\"transformOverridden\":false,\"transformPersisted\":false,\"headersOverridden\":false,\"targetDestinationId\":\"dst_1\"}");
        DeliveryReplayResult r = deliveries.replay("del_1");
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/deliveries/del_1/replay");
        assertThat(r.deliveryId()).isEqualTo("del_new");
        assertThat(r.targetDestinationId()).isEqualTo("dst_1");

        server.enqueue(200, "{\"message\":\"1 deliveries queued for replay, 1 skipped\",\"queued\":1,\"skipped\":1,\"results\":["
                + "{\"originalId\":\"del_1\",\"newId\":\"del_9\",\"status\":\"queued\"},"
                + "{\"originalId\":\"del_2\",\"newId\":\"\",\"status\":\"skipped\",\"reason\":\"Event payload not found\"}]}");
        DeliveryBulkReplayResult bulk = deliveries.bulkReplay(List.of("del_1", "del_2"));
        assertThat(server.last().path()).isEqualTo("/api/deliveries/bulk-replay");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"deliveryIds\":[\"del_1\",\"del_2\"]}"));
        assertThat(bulk.results().get(1).reason()).isEqualTo("Event payload not found");
        assertThat(bulk.eventsProcessed()).isNull();

        server.enqueue(200, "{\"message\":\"m\",\"queued\":2,\"skipped\":0,\"eventsProcessed\":2,\"overrides\":{\"destinationOverride\":null},\"results\":[]}");
        DeliveryBulkReplayResult ev = deliveries.bulkReplayEvents(List.of("evt_1", "evt_2"));
        assertThat(server.last().path()).isEqualTo("/api/deliveries/bulk-replay-events");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"eventIds\":[\"evt_1\",\"evt_2\"]}"));
        assertThat(ev.eventsProcessed()).isEqualTo(2);
        assertThat(ev.overrides().has("destinationOverride")).isTrue();
    }

    static final String REPLAYED = "{\"deliveryId\":\"del_new\",\"message\":\"Replay queued\",\"modified\":true,"
            + "\"destinationOverridden\":true,\"transformOverridden\":true,\"transformPersisted\":false,"
            + "\"headersOverridden\":true,\"targetDestinationId\":\"dst_2\"}";

    @Test
    void replayWithEditsSendsEveryChangeUnderItsWireName() {
        server.enqueue(200, REPLAYED);
        DeliveryReplayResult r = deliveries.replay("del_1", new ReplayDeliveryParams()
                .payloadPatch(Map.of("status", "paid"))
                .destinationOverride("backup-dest")
                .transformOverride(new ReplayTransformOverride("{ \"id\": id }", "jsonata"))
                .headersOverride(Map.of("X-Replay", "1"))
                .persistTransform(false));

        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/deliveries/del_1/replay");
        // A null component of the override is left out, not sent as null.
        assertThat(server.last().body()).isEqualTo(MockServer.json("""
                {"payloadPatch":{"status":"paid"},"destinationOverride":"backup-dest",
                 "transformOverride":{"code":"{ \\"id\\": id }","type":"jsonata"},
                 "headersOverride":{"X-Replay":"1"},"persistTransform":false}"""));
        assertThat(r.modified()).isTrue();
        assertThat(r.destinationOverridden()).isTrue();
        assertThat(r.transformOverridden()).isTrue();
        assertThat(r.headersOverridden()).isTrue();
        assertThat(r.targetDestinationId()).isEqualTo("dst_2");
    }

    @Test
    void replayWithAModifiedPayloadSendsAnyJsonValue() {
        server.enqueue(200, REPLAYED);
        deliveries.replay("del_1", new ReplayDeliveryParams().modifiedPayload(List.of(1, 2)));
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"modifiedPayload\":[1,2]}"));
    }

    @Test
    void replayWithBothPayloadEditsFailsBeforeAnyRequest() {
        ReplayDeliveryParams both = new ReplayDeliveryParams().modifiedPayload(Map.of("a", 1)).payloadPatch(Map.of("b", 2));
        assertThatThrownBy(() -> deliveries.replay("del_1", both))
                .isInstanceOf(HookbaseException.class)
                .hasMessageContaining("modifiedPayload")
                .hasMessageContaining("payloadPatch");
        // A JSON null sent through extra() counts as set, as it does to the API.
        ReplayDeliveryParams withNull = new ReplayDeliveryParams().extra("modifiedPayload", null).payloadPatch(Map.of("b", 2));
        assertThatThrownBy(() -> deliveries.replay("del_1", withNull)).isInstanceOf(HookbaseException.class);
        assertThat(server.requests()).isEmpty();
    }

    @Test
    void aNullReplayEditIsLeftOutNotSentAsJsonNull() {
        // The API takes any field that is present as an edit: a JSON null modifiedPayload would
        // replay a null body to the real destination.
        ReplayDeliveryParams nulls = new ReplayDeliveryParams()
                .modifiedPayload(null).payloadPatch(null).destinationOverride(null)
                .transformOverride(null).headersOverride(null);
        assertThat(nulls.toMap()).isEmpty();

        // null also drops a value set earlier, so a patch can follow a cleared modifiedPayload.
        ReplayDeliveryParams cleared = new ReplayDeliveryParams()
                .modifiedPayload(Map.of("a", 1)).modifiedPayload(null)
                .payloadPatch(Map.of("b", 2))
                .destinationOverride("dst_2").destinationOverride(null)
                .transformOverride(new ReplayTransformOverride("x", "jsonata")).transformOverride(null)
                .headersOverride(Map.of("X-A", "1")).headersOverride(null);
        assertThat(cleared.toMap()).containsOnlyKeys("payloadPatch");

        server.enqueue(200, REPLAYED).enqueue(200, REPLAYED);
        deliveries.replay("del_1", nulls);
        deliveries.replay("del_1", cleared);
        assertThat(server.requests().get(0).rawBody()).isNullOrEmpty();
        assertThat(server.requests().get(1).body()).isEqualTo(MockServer.json("{\"payloadPatch\":{\"b\":2}}"));
    }

    @Test
    void replayWithNoOrEmptyParamsSendsNoBody() {
        server.enqueue(200, REPLAYED);
        server.enqueue(200, REPLAYED);
        deliveries.replay("del_1", (ReplayDeliveryParams) null);
        deliveries.replay("del_1", new ReplayDeliveryParams());
        assertThat(server.requests()).extracting(MockServer.Request::rawBody).allMatch(b -> b == null || b.isEmpty());
    }

    @Test
    void aHeldDeliveryCarriesWhenItsHoldEnds() {
        server.enqueue(200, "{\"delivery\":{\"id\":\"del_1\",\"status\":\"held\",\"nextRetryAt\":null,"
                + "\"holdExpiresAt\":\"2026-11-12T00:00:00Z\"}}");
        Delivery d = deliveries.get("del_1");
        assertThat(d.status()).isEqualTo("held");
        assertThat(d.nextRetryAt()).isNull();
        assertThat(d.holdExpiresAt()).isEqualTo("2026-11-12T00:00:00Z");
    }
}
