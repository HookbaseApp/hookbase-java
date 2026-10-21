package app.hookbase.models;

/**
 * The result of {@code deliveries().replay()}: a new delivery was queued.
 *
 * @param deliveryId the new delivery's id
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
