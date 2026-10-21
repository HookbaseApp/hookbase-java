package app.hookbase.models;

import app.hookbase.internal.Redact;
import java.util.Map;

/**
 * What {@code endpoints().rotateSecret()} returns.
 *
 * @param signatureScheme {@code hmac-sha256} or {@code ed25519}
 * @param secret the new signing secret ({@code whsec_...}), shown once; {@code null} for an
 *     {@code ed25519} endpoint, which gets a new key pair instead
 * @param publicKey the new public key ({@code ed25519} only)
 * @param previousPublicKey the public key being rotated out ({@code ed25519} only)
 * @param previousSecretExpiresAt until this time deliveries are signed with both the new and the
 *     previous secret, so a receiver still holding the old one keeps verifying
 * @param secretVersion the endpoint's secret version after the rotation
 */
public record EndpointRotateSecretResult(
        String signatureScheme,
        String secret,
        String publicKey,
        String previousPublicKey,
        String previousSecretExpiresAt,
        int secretVersion) {
    /** Credentials are printed as {@code <redacted>}, so logging this record is safe. */
    @Override
    public String toString() {
        return Redact.toString(this, Map.of("secret", Redact.Rule.SECRET));
    }
}
