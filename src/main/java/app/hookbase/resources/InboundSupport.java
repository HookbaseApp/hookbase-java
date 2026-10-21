package app.hookbase.resources;

import app.hookbase.params.ImportParams;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Request and response helpers shared by the sources, destinations and routes resources. */
final class InboundSupport {
    private InboundSupport() {}

    /** {@code node[field]} as text, or {@code null} when absent. */
    static String text(JsonNode node, String field) {
        return node == null || !node.hasNonNull(field) ? null : node.get(field).asText();
    }

    /** {@code ?ids=a,b} for an export, or no query (export everything) when there are no ids. */
    static Map<String, Object> exportQuery(List<String> ids) {
        Map<String, Object> q = new LinkedHashMap<>();
        if (ids != null && !ids.isEmpty()) q.put("ids", ids);
        return q;
    }

    /**
     * {@code {"<key>": items, "conflictStrategy", "validateOnly"}}. The API requires a conflict
     * strategy, so {@code skip} is sent when none was set (as the Python SDK does).
     */
    static Map<String, Object> importBody(String key, Iterable<?> items, ImportParams params) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(key, items);
        body.putAll(BaseResource.body(params));
        if (body.get("conflictStrategy") == null) body.put("conflictStrategy", "skip");
        return body;
    }

    /** {@code {"ids": [...]}} for a bulk call. */
    static Map<String, Object> ids(List<String> ids) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ids", ids);
        return body;
    }
}
