package app.hookbase.models;

import app.hookbase.internal.JsonTextDeserializer;
import app.hookbase.internal.Redact;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;

/**
 * One inbound delivery: an event sent to one destination by one route.
 *
 * @param status {@code pending} (not sent yet), {@code delivered}, {@code failed},
 *     {@code failed_over} (failed, and handed to the route's failover destination),
 *     {@code schema_failed}, {@code throttled} (waiting on the destination's throttle),
 *     {@code awaiting_retry} (an attempt failed and the next is scheduled at {@code nextRetryAt})
 *     or {@code held} (waiting while the route's circuit is open; see {@code holdExpiresAt});
 *     open-ended, so a status the API adds later still parses
 * @param nextRetryAt when the next attempt is scheduled; {@code null} when none is, and always
 *     for a {@code held} delivery
 * @param responseStatus the destination's HTTP status; {@code null} when no response came back
 * @param responseHeaders the destination's response headers
 * @param latencyMs round-trip time of the last attempt
 * @param destinationUrl set by {@code deliveries().get()} and on event details, not on list
 * @param eventType set on list only
 * @param holdExpiresAt for a {@code held} delivery, when the hold ends: a delivery whose route has
 *     not recovered by then fails. {@code null} for every other status
 */
public record Delivery(
        String id,
        String organizationId,
        String eventId,
        String routeId,
        String destinationId,
        String status,
        int attemptCount,
        int maxAttempts,
        String nextRetryAt,
        Integer responseStatus,
        String responseKey,
        @JsonDeserialize(using = JsonTextDeserializer.class) JsonNode responseHeaders,
        String responseBody,
        Long latencyMs,
        String errorMessage,
        String deliveredAt,
        String createdAt,
        String updatedAt,
        String destinationName,
        String destinationUrl,
        String eventType,
        String holdExpiresAt) {
    /** Credentials are printed as {@code <redacted>}, so logging this record is safe. */
    @Override
    public String toString() {
        return Redact.toString(this, Map.of("responseHeaders", Redact.Rule.HEADERS));
    }
}
