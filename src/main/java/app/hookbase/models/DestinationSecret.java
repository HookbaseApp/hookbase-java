package app.hookbase.models;

import app.hookbase.internal.Redact;
import java.util.Map;

/**
 * What {@code destinations().revealSecret()} and {@code destinations().rotateSecret()} return: an
 * HTTP destination's signing secret, which Hookbase signs every delivery to it with (Standard
 * Webhooks {@code v1}; verify with {@code new Webhook(signingSecret)}).
 *
 * @param signingSecret the signing secret in full ({@code whsec_...})
 * @param previousSecretExpiresAt until this time deliveries are signed with both this secret and
 *     the previous one, so a receiver still holding the old secret keeps verifying; {@code null}
 *     when no grace period is running
 */
public record DestinationSecret(String signingSecret, String previousSecretExpiresAt) {
    /** Credentials are printed as {@code <redacted>}, so logging this record is safe. */
    @Override
    public String toString() {
        return Redact.toString(this, Map.of("signingSecret", Redact.Rule.SECRET));
    }
}
