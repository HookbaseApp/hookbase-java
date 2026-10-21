package app.hookbase.internal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import java.io.IOException;

/**
 * Binds a field the API sends either as JSON or as a string holding JSON text. Several columns are
 * stored as {@code text} and passed through as-is on list/get but echoed as real JSON on create
 * (API key {@code scopes}, filter {@code conditions}, schema {@code jsonSchema}, event
 * {@code headers}, event type {@code schema}/{@code examplePayload}, outbound message
 * {@code metadata}, attempt request/response headers), so the same field arrives in both shapes.
 * The Go and Python SDKs decode the same fields the same way. A string that is not a JSON object or
 * array is bound as the string itself. Internal: not part of the public API.
 */
public final class JsonTextDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {

    private final JavaType type;

    public JsonTextDeserializer() {
        this(null);
    }

    private JsonTextDeserializer(JavaType type) {
        this.type = type;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        JavaType t = property != null ? property.getType() : ctxt.getContextualType();
        return new JsonTextDeserializer(t);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = ctxt.readTree(p);
        if (node.isTextual()) {
            String text = node.asText().trim();
            if (text.startsWith("{") || text.startsWith("[")) {
                try {
                    node = Json.MAPPER.readTree(text);
                } catch (IOException e) {
                    // Not JSON after all: bind the string itself.
                }
            }
        }
        if (type == null || JsonNode.class.isAssignableFrom(type.getRawClass())) return node;
        if (node.isNull()) return null;
        return ctxt.readTreeAsValue(node, type);
    }
}
