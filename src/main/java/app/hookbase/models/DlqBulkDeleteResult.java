package app.hookbase.models;

/**
 * What {@code dlq().deleteBulk()} returns.
 *
 * @param deleted how many of the ids were in the DLQ and were removed; unknown ids are not counted
 */
public record DlqBulkDeleteResult(int deleted) {}
