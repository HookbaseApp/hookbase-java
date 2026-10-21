package app.hookbase.params;

/** Filters for {@code subscriptions().list()}. The application comes from the call. */
public final class ListSubscriptionsParams extends Params<ListSubscriptionsParams> {
    /** Page size (default 50 on {@code list}, 100 on {@code listAll}). */
    public ListSubscriptionsParams limit(int limit) {
        return set("limit", limit);
    }

    /** The {@code cursor} from the previous page. */
    public ListSubscriptionsParams cursor(String cursor) {
        return set("cursor", cursor);
    }

    public ListSubscriptionsParams endpointId(String endpointId) {
        return set("endpointId", endpointId);
    }

    public ListSubscriptionsParams eventTypeId(String eventTypeId) {
        return set("eventTypeId", eventTypeId);
    }

    public ListSubscriptionsParams isEnabled(boolean isEnabled) {
        return set("isEnabled", isEnabled);
    }
}
