package app.hookbase.models;

/**
 * A group of cron jobs.
 *
 * @param jobCount the number of jobs in the group (list responses)
 */
public record CronGroup(
        String id,
        String organizationId,
        String name,
        String slug,
        String description,
        boolean isCollapsed,
        int sortOrder,
        long jobCount,
        String createdAt,
        String updatedAt) {}
