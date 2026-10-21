package app.hookbase.models;

/**
 * One delivery in a bulk replay.
 *
 * @param originalId the delivery that was replayed
 * @param newId the new delivery's id; empty when skipped
 * @param status {@code queued} or {@code skipped}
 * @param reason why it was skipped
 */
public record DeliveryBulkReplayItem(String originalId, String newId, String status, String reason) {}
