package app.hookbase.models;

import java.util.List;

/**
 * An inbound source as the API returns it.
 *
 * <p>The signing secret is never included: responses carry {@code hasSigningSecret} and the last
 * four characters instead. The full value comes back exactly twice, from {@code sources().create()}
 * (a {@link SourceWithSecret}) and from {@code sources().revealSecret()}.
 *
 * <p>{@code hasSigningSecret == false} is not always a problem; check the two flags beside it.
 *
 * @param provider any string; see {@link KnownSourceProviders} for the values accepted today
 * @param signingSecretLast4 last four characters of the signing secret prefixed with {@code ...},
 *     or {@code null} when none is set
 * @param verifiesWithoutSecret the provider verifies with a key Hookbase already holds ({@code wise},
 *     {@code wise-sandbox}, {@code aws-sns}): there is no secret to set, and {@code hasSigningSecret}
 *     stays false on a fully verified source
 * @param awaitingSigningSecret the vendor issues the secret only once this URL is registered with it
 *     ({@code asana}, {@code notion}) and it has not been set yet; events are unverified until it is.
 *     For Notion, see {@code sources().getPendingVerification()}
 * @param rejectInvalidSignatures reject events whose signature fails verification; when false a
 *     failing event is flagged {@code signatureValid: false} and delivered anyway
 * @param ipFilterMode {@code none}, {@code allowlist}, {@code denylist} or {@code both}
 * @param encryptFields JSONPath expressions whose values are encrypted at rest
 * @param maskFields JSONPath expressions whose values are masked in stored payloads
 * @param dedupStrategy {@code auto} (the default), {@code provider_id}, {@code payload_hash},
 *     {@code idempotency_key} or {@code none}
 * @param dedupCustomHeader header to deduplicate on when {@code dedupStrategy} is
 *     {@code idempotency_key}
 * @param transientMode payloads are never stored at rest
 * @param allowedMethods HTTP verbs the ingest endpoint accepts ({@code GET}, {@code POST},
 *     {@code PUT}, {@code PATCH}, {@code DELETE}, {@code HEAD}); empty means any method
 * @param ingestUrl returned by {@code get()} and {@code create()}, not by {@code list()}
 */
public record Source(
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
        String updatedAt) {

    public Source {
        ipAllowlist = ipAllowlist == null ? List.of() : List.copyOf(ipAllowlist);
        ipDenylist = ipDenylist == null ? List.of() : List.copyOf(ipDenylist);
        encryptFields = encryptFields == null ? List.of() : List.copyOf(encryptFields);
        maskFields = maskFields == null ? List.of() : List.copyOf(maskFields);
        allowedMethods = allowedMethods == null ? List.of() : List.copyOf(allowedMethods);
    }
}
