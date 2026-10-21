package app.hookbase.params;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class of every request-parameter object.
 *
 * <p>Values live in an insertion-ordered map rather than in fields, which gives each key three
 * states, the same three the other SDKs distinguish:
 *
 * <ul>
 *   <li><b>not set</b>: the key is left out of the request, so an update leaves it unchanged;
 *   <li><b>set to a value</b>: sent as that value;
 *   <li><b>set to {@code null}</b>: sent as JSON {@code null}, which on an update clears the field
 *       (Node passes {@code null}, Go and .NET take a {@code Clear} list, Python a {@code clear=}
 *       argument).
 * </ul>
 *
 * <p>{@link #extra(String, Object)} sends a field this SDK version does not model yet.
 *
 * @param <SELF> the concrete subclass, so the fluent setters return it
 */
public abstract class Params<SELF extends Params<SELF>> {

    private final LinkedHashMap<String, Object> values = new LinkedHashMap<>();

    protected Params() {}

    @SuppressWarnings("unchecked")
    protected final SELF self() {
        return (SELF) this;
    }

    /** Set {@code key}; a {@code null} value is sent as JSON null. */
    protected final SELF set(String key, Object value) {
        values.put(key, value);
        return self();
    }

    /**
     * Send a field this SDK version does not have a setter for. The key is the API's camelCase field
     * name; the value is anything Jackson can serialize.
     */
    public final SELF extra(String key, Object value) {
        return set(key, value);
    }

    /** Forget a key set earlier, so it is left out of the request again. */
    public final SELF unset(String key) {
        values.remove(key);
        return self();
    }

    /** Whether {@code key} has been set (to a value or to {@code null}). */
    public final boolean has(String key) {
        return values.containsKey(key);
    }

    /** The value set for {@code key}, or {@code null} when unset or set to null. */
    public final Object get(String key) {
        return values.get(key);
    }

    /** A read-only view of the keys set so far, in the order they were set. */
    public final Map<String, Object> toMap() {
        return Collections.unmodifiableMap(values);
    }

    /** A mutable copy of the keys set so far, for building a request body. */
    public final LinkedHashMap<String, Object> copy() {
        return new LinkedHashMap<>(values);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + values.keySet();
    }
}
