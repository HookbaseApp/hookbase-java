package app.hookbase.resources;

import app.hookbase.Page;
import app.hookbase.Pager;
import app.hookbase.RequestOptions;
import app.hookbase.internal.HttpTransport;
import app.hookbase.models.Subscription;
import app.hookbase.models.SubscriptionBulkResult;
import app.hookbase.params.CreateSubscriptionParams;
import app.hookbase.params.ListSubscriptionsParams;
import app.hookbase.params.UpdateSubscriptionParams;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Subscriptions: which event types each endpoint receives.
 *
 * <p>Methods take the {@code applicationId} first, as in the other Hookbase SDKs. Only {@code list},
 * {@code listAll} and {@code unsubscribeAll} send it; the API finds a subscription by its id alone.
 */
public final class SubscriptionsResource extends BaseResource {

    private static final String PATH = "/api/webhook-subscriptions";

    public SubscriptionsResource(HttpTransport http) {
        super(http);
    }

    /** One page of an application's subscriptions. Pass the returned {@link Page#cursor()} back as {@code cursor} for the next. */
    public Page<Subscription> list(String applicationId, ListSubscriptionsParams params, RequestOptions options) {
        Map<String, Object> q = query(params);
        q.put("applicationId", applicationId);
        return cursorPage(PATH, q, options, Subscription.class, null);
    }

    public Page<Subscription> list(String applicationId, ListSubscriptionsParams params) {
        return list(applicationId, params, null);
    }

    public Page<Subscription> list(String applicationId) {
        return list(applicationId, null, null);
    }

    /** Every subscription of an application, fetched 100 at a time (or {@code limit}) as you iterate. */
    public Pager<Subscription> listAll(String applicationId, ListSubscriptionsParams params, RequestOptions options) {
        Map<String, Object> q = query(params);
        q.put("applicationId", applicationId);
        q.putIfAbsent("limit", 100);
        String start = (String) q.remove("cursor");
        return Pager.cursor(start, cursor -> {
            Map<String, Object> page = new LinkedHashMap<>(q);
            page.put("cursor", cursor);
            return cursorPage(PATH, page, options, Subscription.class, null);
        });
    }

    public Pager<Subscription> listAll(String applicationId, ListSubscriptionsParams params) {
        return listAll(applicationId, params, null);
    }

    public Pager<Subscription> listAll(String applicationId) {
        return listAll(applicationId, null, null);
    }

    public Subscription get(String applicationId, String subscriptionId, RequestOptions options) {
        return unwrap(get(PATH + "/" + seg(subscriptionId), (Map<String, ?>) null, options), "data", Subscription.class);
    }

    public Subscription get(String applicationId, String subscriptionId) {
        return get(applicationId, subscriptionId, null);
    }

    public Subscription create(String applicationId, CreateSubscriptionParams params, RequestOptions options) {
        return unwrap(post(PATH, body(params), options), "data", Subscription.class);
    }

    public Subscription create(String applicationId, CreateSubscriptionParams params) {
        return create(applicationId, params, null);
    }

    public Subscription update(
            String applicationId, String subscriptionId, UpdateSubscriptionParams params, RequestOptions options) {
        return unwrap(patch(PATH + "/" + seg(subscriptionId), body(params), options), "data", Subscription.class);
    }

    public Subscription update(String applicationId, String subscriptionId, UpdateSubscriptionParams params) {
        return update(applicationId, subscriptionId, params, null);
    }

    public void delete(String applicationId, String subscriptionId, RequestOptions options) {
        delete(PATH + "/" + seg(subscriptionId), (Object) null, options);
    }

    public void delete(String applicationId, String subscriptionId) {
        delete(applicationId, subscriptionId, null);
    }

    /** Enable the subscription (an update that sends only {@code isEnabled: true}). */
    public Subscription enable(String applicationId, String subscriptionId, RequestOptions options) {
        return update(applicationId, subscriptionId, new UpdateSubscriptionParams().isEnabled(true), options);
    }

    public Subscription enable(String applicationId, String subscriptionId) {
        return enable(applicationId, subscriptionId, null);
    }

    /** Disable the subscription (an update that sends only {@code isEnabled: false}). */
    public Subscription disable(String applicationId, String subscriptionId, RequestOptions options) {
        return update(applicationId, subscriptionId, new UpdateSubscriptionParams().isEnabled(false), options);
    }

    public Subscription disable(String applicationId, String subscriptionId) {
        return disable(applicationId, subscriptionId, null);
    }

    /**
     * Subscribe an endpoint to several event types at once (up to 100) and return the subscriptions
     * created. Event types it was already subscribed to are skipped, so the list can be shorter than
     * {@code eventTypeIds}, or empty.
     */
    public List<Subscription> subscribeToMany(
            String applicationId, String endpointId, List<String> eventTypeIds, RequestOptions options) {
        return bulkSubscribe(endpointId, eventTypeIds, options).subscriptions();
    }

    public List<Subscription> subscribeToMany(String applicationId, String endpointId, List<String> eventTypeIds) {
        return subscribeToMany(applicationId, endpointId, eventTypeIds, null);
    }

    /** Subscribe an endpoint to several event types at once (up to 100), with the created/skipped counts. */
    public SubscriptionBulkResult bulkSubscribe(String endpointId, List<String> eventTypeIds, RequestOptions options) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("endpointId", endpointId);
        body.put("eventTypeIds", eventTypeIds);
        // When every event type was already subscribed the API answers { created: 0, skipped } with
        // no `subscriptions` key; the record turns that into an empty list.
        return as(post(PATH + "/bulk", body, options), SubscriptionBulkResult.class);
    }

    public SubscriptionBulkResult bulkSubscribe(String endpointId, List<String> eventTypeIds) {
        return bulkSubscribe(endpointId, eventTypeIds, null);
    }

    /**
     * Delete every subscription the endpoint has, on every page: all of them are listed first, then
     * deleted one by one.
     */
    public void unsubscribeAll(String applicationId, String endpointId, RequestOptions options) {
        List<Subscription> subs = new ArrayList<>();
        for (Subscription sub : listAll(applicationId, new ListSubscriptionsParams().endpointId(endpointId), options)) {
            subs.add(sub);
        }
        for (Subscription sub : subs) {
            delete(applicationId, sub.id(), options);
        }
    }

    public void unsubscribeAll(String applicationId, String endpointId) {
        unsubscribeAll(applicationId, endpointId, null);
    }
}
