package app.hookbase.models;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * An outbound application: one of your customers or tenants, grouping their endpoints.
 *
 * @param externalId your own identifier for the customer (the other SDKs also mirror it as {@code uid})
 * @param rateLimitPerSecond {@code null} when unset; {@code null} and 0 are different answers
 * @param sendEventUrl set on create and get-or-create responses only
 */
public record Application(
        String id,
        String name,
        String organizationId,
        String externalId,
        JsonNode metadata,
        Integer rateLimitPerSecond,
        Integer rateLimitPerMinute,
        Integer rateLimitPerHour,
        boolean isDisabled,
        String disabledAt,
        String disabledReason,
        long totalEndpoints,
        long totalMessagesSent,
        long totalMessagesFailed,
        String lastEventAt,
        String createdBy,
        String apiKeyId,
        String createdAt,
        String updatedAt,
        String sendEventUrl) {}
