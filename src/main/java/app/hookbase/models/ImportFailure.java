package app.hookbase.models;

/** An imported item that failed, and why. */
public record ImportFailure(String name, String error) {}
