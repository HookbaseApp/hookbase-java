package app.hookbase.models;

/**
 * What {@code dlq().deleteBulk()} returns.
 *
 * @param deleted how many ids were submitted for deletion
 */
public record DlqBulkDeleteResult(int deleted) {}
