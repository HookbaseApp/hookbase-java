package app.hookbase.models;

/** The endpoint a subscription delivers to, as nested in {@code subscriptions().get()}. */
public record SubscriptionEndpoint(String id, String url) {}
