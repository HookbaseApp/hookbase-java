package app.hookbase.models;

import app.hookbase.internal.JsonTextDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

/**
 * One event with its payload and deliveries ({@code events().get()}).
 *
 * @param headers the request headers the webhook arrived with
 * @param payload the stored payload; {@code null} once it has expired or for a transient event
 * @param isTransient the source does not store payloads, so none can be shown or replayed
 *     (JSON {@code transient})
 * @param status as on {@link Event#status()}
 */
public record EventDetail(
        String id,
        String organizationId,
        String sourceId,
        String eventType,
        String payloadHash,
        String payloadKey,
        @JsonDeserialize(using = JsonTextDeserializer.class) JsonNode headers,
        String ipAddress,
        Boolean signatureValid,
        String receivedAt,
        String sourceName,
        String status,
        String method,
        JsonNode encryptFields,
        JsonNode maskFields,
        JsonNode payload,
        @JsonProperty("transient") boolean isTransient,
        List<Delivery> deliveries) {}
