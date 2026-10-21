package app.hookbase.models;

import app.hookbase.internal.JsonTextDeserializer;
import app.hookbase.internal.Redact;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Map;

/**
 * A local-development tunnel.
 *
 * @param status e.g. {@code connected} or {@code disconnected}
 * @param authToken the token the CLI connects with; set on the create response only (the API sends
 *     it as {@code auth_token} there)
 * @param direction {@code inbound} or {@code bidirectional}
 * @param allowedHosts hosts a bidirectional tunnel may reach; {@code null} when unrestricted
 */
public record Tunnel(
        String id,
        String organizationId,
        String userId,
        String name,
        String subdomain,
        Integer localPort,
        String status,
        @JsonAlias("auth_token") String authToken,
        String createdAt,
        String lastConnectedAt,
        String lastRequestAt,
        long totalRequests,
        String customDomainId,
        String direction,
        @JsonDeserialize(using = JsonTextDeserializer.class) List<String> allowedHosts) {
    /** Credentials are printed as {@code <redacted>}, so logging this record is safe. */
    @Override
    public String toString() {
        return Redact.toString(this, Map.of("authToken", Redact.Rule.SECRET));
    }
}
