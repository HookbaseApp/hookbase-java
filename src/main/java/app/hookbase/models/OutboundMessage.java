package app.hookbase.models;

import app.hookbase.internal.JsonTextDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * One delivery of an event to one endpoint, as {@code messages().list()} and {@code messages().get()}
 * return it. The fields marked "get only" are on the single-message response and {@code null} on
 * list items.
 *
 * @param eventId the outbound event this message delivers (what {@code send} returned as {@code eventId})
 * @param endpointUrl {@code null} when the endpoint has since been deleted
 * @param status {@code pending}, {@code processing}, {@code success}, {@code failed},
 *     {@code awaiting_retry}, {@code exhausted} or {@code dlq}; open-ended, so a status the API adds
 *     later still parses
 * @param subscriptionId get only
 * @param eventTypeId get only
 * @param lastErrorType get only
 * @param payloadKey get only
 * @param payloadSizeBytes get only
 * @param payloadHash get only
 * @param metadata get only; decoded when the API sends it as JSON text
 * @param priority get only; 0=Critical .. 4=Bulk
 * @param endpointDescription get only
 * @param payload get only: the stored payload, or {@code null} once retention has removed it
 */
public record OutboundMessage(
        String id,
        String eventId,
        String eventType,
        String applicationId,
        String applicationName,
        String endpointId,
        String endpointUrl,
        String status,
        int attempts,
        int maxAttempts,
        Integer lastResponseStatus,
        String lastErrorMessage,
        String createdAt,
        String completedAt,
        String subscriptionId,
        String eventTypeId,
        String lastErrorType,
        String payloadKey,
        Long payloadSizeBytes,
        String payloadHash,
        @JsonDeserialize(using = JsonTextDeserializer.class) JsonNode metadata,
        Integer priority,
        String endpointDescription,
        JsonNode payload) {}
