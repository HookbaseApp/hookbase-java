package app.hookbase.models;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A subscription: one endpoint receiving one event type.
 *
 * <p>List responses carry the joined names flat ({@code endpointUrl}, {@code eventTypeName},
 * {@code applicationId}, ...); the single-subscription {@code get} carries them nested instead
 * ({@code endpoint}, {@code eventType}, {@code application}). Whichever form a response does not
 * use is {@code null}.
 *
 * @param filterExpression JSONata expression a payload must satisfy to be delivered, or {@code null}
 * @param labelFilters label name to a value or array of values the event's labels must match
 * @param labelFilterMode {@code all} or {@code any}
 * @param priorityOverride 0=Critical .. 4=Bulk, or {@code null} to use the event type's priority
 * @param eventVersion {@code latest}, or the event-type version label this subscription is pinned to
 */
public record Subscription(
        String id,
        String endpointId,
        String eventTypeId,
        String filterExpression,
        JsonNode labelFilters,
        String labelFilterMode,
        String transformId,
        Integer priorityOverride,
        boolean isEnabled,
        String createdAt,
        String createdBy,
        String apiKeyId,
        String endpointUrl,
        String eventTypeName,
        String eventTypeDisplayName,
        String applicationId,
        String applicationName,
        String eventVersion,
        boolean eventVersionDeprecated,
        String eventVersionSunsetAt,
        SubscriptionEndpoint endpoint,
        SubscriptionEventType eventType,
        SubscriptionApplication application) {}
