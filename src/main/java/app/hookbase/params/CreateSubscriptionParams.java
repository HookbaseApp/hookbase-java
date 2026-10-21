package app.hookbase.params;

import java.util.Map;

/** Body for {@code subscriptions().create()}. {@code endpointId} and {@code eventTypeId} are required. */
public final class CreateSubscriptionParams extends Params<CreateSubscriptionParams> {
    public CreateSubscriptionParams endpointId(String endpointId) {
        return set("endpointId", endpointId);
    }

    public CreateSubscriptionParams eventTypeId(String eventTypeId) {
        return set("eventTypeId", eventTypeId);
    }

    /** JSONata expression a payload must satisfy to be delivered, max 1000 characters. */
    public CreateSubscriptionParams filterExpression(String filterExpression) {
        return set("filterExpression", filterExpression);
    }

    /**
     * Label name (max 64) to a value or a list of up to 20 values (each max 256) that the event's
     * labels must match.
     */
    public CreateSubscriptionParams labelFilters(Map<String, ?> labelFilters) {
        return set("labelFilters", labelFilters);
    }

    /** {@code all} (the default) or {@code any}: whether every label filter must match, or one. */
    public CreateSubscriptionParams labelFilterMode(String labelFilterMode) {
        return set("labelFilterMode", labelFilterMode);
    }

    /** A transform to apply to the payload before delivery. */
    public CreateSubscriptionParams transformId(String transformId) {
        return set("transformId", transformId);
    }

    public CreateSubscriptionParams isEnabled(boolean isEnabled) {
        return set("isEnabled", isEnabled);
    }

    /** 0=Critical, 1=High, 2=Normal, 3=Low, 4=Bulk; overrides the event type's default priority. */
    public CreateSubscriptionParams priorityOverride(Integer priorityOverride) {
        return set("priorityOverride", priorityOverride);
    }

    /** {@code latest} to follow the newest event-type version, or a version label to pin to. */
    public CreateSubscriptionParams eventVersion(String eventVersion) {
        return set("eventVersion", eventVersion);
    }
}
