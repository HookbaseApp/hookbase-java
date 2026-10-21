package app.hookbase.models;

/** DLQ messages for one event type, in {@link DlqStats#byEventType()}. */
public record DlqEventTypeCount(String eventType, long count) {}
