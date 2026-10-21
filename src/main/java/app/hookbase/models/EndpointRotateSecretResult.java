package app.hookbase.models;

import app.hookbase.internal.Redact;
import java.util.Map;

/**
 * What {@code endpoints().rotateSecret()} returns.
 *
 * @param secret the new signing secret ({@code whsec_...}); shown once
 * @param previousSecretExpiresAt until this time deliveries are signed with both the new and the
 *     previous secret, so a receiver still holding the old one keeps verifying
 * @param secretVersion the endpoint's secret version after the rotation
 */
public record EndpointRotateSecretResult(String secret, String previousSecretExpiresAt, int secretVersion) {
    /** Credentials are printed as {@code <redacted>}, so logging this record is safe. */
    @Override
    public String toString() {
        return Redact.toString(this, Map.of("secret", Redact.Rule.SECRET));
    }
}
