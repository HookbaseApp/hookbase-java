package app.hookbase.models;

import app.hookbase.internal.Redact;
import java.util.Map;

/**
 * A verification token a vendor (Notion) delivered to the ingest URL during registration. The
 * request carrying it was unauthenticated, so it is held: paste {@code token} into the vendor's
 * verify form, and once the vendor accepts it call {@code sources().adoptPendingVerification()}.
 *
 * @param clientIp the address the token came from, or {@code null}
 */
public record PendingVerification(String provider, String token, String receivedAt, String clientIp) {
    /** Credentials are printed as {@code <redacted>}, so logging this record is safe. */
    @Override
    public String toString() {
        return Redact.toString(this, Map.of("token", Redact.Rule.SECRET));
    }
}
