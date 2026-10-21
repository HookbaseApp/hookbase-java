package app.hookbase.models;

/** An endpoint an event was queued for, as listed in a {@link SendMessageResponse}. */
public record SendMessageEndpoint(String id, String url) {}
