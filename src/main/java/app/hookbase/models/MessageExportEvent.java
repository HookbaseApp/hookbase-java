package app.hookbase.models;

/** One outbound event in a {@link MessageExport} of type {@code events}. */
public record MessageExportEvent(
        String id,
        String eventType,
        String applicationId,
        String labels,
        String status,
        Integer endpointsTargeted,
        Integer endpointsFiltered,
        Long payloadSizeBytes,
        String idempotencyKey,
        String sourceIp,
        String createdAt) {}
