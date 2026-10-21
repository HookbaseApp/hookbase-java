package app.hookbase.params;

import java.util.List;
import java.util.Map;

/** Body for {@code routes().create()}. {@code name}, {@code sourceId} and {@code destinationId} are required. */
public final class CreateRouteParams extends Params<CreateRouteParams> {
    public CreateRouteParams name(String name) {
        return set("name", name);
    }

    public CreateRouteParams sourceId(String sourceId) {
        return set("sourceId", sourceId);
    }

    public CreateRouteParams destinationId(String destinationId) {
        return set("destinationId", destinationId);
    }

    /** A saved filter. Mutually exclusive with {@code filterConditions}. */
    public CreateRouteParams filterId(String filterId) {
        return set("filterId", filterId);
    }

    /** Inline filter conditions, each {@code {"field", "operator", "value"}}. Mutually exclusive with {@code filterId}. */
    public CreateRouteParams filterConditions(List<? extends Map<String, ?>> filterConditions) {
        return set("filterConditions", filterConditions);
    }

    /** How inline conditions combine ({@code and} / {@code or}). */
    public CreateRouteParams filterLogic(String filterLogic) {
        return set("filterLogic", filterLogic);
    }

    public CreateRouteParams transformId(String transformId) {
        return set("transformId", transformId);
    }

    /** A schema events must validate against. */
    public CreateRouteParams schemaId(String schemaId) {
        return set("schemaId", schemaId);
    }

    /** Lower runs first. */
    public CreateRouteParams priority(int priority) {
        return set("priority", priority);
    }

    public CreateRouteParams isActive(boolean isActive) {
        return set("isActive", isActive);
    }

    public CreateRouteParams notifyOnFailure(boolean notifyOnFailure) {
        return set("notifyOnFailure", notifyOnFailure);
    }

    public CreateRouteParams notifyOnSuccess(boolean notifyOnSuccess) {
        return set("notifyOnSuccess", notifyOnSuccess);
    }

    public CreateRouteParams notifyOnRecovery(boolean notifyOnRecovery) {
        return set("notifyOnRecovery", notifyOnRecovery);
    }

    /** Comma-separated addresses to notify. */
    public CreateRouteParams notifyEmails(String notifyEmails) {
        return set("notifyEmails", notifyEmails);
    }

    /** Failures before a failure notification is sent. */
    public CreateRouteParams failureThreshold(int failureThreshold) {
        return set("failureThreshold", failureThreshold);
    }

    /** Destinations tried in order when the primary keeps failing. */
    public CreateRouteParams failoverDestinationIds(List<String> failoverDestinationIds) {
        return set("failoverDestinationIds", failoverDestinationIds);
    }

    /** Attempts on the primary before failing over, 1-5. */
    public CreateRouteParams failoverAfterAttempts(int failoverAfterAttempts) {
        return set("failoverAfterAttempts", failoverAfterAttempts);
    }

    public CreateRouteParams expectedResponse(String expectedResponse) {
        return set("expectedResponse", expectedResponse);
    }
}
