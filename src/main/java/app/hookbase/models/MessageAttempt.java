package app.hookbase.models;

import app.hookbase.internal.JsonTextDeserializer;
import app.hookbase.internal.Redact;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;

/**
 * One delivery attempt of an outbound message, as {@code messages().listAttempts()} returns it.
 *
 * @param messageId the outbound message this attempt belongs to
 * @param status the attempt's outcome, e.g. {@code success} or {@code failed}
 * @param responseTimeMs time to the endpoint's response, in milliseconds
 * @param triggeredBy what started the attempt, e.g. {@code system} or a manual replay
 */
public record MessageAttempt(
        String id,
        String messageId,
        int attemptNumber,
        String status,
        Integer responseStatus,
        @JsonDeserialize(using = JsonTextDeserializer.class) Map<String, String> responseHeaders,
        String responseBody,
        Integer responseTimeMs,
        Integer totalTimeMs,
        String errorType,
        String errorMessage,
        String errorCode,
        String requestUrl,
        @JsonDeserialize(using = JsonTextDeserializer.class) Map<String, String> requestHeaders,
        Integer requestBodySize,
        String triggeredBy,
        String triggeredByUser,
        String createdAt,
        String completedAt) {
    /** Credentials are printed as {@code <redacted>}, so logging this record is safe. */
    @Override
    public String toString() {
        return Redact.toString(this, Map.of("requestHeaders", Redact.Rule.HEADERS, "responseHeaders", Redact.Rule.HEADERS));
    }
}
