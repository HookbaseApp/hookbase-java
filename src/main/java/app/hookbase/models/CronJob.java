package app.hookbase.models;

/**
 * A scheduled HTTP request.
 *
 * @param headers the request headers as a JSON object string; credential values are masked for
 *     callers with read-only access
 * @param payload the request body as sent; {@code null} for none
 * @param notifyEmails comma-separated addresses notified on failure/success
 * @param consecutiveFailures failed runs in a row, reset by a success
 */
public record CronJob(
        String id,
        String organizationId,
        String groupId,
        String name,
        String description,
        String cronExpression,
        String timezone,
        String url,
        String method,
        String headers,
        String payload,
        int timeoutMs,
        boolean useStaticIp,
        boolean isActive,
        boolean notifyOnFailure,
        boolean notifyOnSuccess,
        String notifyEmails,
        int consecutiveFailures,
        String lastRunAt,
        String nextRunAt,
        String createdAt,
        String updatedAt) {}
