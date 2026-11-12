package app.hookbase.models;

/**
 * Where a message came from when a {@code bridge} destination published its event: the inbound
 * event and the delivery that reached the bridge ({@link OutboundMessage#bridge()}).
 *
 * @param inboundEventId the inbound event the outbound event was published from
 * @param deliveryId the inbound delivery to the bridge destination; {@code null} when not recorded
 * @param hop how many bridges the event has passed through; {@code null} when not recorded
 */
public record OutboundMessageBridge(String inboundEventId, String deliveryId, Integer hop) {}
