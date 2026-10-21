package app.hookbase.params;

import app.hookbase.models.EndpointHeader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Body for {@code endpoints().create()}. {@code url} is required; the application comes from the call. */
public final class CreateEndpointParams extends Params<CreateEndpointParams> {
    public CreateEndpointParams url(String url) {
        return set("url", url);
    }

    /** Max 500 characters. */
    public CreateEndpointParams description(String description) {
        return set("description", description);
    }

    /** Custom headers sent with every delivery, max 10. Sent as {@code [{name, value}]} in the map's iteration order. */
    public CreateEndpointParams headers(Map<String, String> headers) {
        if (headers == null) return set("headers", null);
        List<EndpointHeader> list = new ArrayList<>();
        headers.forEach((name, value) -> list.add(new EndpointHeader(name, value)));
        return set("headers", list);
    }

    /** Custom headers sent with every delivery, max 10, in the API's own shape. */
    public CreateEndpointParams headers(List<EndpointHeader> headers) {
        return set("headers", headers == null ? null : List.copyOf(headers));
    }

    /** Per-delivery HTTP timeout in seconds, 1-120. */
    public CreateEndpointParams timeoutSeconds(int timeoutSeconds) {
        return set("timeoutSeconds", timeoutSeconds);
    }

    /** Delivery rate cap in requests per second, 0-1000. 0 means unlimited. */
    public CreateEndpointParams rateLimitPerSecond(int rateLimitPerSecond) {
        return set("rateLimitPerSecond", rateLimitPerSecond);
    }

    /**
     * Status codes that count as success, max 20: exact codes as {@code Integer}s ({@code 200}) or
     * class patterns as {@code String}s ({@code "2xx"}).
     */
    public CreateEndpointParams successStatusCodes(List<?> successStatusCodes) {
        return set("successStatusCodes", successStatusCodes);
    }

    /** {@code exponential}, {@code linear} or {@code fixed}. */
    public CreateEndpointParams backoffType(String backoffType) {
        return set("backoffType", backoffType);
    }

    /** Retry delays in seconds, max 10 entries, each 1-86400. */
    public CreateEndpointParams retryDelays(List<Integer> retryDelays) {
        return set("retryDelays", retryDelays);
    }

    /** Informational note recording which IPs this endpoint expects traffic from, max 1000 characters. */
    public CreateEndpointParams ipAllowlistNotes(String ipAllowlistNotes) {
        return set("ipAllowlistNotes", ipAllowlistNotes);
    }

    /** Deliver through the dedicated static-IP proxy. */
    public CreateEndpointParams useStaticIp(boolean useStaticIp) {
        return set("useStaticIp", useStaticIp);
    }

    /** Consecutive failures that open the circuit, 1-100. */
    public CreateEndpointParams circuitFailureThreshold(int circuitFailureThreshold) {
        return set("circuitFailureThreshold", circuitFailureThreshold);
    }

    /** Consecutive successes that close a half-open circuit, 1-100. */
    public CreateEndpointParams circuitSuccessThreshold(int circuitSuccessThreshold) {
        return set("circuitSuccessThreshold", circuitSuccessThreshold);
    }

    /** Seconds an open circuit waits before it probes again, 10-3600. */
    public CreateEndpointParams circuitCooldownSeconds(int circuitCooldownSeconds) {
        return set("circuitCooldownSeconds", circuitCooldownSeconds);
    }
}
