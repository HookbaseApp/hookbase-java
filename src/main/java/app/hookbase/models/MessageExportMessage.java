package app.hookbase.models;

/**
 * One outbound message in a {@link MessageExport} of type {@code messages}.
 *
 * @param endpointUrl {@code null} when the endpoint has since been deleted
 */
public record MessageExportMessage(
        String id,
        String eventId,
        String eventType,
        String applicationId,
        String endpointId,
        String endpointUrl,
        String status,
        int attempts,
        int maxAttempts,
        Integer lastResponseStatus,
        String lastErrorMessage,
        String completedAt,
        String createdAt) {}
