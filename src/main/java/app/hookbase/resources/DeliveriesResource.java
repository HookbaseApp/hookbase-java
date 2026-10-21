package app.hookbase.resources;

import app.hookbase.Page;
import app.hookbase.Pager;
import app.hookbase.RequestOptions;
import app.hookbase.internal.HttpTransport;
import app.hookbase.models.Delivery;
import app.hookbase.models.DeliveryBulkReplayResult;
import app.hookbase.models.DeliveryReplayResult;
import app.hookbase.params.ListDeliveriesParams;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Inbound deliveries: each event sent to each destination, and replaying them. */
public final class DeliveriesResource extends BaseResource {

    private static final String PATH = "/api/deliveries";

    public DeliveriesResource(HttpTransport http) {
        super(http);
    }

    /**
     * One page of deliveries ({@code limit}/{@code offset} paging). The API sends no total, so
     * {@link Page#total()} is the size of this page and {@link Page#hasMore()} is true while pages
     * come back full.
     */
    public Page<Delivery> list(ListDeliveriesParams params, RequestOptions options) {
        return page(query(params), options);
    }

    public Page<Delivery> list(ListDeliveriesParams params) {
        return list(params, null);
    }

    public Page<Delivery> list() {
        return list(null, null);
    }

    /** Every matching delivery, fetched {@code limit} (default 50) at a time from {@code offset} (default 0). */
    public Pager<Delivery> listAll(ListDeliveriesParams params, RequestOptions options) {
        Map<String, Object> q = query(params);
        int limit = q.get("limit") instanceof Number n ? n.intValue() : 50;
        int start = q.remove("offset") instanceof Number n ? n.intValue() : 0;
        q.put("limit", limit);
        return Pager.numbered(0, n -> {
            Map<String, Object> page = new LinkedHashMap<>(q);
            page.put("offset", start + n * limit);
            return page(page, options);
        });
    }

    public Pager<Delivery> listAll(ListDeliveriesParams params) {
        return listAll(params, null);
    }

    public Pager<Delivery> listAll() {
        return listAll(null, null);
    }

    public Delivery get(String deliveryId, RequestOptions options) {
        return unwrap(get(PATH + "/" + seg(deliveryId), null, options), "delivery", Delivery.class);
    }

    public Delivery get(String deliveryId) {
        return get(deliveryId, null);
    }

    /** Queue a new delivery of the same event to the same destination. */
    public DeliveryReplayResult replay(String deliveryId, RequestOptions options) {
        return as(post(PATH + "/" + seg(deliveryId) + "/replay", null, options), DeliveryReplayResult.class);
    }

    public DeliveryReplayResult replay(String deliveryId) {
        return replay(deliveryId, null);
    }

    /** Replay up to 100 deliveries. */
    public DeliveryBulkReplayResult bulkReplay(List<String> deliveryIds, RequestOptions options) {
        return as(post(PATH + "/bulk-replay", Map.of("deliveryIds", deliveryIds), options), DeliveryBulkReplayResult.class);
    }

    public DeliveryBulkReplayResult bulkReplay(List<String> deliveryIds) {
        return bulkReplay(deliveryIds, null);
    }

    /** Replay the failed deliveries of up to 50 events. */
    public DeliveryBulkReplayResult bulkReplayEvents(List<String> eventIds, RequestOptions options) {
        return as(post(PATH + "/bulk-replay-events", Map.of("eventIds", eventIds), options), DeliveryBulkReplayResult.class);
    }

    public DeliveryBulkReplayResult bulkReplayEvents(List<String> eventIds) {
        return bulkReplayEvents(eventIds, null);
    }

    private Page<Delivery> page(Map<String, Object> q, RequestOptions options) {
        JsonNode node = get(PATH, q, options);
        List<Delivery> data = list(node, "deliveries", Delivery.class);
        int limit = node != null && node.hasNonNull("limit")
                ? node.get("limit").asInt()
                : q.get("limit") instanceof Number n ? n.intValue() : 50;
        int offset = node != null && node.hasNonNull("offset")
                ? node.get("offset").asInt()
                : q.get("offset") instanceof Number n ? n.intValue() : 0;
        boolean hasMore = !data.isEmpty() && data.size() >= limit;
        return new Page<>(data, data.size(), limit, offset, hasMore, null);
    }
}
