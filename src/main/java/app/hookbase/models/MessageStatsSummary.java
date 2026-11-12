package app.hookbase.models;

/**
 * Outbound message counts by status, from {@code messages().getStatsSummary()}. {@code total}
 * counts every status, including those without a field here ({@code awaiting_pull},
 * {@code fifo_waiting}).
 *
 * @param awaitingRetry retries handed to the cron sweep between attempts
 * @param held messages waiting while their endpoint's circuit is open; released when it recovers
 */
public record MessageStatsSummary(
        long pending,
        long processing,
        long success,
        long failed,
        long awaitingRetry,
        long held,
        long exhausted,
        long dlq,
        long total) {

    /**
     * A summary with no held count: the constructor this record had before holds, kept so code that
     * builds one (a test stub, say) still compiles.
     */
    public MessageStatsSummary(
            long pending, long processing, long success, long failed, long awaitingRetry, long exhausted, long dlq, long total) {
        this(pending, processing, success, failed, awaitingRetry, 0, exhausted, dlq, total);
    }
}
