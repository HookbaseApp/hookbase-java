package app.hookbase.models;

/**
 * An inbound webhook event, as {@code events().list()} returns it (no payload; see
 * {@link EventDetail}).
 *
 * @param status computed from the event's deliveries: {@code delivered}, {@code failed},
 *     {@code pending}, {@code partial}, {@code duplicate} or {@code no_routes}
 * @param signatureValid whether the provider signature checked out; {@code null} when the source
 *     verifies none
 * @param method the HTTP verb the webhook arrived with
 */
public record Event(
        String id,
        String organizationId,
        String sourceId,
        String eventType,
        String payloadHash,
        Boolean signatureValid,
        String receivedAt,
        String ipAddress,
        String sourceName,
        String sourceSlug,
        String status,
        String method,
        EventDeliveryStats deliveryStats) {}
