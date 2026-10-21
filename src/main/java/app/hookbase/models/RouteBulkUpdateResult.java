package app.hookbase.models;

/** Result of {@code routes().bulkUpdate()}: how many routes were changed. */
public record RouteBulkUpdateResult(boolean success, int updated) {}
