package app.hookbase.models;

import java.util.List;

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
        Boolean isRevoked) {}
