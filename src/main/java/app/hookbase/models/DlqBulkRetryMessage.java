package app.hookbase.models;

/** One message queued by {@code dlq().retryBulk()}: the DLQ message and the new message carrying its retry. */
public record DlqBulkRetryMessage(String originalMessageId, String newMessageId) {}
