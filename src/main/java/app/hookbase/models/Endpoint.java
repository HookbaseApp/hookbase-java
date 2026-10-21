package app.hookbase.models;

import app.hookbase.internal.Redact;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An outbound webhook endpoint: a URL of your customer's that receives the events it is subscribed
 * to.
 *
 * @param secretPrefix the first twelve characters of the signing secret, followed by {@code ...}
 * @param secretVersion increments on every rotation
 * @param headers custom delivery headers, in the order the API sent them; empty when there are none
 *     (see {@link #headerMap()} for a map view)
 * @param timeoutSeconds per-delivery HTTP timeout in seconds
 * @param rateLimitPerSecond delivery rate cap in requests per second; 0 means unlimited
 * @param successStatusCodes status codes counted as success: exact codes ({@code "200"}) or class
 *     patterns ({@code "2xx"}); {@code null} for the 200-299 default
 * @param backoffType {@code exponential}, {@code linear} or {@code fixed}; {@code null} means the
 *     platform default (exponential)
 * @param retryDelays explicit retry delays in seconds, or {@code null} for the default schedule
 * @param ipAllowlistNotes informational note recording which IPs this endpoint expects traffic from
 * @param useStaticIp whether deliveries go through the dedicated static-IP proxy
 * @param circuitState {@code closed}, {@code open} or {@code half_open}
 * @param subscriptionCount set on list responses only
 * @param secret the plaintext signing secret: set on the {@code create} response only, and shown
 *     only that once
 */
public record Endpoint(
        String id,
        String applicationId,
        String url,
        String description,
        String secretPrefix,
        boolean hasSecret,
        int secretVersion,
        List<EndpointHeader> headers,
        int timeoutSeconds,
        boolean isDisabled,
        String disabledAt,
        String disabledReason,
        int rateLimitPerSecond,
        List<String> successStatusCodes,
        String backoffType,
        List<Integer> retryDelays,
        String ipAllowlistNotes,
        boolean useStaticIp,
        String circuitState,
        String circuitOpenedAt,
        int circuitFailureCount,
        int circuitFailureThreshold,
        int circuitSuccessThreshold,
        int circuitCooldownSeconds,
        long totalMessages,
        long totalSuccesses,
        long totalFailures,
        Double avgResponseTimeMs,
        String lastSuccessAt,
        String lastFailureAt,
        Integer lastResponseStatus,
        boolean isVerified,
        String verifiedAt,
        String createdAt,
        String updatedAt,
        String createdBy,
        String apiKeyId,
        Integer subscriptionCount,
        String secret) {

    public Endpoint {
        headers = headers == null ? List.of() : List.copyOf(headers);
    }

    /** The headers as a name-to-value map, in the order the API sent them (a later duplicate name wins). */
    public Map<String, String> headerMap() {
        Map<String, String> out = new LinkedHashMap<>();
        for (EndpointHeader h : headers) {
            if (h != null && h.name() != null) out.put(h.name(), h.value() == null ? "" : h.value());
        }
        return out;
    }

    /** Credentials are printed as {@code <redacted>}, so logging this record is safe. */
    @Override
    public String toString() {
        return Redact.toString(this, Map.of("secret", Redact.Rule.SECRET));
    }
}
