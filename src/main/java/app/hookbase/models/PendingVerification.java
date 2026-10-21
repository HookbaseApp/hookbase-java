package app.hookbase.models;

/**
 * A verification token a vendor (Notion) delivered to the ingest URL during registration. The
 * request carrying it was unauthenticated, so it is held: paste {@code token} into the vendor's
 * verify form, and once the vendor accepts it call {@code sources().adoptPendingVerification()}.
 *
 * @param clientIp the address the token came from, or {@code null}
 */
public record PendingVerification(String provider, String token, String receivedAt, String clientIp) {}
