package app.hookbase.models;

/** Ready-to-run commands for an event. {@code curlCommand} is {@code null} for a transient event. */
public record EventDebugCommands(String curlCommand) {}
