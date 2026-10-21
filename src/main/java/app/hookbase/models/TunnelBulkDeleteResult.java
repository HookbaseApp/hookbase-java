package app.hookbase.models;

/** The result of {@code tunnels().bulkDelete()}. */
public record TunnelBulkDeleteResult(boolean success, int deleted) {}
