package app.hookbase.models;

/**
 * The event summary inside {@link EventDebugInfo}.
 *
 * @param signatureIssue {@code unsigned} when the sender attached no signature at all (as opposed
 *     to one that did not match), otherwise {@code null}
 */
public record EventDebugEvent(
        String id,
        String sourceId,
        String sourceName,
        String sourceSlug,
        String provider,
        String eventType,
        String payloadHash,
        String ipAddress,
        Boolean signatureValid,
        String receivedAt,
        String signatureIssue) {}
