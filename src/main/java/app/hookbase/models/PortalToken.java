package app.hookbase.models;

import app.hookbase.internal.Redact;
import java.util.List;
import java.util.Map;

/**
 * A token for the embeddable customer portal, scoped to one application.
 *
 * @param token the plaintext token: set on the {@code create} response only
 * @param tokenPrefix the start of the token, for telling tokens apart in a list
 * @param scopes {@code read} and/or {@code write}
 */
public record PortalToken(
        String id,
        String applicationId,
        String token,
        String tokenPrefix,
        String name,
        List<String> scopes,
        String expiresAt,
        String createdAt,
        Boolean isExpired,
        Boolean isRevoked) {
    /** Credentials are printed as {@code <redacted>}, so logging this record is safe. */
    @Override
    public String toString() {
        return Redact.toString(this, Map.of("token", Redact.Rule.SECRET));
    }
}
