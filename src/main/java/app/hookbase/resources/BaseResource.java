package app.hookbase.resources;

import app.hookbase.Page;
import app.hookbase.RequestOptions;
import app.hookbase.internal.HttpTransport;
import app.hookbase.internal.Json;
import app.hookbase.params.Params;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/** Shared plumbing for the resource classes. Not meant to be subclassed outside the SDK. */
public abstract class BaseResource {

    protected final HttpTransport http;

    protected BaseResource(HttpTransport http) {
        this.http = http;
    }

    // ---- requests ----------------------------------------------------------------------------

    protected final JsonNode get(String path, Map<String, ?> query, RequestOptions options) {
        return http.request("GET", path, query, null, options);
    }

    protected final JsonNode post(String path, Object body, RequestOptions options) {
        return http.request("POST", path, null, body, options);
    }

    protected final JsonNode patch(String path, Object body, RequestOptions options) {
        return http.request("PATCH", path, null, body, options);
    }

    protected final JsonNode put(String path, Object body, RequestOptions options) {
        return http.request("PUT", path, null, body, options);
    }

    protected final JsonNode delete(String path, Object body, RequestOptions options) {
        return http.request("DELETE", path, null, body, options);
    }

    /** One percent-encoded path segment. */
    protected static String seg(String id) {
        return HttpTransport.segment(id);
    }

    /**
     * Query parameters from a params object: scalar values and collections only (nested objects
     * have no query form), {@code null}s dropped. {@code exclude} keys are left out.
     */
    protected static Map<String, Object> query(Params<?> params, String... exclude) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (params == null) return out;
        for (Map.Entry<String, Object> entry : params.toMap().entrySet()) {
            Object value = entry.getValue();
            if (value == null) continue;
            if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean
                    || value instanceof Enum<?> || value instanceof Collection<?>) {
                out.put(entry.getKey(), value instanceof Enum<?> e ? enumValue(e) : value);
            }
        }
        for (String key : exclude) out.remove(key);
        return out;
    }

    /** A request body from a params object (a mutable copy, so callers can adjust it). */
    protected static LinkedHashMap<String, Object> body(Params<?> params) {
        return params == null ? new LinkedHashMap<>() : params.copy();
    }

    private static Object enumValue(Enum<?> e) {
        return Json.MAPPER.convertValue(e, String.class);
    }

    // ---- responses ---------------------------------------------------------------------------

    protected static <T> T as(JsonNode node, Class<T> type) {
        return node == null || node.isNull() ? null : Json.convert(node, type);
    }

    protected static <T> T as(JsonNode node, JavaType type) {
        return node == null || node.isNull() ? null : Json.convert(node, type);
    }

    /**
     * {@code node[field]} bound to {@code type}; most single-resource responses wrap the object
     * ({@code {"source": {...}}}). Falls back to the whole node when the field is absent, so an
     * endpoint that starts returning the bare object keeps working.
     */
    protected static <T> T unwrap(JsonNode node, String field, Class<T> type) {
        if (node == null) return null;
        JsonNode inner = node.get(field);
        return as(inner != null ? inner : node, type);
    }

    /** {@code node[field]} as a list of {@code type}; empty when absent. */
    protected static <T> List<T> list(JsonNode node, String field, Class<T> type) {
        JsonNode array = node == null ? null : (field == null ? node : node.get(field));
        List<T> out = new ArrayList<>();
        if (array == null || !array.isArray()) return out;
        for (JsonNode item : array) out.add(as(item, type));
        return out;
    }

    protected static JsonNode orNull(JsonNode node) {
        return node == null ? NullNode.getInstance() : node;
    }

    /**
     * A page-numbered list response, {@code {"<dataField>": [...], "pagination": {"total", "page",
     * "pageSize"}}}, in the common {@link Page} shape. Missing pagination falls back to the page
     * and page size that were asked for.
     */
    protected static <T> Page<T> numberedPage(JsonNode node, String dataField, Class<T> type, int page, int pageSize) {
        List<T> data = list(node, dataField, type);
        JsonNode p = node == null ? null : node.get("pagination");
        int total = p != null && p.has("total") ? p.get("total").asInt() : data.size();
        int currentPage = p != null && p.has("page") ? p.get("page").asInt() : page;
        int size = p != null && p.has("pageSize") ? p.get("pageSize").asInt() : pageSize;
        boolean hasMore = currentPage * (long) size < total;
        return new Page<>(data, total, size, Math.max(0, (currentPage - 1) * size), hasMore, null);
    }

    /**
     * Fetch one page of a cursor list ({@code {"data": [...], "pagination": {"hasMore",
     * "nextCursor"}}}). {@code offset} is dropped from the query: no cursor list reads it, so sending
     * it only made a request for "page two" quietly return page one again.
     */
    protected final <T> Page<T> cursorPage(
            String path, Map<String, Object> query, RequestOptions options, Class<T> type, UnaryOperator<T> map) {
        Map<String, Object> q = new LinkedHashMap<>(query);
        q.remove("offset");
        JsonNode node = get(path, q, options);
        List<T> data = list(node, "data", type);
        if (map != null) data.replaceAll(map);
        JsonNode p = node == null ? null : node.get("pagination");
        String cursor = p != null && p.hasNonNull("nextCursor") ? p.get("nextCursor").asText() : null;
        boolean hasMore = p != null && p.path("hasMore").asBoolean(false) && cursor != null;
        int limit = q.get("limit") instanceof Number n ? n.intValue() : 50;
        return new Page<>(data, data.size(), limit, 0, hasMore, cursor);
    }
}
