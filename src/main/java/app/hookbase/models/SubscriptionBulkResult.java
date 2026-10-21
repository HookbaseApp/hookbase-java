package app.hookbase.models;

import java.util.List;

/**
 * What {@code subscriptions().bulkSubscribe()} returns.
 *
 * @param created how many subscriptions were created
 * @param skipped how many event types the endpoint was already subscribed to
 * @param subscriptions the subscriptions created; empty when every event type was already subscribed
 */
public record SubscriptionBulkResult(int created, int skipped, List<Subscription> subscriptions) {
    public SubscriptionBulkResult {
        subscriptions = subscriptions == null ? List.of() : List.copyOf(subscriptions);
    }
}
