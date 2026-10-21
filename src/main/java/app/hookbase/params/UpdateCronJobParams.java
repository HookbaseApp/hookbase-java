package app.hookbase.params;

import java.util.Map;

/** Body for {@code cronJobs().update()}. Only the fields you set are changed; set {@code description}, {@code groupId} or {@code notifyEmails} to {@code null} to clear it. */
public final class UpdateCronJobParams extends Params<UpdateCronJobParams> {
    public UpdateCronJobParams name(String name) {
        return set("name", name);
    }

    public UpdateCronJobParams description(String description) {
        return set("description", description);
    }

    /** A standard five-field cron expression, evaluated in {@code timezone}. */
    public UpdateCronJobParams cronExpression(String cronExpression) {
        return set("cronExpression", cronExpression);
    }

    /** IANA time zone (default {@code UTC}). */
    public UpdateCronJobParams timezone(String timezone) {
        return set("timezone", timezone);
    }

    public UpdateCronJobParams url(String url) {
        return set("url", url);
    }

    /** HTTP method (default {@code POST}). */
    public UpdateCronJobParams method(String method) {
        return set("method", method);
    }

    /** Request headers. Stored encrypted. */
    public UpdateCronJobParams headers(Map<String, String> headers) {
        return set("headers", headers);
    }

    /** The request body, sent as-is. */
    public UpdateCronJobParams payload(String payload) {
        return set("payload", payload);
    }

    /** Request timeout in ms (default 30000). */
    public UpdateCronJobParams timeoutMs(int timeoutMs) {
        return set("timeoutMs", timeoutMs);
    }

    /** The cron group to file the job under. */
    public UpdateCronJobParams groupId(String groupId) {
        return set("groupId", groupId);
    }

    /** Send from the static egress IP (plan-gated). */
    public UpdateCronJobParams useStaticIp(boolean useStaticIp) {
        return set("useStaticIp", useStaticIp);
    }

    public UpdateCronJobParams notifyOnFailure(boolean notifyOnFailure) {
        return set("notifyOnFailure", notifyOnFailure);
    }

    public UpdateCronJobParams notifyOnSuccess(boolean notifyOnSuccess) {
        return set("notifyOnSuccess", notifyOnSuccess);
    }

    /** Comma-separated addresses to notify. */
    public UpdateCronJobParams notifyEmails(String notifyEmails) {
        return set("notifyEmails", notifyEmails);
    }

    /** Pause ({@code false}) or resume ({@code true}) the schedule. */
    public UpdateCronJobParams isActive(boolean isActive) {
        return set("isActive", isActive);
    }
}
