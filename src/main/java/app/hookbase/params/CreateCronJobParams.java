package app.hookbase.params;

import java.util.Map;

/** Body for {@code cronJobs().create()}. {@code name}, {@code cronExpression} and {@code url} are required. New jobs start active. */
public final class CreateCronJobParams extends Params<CreateCronJobParams> {
    public CreateCronJobParams name(String name) {
        return set("name", name);
    }

    public CreateCronJobParams description(String description) {
        return set("description", description);
    }

    /** A standard five-field cron expression, evaluated in {@code timezone}. */
    public CreateCronJobParams cronExpression(String cronExpression) {
        return set("cronExpression", cronExpression);
    }

    /** IANA time zone (default {@code UTC}). */
    public CreateCronJobParams timezone(String timezone) {
        return set("timezone", timezone);
    }

    public CreateCronJobParams url(String url) {
        return set("url", url);
    }

    /** HTTP method (default {@code POST}). */
    public CreateCronJobParams method(String method) {
        return set("method", method);
    }

    /** Request headers. Stored encrypted. */
    public CreateCronJobParams headers(Map<String, String> headers) {
        return set("headers", headers);
    }

    /** The request body, sent as-is. */
    public CreateCronJobParams payload(String payload) {
        return set("payload", payload);
    }

    /** Request timeout in ms (default 30000). */
    public CreateCronJobParams timeoutMs(int timeoutMs) {
        return set("timeoutMs", timeoutMs);
    }

    /** The cron group to file the job under. */
    public CreateCronJobParams groupId(String groupId) {
        return set("groupId", groupId);
    }

    /** Send from the static egress IP (plan-gated). */
    public CreateCronJobParams useStaticIp(boolean useStaticIp) {
        return set("useStaticIp", useStaticIp);
    }

    public CreateCronJobParams notifyOnFailure(boolean notifyOnFailure) {
        return set("notifyOnFailure", notifyOnFailure);
    }

    public CreateCronJobParams notifyOnSuccess(boolean notifyOnSuccess) {
        return set("notifyOnSuccess", notifyOnSuccess);
    }

    /** Comma-separated addresses to notify. */
    public CreateCronJobParams notifyEmails(String notifyEmails) {
        return set("notifyEmails", notifyEmails);
    }
}
