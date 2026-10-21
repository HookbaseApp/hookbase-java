package app.hookbase.params;

import java.util.List;
import java.util.Map;

/**
 * Body for {@code routes().update()}. Only the fields you set are changed; set {@code filterId},
 * {@code filterConditions}, {@code filterLogic}, {@code transformId}, {@code schemaId},
 * {@code notifyEmails}, {@code failureThreshold}, {@code failoverDestinationIds},
 * {@code failoverAfterAttempts} or {@code expectedResponse} to {@code null} to clear it.
 */
public final class UpdateRouteParams extends Params<UpdateRouteParams> {
    public UpdateRouteParams name(String name) {
        return set("name", name);
    }

    public UpdateRouteParams sourceId(String sourceId) {
        return set("sourceId", sourceId);
    }

    public UpdateRouteParams destinationId(String destinationId) {
        return set("destinationId", destinationId);
    }

    /** A saved filter. Mutually exclusive with {@code filterConditions}. */
    public UpdateRouteParams filterId(String filterId) {
        return set("filterId", filterId);
    }

    /** Inline filter conditions, each {@code {"field", "operator", "value"}}. Mutually exclusive with {@code filterId}. */
    public UpdateRouteParams filterConditions(List<? extends Map<String, ?>> filterConditions) {
        return set("filterConditions", filterConditions);
    }

    /** How inline conditions combine ({@code and} / {@code or}). */
    public UpdateRouteParams filterLogic(String filterLogic) {
        return set("filterLogic", filterLogic);
    }

    public UpdateRouteParams transformId(String transformId) {
        return set("transformId", transformId);
    }

    /** A schema events must validate against. */
    public UpdateRouteParams schemaId(String schemaId) {
        return set("schemaId", schemaId);
    }

    /** Lower runs first. */
    public UpdateRouteParams priority(int priority) {
        return set("priority", priority);
    }

    public UpdateRouteParams isActive(boolean isActive) {
        return set("isActive", isActive);
    }

    public UpdateRouteParams notifyOnFailure(boolean notifyOnFailure) {
        return set("notifyOnFailure", notifyOnFailure);
    }

    public UpdateRouteParams notifyOnSuccess(boolean notifyOnSuccess) {
        return set("notifyOnSuccess", notifyOnSuccess);
    }

    public UpdateRouteParams notifyOnRecovery(boolean notifyOnRecovery) {
        return set("notifyOnRecovery", notifyOnRecovery);
    }

    /** Comma-separated addresses to notify. */
    public UpdateRouteParams notifyEmails(String notifyEmails) {
        return set("notifyEmails", notifyEmails);
    }

    /** Failures before a failure notification is sent. */
    public UpdateRouteParams failureThreshold(Integer failureThreshold) {
        return set("failureThreshold", failureThreshold);
    }

    /** Destinations tried in order when the primary keeps failing. */
    public UpdateRouteParams failoverDestinationIds(List<String> failoverDestinationIds) {
        return set("failoverDestinationIds", failoverDestinationIds);
    }

    /** Attempts on the primary before failing over, 1-5. */
    public UpdateRouteParams failoverAfterAttempts(Integer failoverAfterAttempts) {
        return set("failoverAfterAttempts", failoverAfterAttempts);
    }

    public UpdateRouteParams expectedResponse(String expectedResponse) {
        return set("expectedResponse", expectedResponse);
    }
}
