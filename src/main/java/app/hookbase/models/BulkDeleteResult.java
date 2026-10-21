package app.hookbase.models;

/** Result of a {@code bulkDelete} call: how many of the requested ids were deleted. */
public record BulkDeleteResult(boolean success, int deleted) {}
