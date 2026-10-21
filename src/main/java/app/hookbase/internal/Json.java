package app.hookbase.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import app.hookbase.errors.HookbaseException;
import com.fasterxml.jackson.core.JacksonException;
import java.io.IOException;
import java.io.UncheckedIOException;

/** The SDK's one ObjectMapper. Internal: not part of the public API. */
public final class Json {

    /**
     * Unknown response fields are ignored, so a field the API adds later never breaks a caller.
     * Request bodies are built as maps by {@link app.hookbase.params.Params}, which keep an explicit
     * {@code null} (meaning "clear this field") and leave unset fields out, so no global inclusion
     * rule is applied to them here.
     */
    public static final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .defaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.ALWAYS, JsonInclude.Include.ALWAYS))
            .build();

    private Json() {}

    public static TypeFactory types() {
        return MAPPER.getTypeFactory();
    }

    public static JavaType type(Class<?> raw) {
        return MAPPER.constructType(raw);
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static JsonNode read(String text) throws IOException {
        return MAPPER.readTree(text);
    }

    /** Bind a response tree to a model; a shape mismatch is reported as a HookbaseException. */
    public static <T> T convert(JsonNode node, JavaType type) {
        try {
            return MAPPER.treeToValue(node, type);
        } catch (JacksonException e) {
            // Jackson's message quotes the offending value, which can be a secret (an API key, a
            // signing secret), so report only the type and where in the response it failed.
            String where = e instanceof JsonMappingException m ? " at " + m.getPathReference() : "";
            throw new HookbaseException("Unexpected response shape for " + type.getTypeName() + where);
        }
    }

    public static <T> T convert(JsonNode node, Class<T> type) {
        return convert(node, type(type));
    }
}
