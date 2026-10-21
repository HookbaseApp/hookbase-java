package app.hookbase.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Everything needed to debug one event ({@code events().debug()}).
 *
 * @param payload the parsed payload, {@code null} when expired or transient
 * @param isTransient the payload was never stored (JSON {@code transient})
 * @param rawPayload the payload as received, cut at 10,000 characters
 * @param deliveries each delivery with retry, failure-cluster and circuit details (free-form)
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

    /** A curl command that re-sends the event to its ingest URL; {@code null} for a transient event. */
    public String curlCommand() {
        return debug == null ? null : debug.curlCommand();
    }
}
