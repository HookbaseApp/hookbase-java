package app.hookbase.models;

import app.hookbase.internal.JsonTextDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * One inbound delivery: an event sent to one destination by one route.
 *
 * @param status e.g. {@code pending}, {@code delivered}, {@code failed}, {@code retrying},
 *     {@code failed_over}, {@code schema_failed}. {@code pending} covers both not-yet-sent and
 *     failed-and-waiting-to-retry (then {@code nextRetryAt} is set)
 * @param responseStatus the destination's HTTP status; {@code null} when no response came back
 * @param responseHeaders the destination's response headers
 * @param latencyMs round-trip time of the last attempt
 * @param destinationUrl set by {@code deliveries().get()} and on event details, not on list
 * @param eventType set on list only
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
        String eventType) {}
