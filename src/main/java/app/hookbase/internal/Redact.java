package app.hookbase.internal;

import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * {@code toString()} for records that carry credentials, so logging a model never writes a secret.
 * Internal: not part of the public API.
 */
public final class Redact {

    /** How a component is printed. */
    public enum Rule {
        /** {@code <redacted>} when set. */
        SECRET,
        /** A header map (or JSON object): values of credential-like header names are redacted. */
        HEADERS,
        /** A map (or JSON object) printed as its keys only. */
        KEYS
    }

    public static final String REDACTED = "<redacted>";

    private static final Pattern SENSITIVE_HEADER = Pattern.compile(
            "authorization|cookie|token|secret|key|signature|password|auth|credential|session",
            Pattern.CASE_INSENSITIVE);

    private Redact() {}

    /** Whether a header with this name may carry a credential. */
    public static boolean isSensitiveHeader(String name) {
        return name != null && SENSITIVE_HEADER.matcher(name).find();
    }

    /** The record's usual {@code Name[a=1, b=2]} form, with the named components masked. */
    public static String toString(Record record, Map<String, Rule> rules) {
        StringBuilder sb = new StringBuilder(record.getClass().getSimpleName()).append('[');
        RecordComponent[] components = record.getClass().getRecordComponents();
        for (int i = 0; i < components.length; i++) {
            RecordComponent c = components[i];
            Object value;
            try {
                value = c.getAccessor().invoke(record);
            } catch (ReflectiveOperationException e) {
                value = "?";
            }
            if (i > 0) sb.append(", ");
            sb.append(c.getName()).append('=').append(render(value, rules.get(c.getName())));
        }
        return sb.append(']').toString();
    }

    private static Object render(Object value, Rule rule) {
        if (value == null || rule == null) return value;
        return switch (rule) {
            case SECRET -> REDACTED;
            case HEADERS -> {
                Map<String, Object> out = new LinkedHashMap<>();
                entries(value).forEach((k, v) -> out.put(k, isSensitiveHeader(k) ? REDACTED : v));
                yield out;
            }
            case KEYS -> {
                List<String> keys = new ArrayList<>(entries(value).keySet());
                Map<String, Object> out = new LinkedHashMap<>();
                for (String k : keys) out.put(k, REDACTED);
                yield out;
            }
        };
    }

    private static Map<String, Object> entries(Object value) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            map.forEach((k, v) -> out.put(String.valueOf(k), v));
        } else if (value instanceof JsonNode node && node.isObject()) {
            for (Iterator<Map.Entry<String, JsonNode>> it = node.properties().iterator(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> e = it.next();
                out.put(e.getKey(), e.getValue().isTextual() ? e.getValue().asText() : e.getValue());
            }
        } else {
            // Not a map (a JSON string or array): nothing safe to show.
            out.put("value", REDACTED);
        }
        return out;
    }
}
