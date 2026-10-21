package app.hookbase.models;

import java.util.List;

/**
 * What {@code dlq().retryBulk()} returns.
 *
 * @param queued how many messages were queued for retry
 * @param skipped how many of the ids asked for were not in the DLQ (or not found) and were skipped
 * @param messages one entry per queued message
 */
public record DlqBulkRetryResult(int queued, int skipped, List<DlqBulkRetryMessage> messages) {
    public DlqBulkRetryResult {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }
}
