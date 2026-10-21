package app.hookbase.models;

/**
 * What replaying an outbound message ({@code messages().retry()}/{@code resend()}) or retrying a
 * DLQ message ({@code dlq().retry()}) returns.
 *
 * @param newMessageId the id of the NEW outbound message carrying the replay
 * @param status {@code queued} today
 */
public record MessageReplayResult(String originalMessageId, String newMessageId, String status) {}
