package app.hookbase.models;

/** The event type a subscription is for, as nested in {@code subscriptions().get()}. */
public record SubscriptionEventType(String id, String name, String displayName) {}
