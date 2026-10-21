package app.hookbase.models;

/** One validation failure: where in the payload ({@code path}, empty for the root) and why. */
public record SchemaValidationError(String path, String message) {}
