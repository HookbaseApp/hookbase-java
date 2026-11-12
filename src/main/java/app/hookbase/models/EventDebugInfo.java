package app.hookbase.models;

import app.hookbase.internal.Json;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * Everything needed to debug one event ({@code events().debug()}).
 *
 * @param payload the parsed payload, {@code null} when expired or transient
 * @param isTransient the payload was never stored (JSON {@code transient})
 * @param rawPayload the payload as received, cut at 10,000 characters
 * @param deliveries each delivery with retry, failure-cluster and circuit details, as the API sent
 *     them; {@link #deliveryList()} has them typed
 * @param timing ingest-to-delivery timing breakdown (free-form)
 * @param filterEvaluations how each route's filter evaluated against the payload (free-form)
 * @param recentConfigChanges audit-log entries for the source/routes/destinations involved
 * @param debug holds {@code curlCommand}; read it with {@link #curlCommand()}
 */
public record EventDebugInfo(
        EventDebugEvent event,
        EventDebugRequest request,
        JsonNode payload,
        @JsonProperty("transient") boolean isTransient,
        String rawPayload,
        JsonNode deliveries,
        JsonNode timing,
        JsonNode filterEvaluations,
        JsonNode recentConfigChanges,
        EventDebugCommands debug) {

    /**
     * {@link #deliveries()} as typed records. Bound on each call, so keep the result rather than
     * calling this in a loop.
     *
     * @return the deliveries; empty when there are none
     * @throws app.hookbase.errors.HookbaseException when the API sent deliveries of a shape this
     *     SDK cannot read
     */
    public List<EventDebugDelivery> deliveryList() {
        if (deliveries == null || !deliveries.isArray()) return List.of();
        List<EventDebugDelivery> list = Json.convert(
                deliveries, Json.types().constructCollectionType(List.class, EventDebugDelivery.class));
        return List.copyOf(list);
    }

    /** A curl command that re-sends the event to its ingest URL; {@code null} for a transient event. */
    public String curlCommand() {
        return debug == null ? null : debug.curlCommand();
    }
}
