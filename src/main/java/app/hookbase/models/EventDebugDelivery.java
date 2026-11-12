package app.hookbase.models;

import app.hookbase.internal.Redact;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

/**
 * One delivery of an event, as {@code events().debug()} shows it, with its retry, failure-cluster
 * and circuit details ({@link EventDebugInfo#deliveryList()}).
 *
 * @param status as {@link Delivery#status()}
 * @param statusCode the HTTP status of the last attempt; {@code null} before one completed
 * @param durationMs how long the last attempt took
 * @param attempt attempts made so far
 * @param nextRetryAt when the next attempt is scheduled; {@code null} when none is, and always for
 *     a {@code held} delivery
 * @param holdExpiresAt for a {@code held} delivery, when the hold ends (the delivery fails if the
 *     route has not recovered by then); {@code null} for every other status
 * @param error the last attempt's error message
 * @param responseHeaders the last response's headers; empty when there were none
 * @param transformedPayload the payload after the route's transform; {@code null} without one
 * @param destination the destination's name, URL and method
 * @param startedAt when the delivery was created
 * @param completedAt when the delivery last changed
 * @param failoverIndex 0 for the primary destination, 1 and up for failover destinations
 * @param parentDeliveryId the delivery this one failed over (or was replayed) from
 * @param fingerprint the failure fingerprint deliveries with the same failure share
 * @param retryAfter the {@code Retry-After} header of the last response, if it had one
 * @param clusterCount how many deliveries share this one's failure fingerprint; {@code null}
 *     without one
 * @param patternHints known failure patterns the last response matches, most specific first;
 *     empty for a delivery that did not fail
 * @param routeCircuit the route's circuit breaker; {@code null} when the route no longer exists
 */
public record EventDebugDelivery(
        String id,
        String status,
        Integer statusCode,
        Long durationMs,
        int attempt,
        int maxAttempts,
        String nextRetryAt,
        String holdExpiresAt,
        String error,
        String responseBody,
        Map<String, String> responseHeaders,
        JsonNode transformedPayload,
        String routeId,
        String destinationId,
        EventDebugDestination destination,
        String startedAt,
        String completedAt,
        int failoverIndex,
        String parentDeliveryId,
        String fingerprint,
        String retryAfter,
        Integer clusterCount,
        List<EventDebugPatternHint> patternHints,
        EventDebugRouteCircuit routeCircuit) {

    public EventDebugDelivery {
        responseHeaders = responseHeaders == null ? Map.of() : responseHeaders;
        patternHints = patternHints == null ? List.of() : List.copyOf(patternHints);
    }

    /** Credentials are printed as {@code <redacted>}, so logging this record is safe. */
    @Override
    public String toString() {
        return Redact.toString(this, Map.of("responseHeaders", Redact.Rule.HEADERS));
    }
}
