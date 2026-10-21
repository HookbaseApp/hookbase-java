package app.hookbase.models;

/** Delivery counts for one event, by status. */
public record EventDeliveryStats(long total, long delivered, long failed, long pending) {}
