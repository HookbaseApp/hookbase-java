package app.hookbase.params;

import java.util.Map;

/**
 * Body for {@code subscriptions().update()}. Only the fields you set are changed;
 * {@code filterExpression}, {@code labelFilters}, {@code labelFilterMode}, {@code transformId} and
 * {@code priorityOverride} can be set to {@code null} to clear them.
 */
public final class UpdateSubscriptionParams extends Params<UpdateSubscriptionParams> {
    /** JSONata expression a payload must satisfy to be delivered, max 1000 characters. */
    public UpdateSubscriptionParams filterExpression(String filterExpression) {
        return set("filterExpression", filterExpression);
    }

    /**
     * Label name (max 64) to a value or a list of up to 20 values (each max 256) that the event's
     * labels must match.
     */
    public UpdateSubscriptionParams labelFilters(Map<String, ?> labelFilters) {
        return set("labelFilters", labelFilters);
    }

    /** {@code all} (the default) or {@code any}: whether every label filter must match, or one. */
    public UpdateSubscriptionParams labelFilterMode(String labelFilterMode) {
        return set("labelFilterMode", labelFilterMode);
    }

    /** A transform to apply to the payload before delivery. */
    public UpdateSubscriptionParams transformId(String transformId) {
        return set("transformId", transformId);
    }

    public UpdateSubscriptionParams isEnabled(boolean isEnabled) {
        return set("isEnabled", isEnabled);
    }

    /** 0=Critical, 1=High, 2=Normal, 3=Low, 4=Bulk; overrides the event type's default priority. */
    public UpdateSubscriptionParams priorityOverride(Integer priorityOverride) {
        return set("priorityOverride", priorityOverride);
    }

    /** {@code latest} to follow the newest event-type version, or a version label to pin to. */
    public UpdateSubscriptionParams eventVersion(String eventVersion) {
        return set("eventVersion", eventVersion);
    }
}
