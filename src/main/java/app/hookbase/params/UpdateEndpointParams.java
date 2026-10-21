package app.hookbase.params;

import app.hookbase.models.EndpointHeader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Body for {@code endpoints().update()}. Only the fields you set are changed. {@code description},
 * {@code successStatusCodes}, {@code backoffType}, {@code retryDelays} and {@code ipAllowlistNotes}
 * can be set to {@code null} to clear them (back to the default); pass an empty header list to clear
 * the headers.
 */
public final class UpdateEndpointParams extends Params<UpdateEndpointParams> {
    public UpdateEndpointParams url(String url) {
        return set("url", url);
    }

    /** Max 500 characters. */
    public UpdateEndpointParams description(String description) {
        return set("description", description);
    }

    public UpdateEndpointParams isDisabled(boolean isDisabled) {
        return set("isDisabled", isDisabled);
    }

    /** Why the endpoint was disabled, max 500 characters. Only read when {@code isDisabled} is true. */
    public UpdateEndpointParams disabledReason(String disabledReason) {
        return set("disabledReason", disabledReason);
    }

    /** Custom headers sent with every delivery, max 10. Sent as {@code [{name, value}]} in the map's iteration order. */
    public UpdateEndpointParams headers(Map<String, String> headers) {
        if (headers == null) return set("headers", null);
        List<EndpointHeader> list = new ArrayList<>();
        headers.forEach((name, value) -> list.add(new EndpointHeader(name, value)));
        return set("headers", list);
    }

    /** Custom headers sent with every delivery, max 10, in the API's own shape. */
    public UpdateEndpointParams headers(List<EndpointHeader> headers) {
        return set("headers", headers == null ? null : List.copyOf(headers));
    }

    /** Per-delivery HTTP timeout in seconds, 1-120. */
    public UpdateEndpointParams timeoutSeconds(int timeoutSeconds) {
        return set("timeoutSeconds", timeoutSeconds);
    }

    /** Delivery rate cap in requests per second, 0-1000. 0 means unlimited. */
    public UpdateEndpointParams rateLimitPerSecond(int rateLimitPerSecond) {
        return set("rateLimitPerSecond", rateLimitPerSecond);
    }

    /**
     * Status codes that count as success, max 20: exact codes as {@code Integer}s ({@code 200}) or
     * class patterns as {@code String}s ({@code "2xx"}).
     */
    public UpdateEndpointParams successStatusCodes(List<?> successStatusCodes) {
        return set("successStatusCodes", successStatusCodes);
    }

    /** {@code exponential}, {@code linear} or {@code fixed}. */
    public UpdateEndpointParams backoffType(String backoffType) {
        return set("backoffType", backoffType);
    }

    /** Retry delays in seconds, max 10 entries, each 1-86400. */
    public UpdateEndpointParams retryDelays(List<Integer> retryDelays) {
        return set("retryDelays", retryDelays);
    }

    /** Informational note recording which IPs this endpoint expects traffic from, max 1000 characters. */
    public UpdateEndpointParams ipAllowlistNotes(String ipAllowlistNotes) {
        return set("ipAllowlistNotes", ipAllowlistNotes);
    }

    /** Deliver through the dedicated static-IP proxy. */
    public UpdateEndpointParams useStaticIp(boolean useStaticIp) {
        return set("useStaticIp", useStaticIp);
    }

    /** Consecutive failures that open the circuit, 1-100. */
    public UpdateEndpointParams circuitFailureThreshold(int circuitFailureThreshold) {
        return set("circuitFailureThreshold", circuitFailureThreshold);
    }

    /** Consecutive successes that close a half-open circuit, 1-100. */
    public UpdateEndpointParams circuitSuccessThreshold(int circuitSuccessThreshold) {
        return set("circuitSuccessThreshold", circuitSuccessThreshold);
    }

    /** Seconds an open circuit waits before it probes again, 10-3600. */
    public UpdateEndpointParams circuitCooldownSeconds(int circuitCooldownSeconds) {
        return set("circuitCooldownSeconds", circuitCooldownSeconds);
    }
}
