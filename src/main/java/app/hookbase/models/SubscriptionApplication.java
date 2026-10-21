package app.hookbase.models;

/** The application a subscription's endpoint belongs to, as nested in {@code subscriptions().get()}. */
public record SubscriptionApplication(String id, String name) {}
