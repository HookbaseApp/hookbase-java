package app.hookbase.models;

import app.hookbase.internal.Redact;
import java.util.List;
import java.util.Map;

/**
 * What {@code sources().create()} returns: a {@link Source} plus the one look at its signing secret
 * you get. See {@link Source} for the shared fields; {@link #toSource()} drops the secret.
 *
 * @param signingSecret the signing secret in full, this once; {@code null} when there is none to
 *     show: the provider verifies without one ({@code verifiesWithoutSecret}), the vendor issues it
 *     after registration ({@code awaitingSigningSecret}), or the provider does not sign
 * @param verifyToken Meta providers ({@code whatsapp}) only: the verify token to paste into Meta's
 *     webhook setup, in full, this once; {@code sources().verifyToken()} reveals it later
 */
public record SourceWithSecret(
        String id,
        String organizationId,
        String name,
        String slug,
        String provider,
        String description,
        boolean hasSigningSecret,
        String signingSecretLast4,
        boolean verifiesWithoutSecret,
        boolean awaitingSigningSecret,
        boolean rejectInvalidSignatures,
        Integer rateLimitPerMinute,
        boolean isActive,
        String customDomainId,
        String ipFilterMode,
        List<String> ipAllowlist,
        List<String> ipDenylist,
        List<String> encryptFields,
        List<String> maskFields,
        boolean dedupEnabled,
        String dedupStrategy,
        int dedupWindowHours,
        String dedupCustomHeader,
        boolean transientMode,
        List<String> allowedMethods,
        long eventCount,
        long routeCount,
        String ingestUrl,
        String createdAt,
        String updatedAt,
        String signingSecret,
        String verifyToken) {

    public SourceWithSecret {
        ipAllowlist = ipAllowlist == null ? List.of() : List.copyOf(ipAllowlist);
        ipDenylist = ipDenylist == null ? List.of() : List.copyOf(ipDenylist);
        encryptFields = encryptFields == null ? List.of() : List.copyOf(encryptFields);
        maskFields = maskFields == null ? List.of() : List.copyOf(maskFields);
        allowedMethods = allowedMethods == null ? List.of() : List.copyOf(allowedMethods);
    }

    /** The same source without the secret and verify token. */
    public Source toSource() {
        return new Source(id, organizationId, name, slug, provider, description, hasSigningSecret, signingSecretLast4,
                verifiesWithoutSecret, awaitingSigningSecret, rejectInvalidSignatures, rateLimitPerMinute, isActive,
                customDomainId, ipFilterMode, ipAllowlist, ipDenylist, encryptFields, maskFields, dedupEnabled,
                dedupStrategy, dedupWindowHours, dedupCustomHeader, transientMode, allowedMethods, eventCount,
                routeCount, ingestUrl, createdAt, updatedAt);
    }

    /** Credentials are printed as {@code <redacted>}, so logging this record is safe. */
    @Override
    public String toString() {
        return Redact.toString(this, Map.of("signingSecret", Redact.Rule.SECRET, "verifyToken", Redact.Rule.SECRET));
    }
}
