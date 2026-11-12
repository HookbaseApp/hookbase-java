package app.hookbase.models;

/**
 * The result of {@code deliveries().replay()}: a new delivery was queued.
 *
 * @param deliveryId the new delivery's id
 * @param modified the payload was replaced ({@code modifiedPayload}) or patched
 *     ({@code payloadPatch})
 * @param destinationOverridden the replay goes to a {@code destinationOverride}
 * @param transformOverridden the replay runs a {@code transformOverride}
 * @param transformPersisted the override was also saved as the route's transform
 * @param headersOverridden the replay carries a {@code headersOverride}
 * @param targetDestinationId the destination the replay goes to
 */
public record DeliveryReplayResult(
        String deliveryId,
        String message,
        boolean modified,
        boolean destinationOverridden,
        boolean transformOverridden,
        boolean transformPersisted,
        boolean headersOverridden,
        String targetDestinationId) {}
