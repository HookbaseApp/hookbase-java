package app.hookbase.models;

/**
 * An outbound message in the dead letter queue, as {@code dlq().list()} returns it.
 *
 * @param eventId the outbound event this message delivers
 * @param endpointUrl {@code null} when the endpoint has since been deleted
 * @param status {@code dlq} for a message still in the queue (see {@link OutboundMessage#status()})
 * @param dlqReason why the message was moved to the DLQ, e.g. {@code max_retries_exceeded}
 */
public record DlqMessage(
        String id,
        String eventId,
        String eventType,
        String applicationId,
        String applicationName,
        String endpointId,
        String endpointUrl,
        String status,
        int attempts,
        int maxAttempts,
        Integer lastResponseStatus,
        String lastErrorType,
        String lastErrorMessage,
        String dlqReason,
        String dlqMovedAt,
        Integer dlqRetryCount,
        String dlqLastRetryAt,
        String createdAt,
        String completedAt) {}
