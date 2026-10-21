package app.hookbase.resources;

import app.hookbase.ApiClient;
import app.hookbase.Page;
import app.hookbase.Pager;
import app.hookbase.RequestOptions;
import app.hookbase.models.Event;
import app.hookbase.models.EventDebugInfo;
import app.hookbase.models.EventDetail;
import app.hookbase.params.ExportEventsParams;
import app.hookbase.params.ListEventsParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Inbound webhook events: what each source received. */
public final class EventsResource extends BaseResource {

    private static final String PATH = "/api/events";

    public EventsResource(ApiClient http) {
        super(http);
    }

    /**
     * One page of events, newest first. Pass the returned {@link Page#cursor()} back as
     * {@code cursor} for the next. {@link Page#total()} is the size of this page unless
     * {@code includeTotal} was set.
     */
    public Page<Event> list(ListEventsParams params, RequestOptions options) {
        return page(query(params), options);
    }

    public Page<Event> list(ListEventsParams params) {
        return list(params, null);
    }

    public Page<Event> list() {
        return list(null, null);
    }

    /** Every matching event, fetched 100 at a time (or {@code limit}) as you iterate. */
    public Pager<Event> listAll(ListEventsParams params, RequestOptions options) {
        Map<String, Object> q = query(params);
        q.putIfAbsent("limit", 100);
        String start = (String) q.remove("cursor");
        return Pager.cursor(start, cursor -> {
            Map<String, Object> page = new LinkedHashMap<>(q);
            page.put("cursor", cursor);
            return page(page, options);
        });
    }

    public Pager<Event> listAll(ListEventsParams params) {
        return listAll(params, null);
    }

    public Pager<Event> listAll() {
        return listAll(null, null);
    }

    /** One event with its payload and deliveries. */
    public EventDetail get(String eventId, RequestOptions options) {
        JsonNode node = get(PATH + "/" + seg(eventId), null, options);
        if (node == null) return null;
        // The API returns {event, payload, transient, deliveries}; fold them into one object.
        JsonNode event = node.get("event");
        ObjectNode merged = event != null && event.isObject()
                ? ((ObjectNode) event).deepCopy()
                : JsonNodeFactory.instance.objectNode();
        if (node.has("payload")) merged.set("payload", node.get("payload"));
        if (node.has("transient")) merged.set("transient", node.get("transient"));
        JsonNode deliveries = node.get("deliveries");
        merged.set("deliveries", deliveries != null && deliveries.isArray() ? deliveries : JsonNodeFactory.instance.arrayNode());
        return as(merged, EventDetail.class);
    }

    public EventDetail get(String eventId) {
        return get(eventId, null);
    }

    /** Debug details for an event: raw payload, per-delivery diagnostics, timing and a curl command. */
    public EventDebugInfo debug(String eventId, RequestOptions options) {
        return as(get(PATH + "/" + seg(eventId) + "/debug", null, options), EventDebugInfo.class);
    }

    public EventDebugInfo debug(String eventId) {
        return debug(eventId, null);
    }

    /**
     * Export matching events. For the default JSON format this is
     * {@code {"events": [...], "exportedAt", "count"}}; with {@code format("csv")} it is a text node
     * holding the CSV (or use {@link #exportCsv}).
     */
    public JsonNode export(ExportEventsParams params, RequestOptions options) {
        return get(PATH + "/export", query(params), options);
    }

    public JsonNode export(ExportEventsParams params) {
        return export(params, null);
    }

    public JsonNode export() {
        return export(null, null);
    }

    /** Export matching events as CSV text ({@code format} is set to {@code csv}). */
    public String exportCsv(ExportEventsParams params, RequestOptions options) {
        Map<String, Object> q = query(params);
        q.put("format", "csv");
        JsonNode node = get(PATH + "/export", q, options);
        if (node == null) return "";
        return node.isTextual() ? node.asText() : node.toString();
    }

    public String exportCsv(ExportEventsParams params) {
        return exportCsv(params, null);
    }

    public String exportCsv() {
        return exportCsv(null, null);
    }

    private Page<Event> page(Map<String, Object> query, RequestOptions options) {
        Map<String, Object> q = new LinkedHashMap<>(query);
        q.remove("offset"); // keyset-paginated: an offset is ignored and would repeat page one
        JsonNode node = get(PATH, q, options);
        List<Event> data = list(node, "events", Event.class);
        String cursor = node != null && node.hasNonNull("nextCursor") ? node.get("nextCursor").asText() : null;
        boolean hasMore = node != null && node.path("hasMore").asBoolean(false) && cursor != null;
        int total = node != null && node.hasNonNull("total") ? node.get("total").asInt() : data.size();
        int limit = node != null && node.hasNonNull("limit")
                ? node.get("limit").asInt()
                : q.get("limit") instanceof Number n ? n.intValue() : 50;
        return new Page<>(data, total, limit, 0, hasMore, cursor);
    }
}
