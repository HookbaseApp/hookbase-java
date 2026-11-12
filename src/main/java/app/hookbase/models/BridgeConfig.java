package app.hookbase.models;

import app.hookbase.internal.Json;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;

/**
 * A {@code bridge} destination's configuration: each delivery that reaches the destination is
 * published as an outbound event, sent to the endpoints subscribed to its event type. Build one with
 * {@link #fixed} or {@link #field} and pass {@link #toMap()} to {@code config(...)} on the create or
 * update params:
 *
 * <pre>{@code
 * hookbase.destinations().create(new CreateDestinationParams()
 *         .name("Orders to customers").type("bridge")
 *         .config(BridgeConfig.field("type").withPrefix("shop.").toMap()));
 * }</pre>
 *
 * <p>Unset components are left out of the request. The API returns the config as it was stored, in
 * {@link Destination#config()}.
 *
 * @param eventTypeSource {@code fixed} (every event gets {@code eventType}) or {@code field} (the
 *     event type is read from {@code eventTypeField} in the payload)
 * @param eventType the event type to publish as, for {@code fixed}; it must exist and be enabled
 * @param eventTypeField a dot path into the payload ({@code data.type}, at most 5 segments), for
 *     {@code field}
 * @param eventTypePrefix prepended to the value read from {@code eventTypeField} ({@code field}
 *     only): lowercase letters, digits and dots, starting with a letter, at most 50 characters
 * @param applicationId publish to this application's endpoints only; {@code null} for every
 *     subscribed endpoint
 * @param allowUnverifiedSources also publish events whose sender the source did not check. A
 *     sender counts as checked by a valid signature made with the source's own secret, or, on a
 *     custom source not made from a provider-catalog entry, an IP allowlist of the sender's own
 *     addresses. Off by default, so an event anyone could have posted to the ingest URL is not
 *     passed on to your customers
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BridgeConfig(
        String eventTypeSource,
        String eventType,
        String eventTypeField,
        String eventTypePrefix,
        String applicationId,
        Boolean allowUnverifiedSources) {

    /** Publish every event as {@code eventType}. */
    public static BridgeConfig fixed(String eventType) {
        return new BridgeConfig("fixed", eventType, null, null, null, null);
    }

    /** Publish each event as the value of the payload field {@code eventTypeField} (a dot path, e.g. {@code data.type}). */
    public static BridgeConfig field(String eventTypeField) {
        return new BridgeConfig("field", null, eventTypeField, null, null, null);
    }

    /** This config with {@code eventTypePrefix} set ({@code field} configs only). */
    public BridgeConfig withPrefix(String eventTypePrefix) {
        return new BridgeConfig(eventTypeSource, eventType, eventTypeField, eventTypePrefix, applicationId, allowUnverifiedSources);
    }

    /** This config, publishing to one application's endpoints only. */
    public BridgeConfig withApplicationId(String applicationId) {
        return new BridgeConfig(eventTypeSource, eventType, eventTypeField, eventTypePrefix, applicationId, allowUnverifiedSources);
    }

    /** This config with {@code allowUnverifiedSources} set. */
    public BridgeConfig withAllowUnverifiedSources(boolean allowUnverifiedSources) {
        return new BridgeConfig(eventTypeSource, eventType, eventTypeField, eventTypePrefix, applicationId, allowUnverifiedSources);
    }

    /** The config as the map {@code config(...)} takes, with unset components left out. */
    public Map<String, Object> toMap() {
        return Json.MAPPER.convertValue(this, new TypeReference<Map<String, Object>>() {});
    }
}
