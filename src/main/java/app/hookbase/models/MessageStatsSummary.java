package app.hookbase.models;

/**
 * Outbound message counts by status, from {@code messages().getStatsSummary()}.
 *
 * @param awaitingRetry retries handed to the cron sweep between attempts
 */
public record MessageStatsSummary(
        long pending,
        long processing,
        long success,
        long failed,
        long awaitingRetry,
        long exhausted,
        long dlq,
        long total) {}
