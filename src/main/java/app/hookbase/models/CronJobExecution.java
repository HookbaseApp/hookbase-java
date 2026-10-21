package app.hookbase.models;

/**
 * The result of running a cron job on demand.
 *
 * @param status {@code success} or {@code failed}
 * @param responseStatus the target's HTTP status; {@code null} when no response came back
 */
public record CronJobExecution(String id, String status, Integer responseStatus, long latencyMs) {}
