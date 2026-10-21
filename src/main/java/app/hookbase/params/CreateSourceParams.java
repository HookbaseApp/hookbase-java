package app.hookbase.params;

import java.util.List;

/**
 * Body for {@code sources().create()}. {@code name} and {@code slug} are required.
 *
 * <p>The setters are exactly the fields {@code POST /api/sources} accepts; its schema is strict,
 * so an unknown key is a 400.
 */
public final class CreateSourceParams extends Params<CreateSourceParams> {
    public CreateSourceParams name(String name) {
        return set("name", name);
    }

    /** Forms the ingest URL, {@code /ingest/<org>/<slug>}, and cannot be changed later. */
    public CreateSourceParams slug(String slug) {
        return set("slug", slug);
    }

    /** Any string; see {@code KnownSourceProviders} for the values accepted today. */
    public CreateSourceParams provider(String provider) {
        return set("provider", provider);
    }

    public CreateSourceParams description(String description) {
        return set("description", description);
    }

    /**
     * What to supply depends on the provider (the catalog's
     * {@code signatureVerification.secretSource}; see {@code providers().get()}):
     *
     * <ul>
     *   <li>{@code provider}: the vendor issues it. Required for several ({@code standard-webhooks}
     *       and its aliases, {@code airtable}, {@code gitlab-signing-token},
     *       {@code supabase-auth-hooks}, {@code jira}, {@code discord}, {@code sendgrid}); a
     *       missing or malformed one is a 400.
     *   <li>{@code operator}: omit it and the API generates one, returned once on the created source.
     *   <li>{@code public_key}: the vendor's public key (PEM or base64).
     *   <li>{@code built_in} ({@code wise}, {@code wise-sandbox}, {@code aws-sns}): send nothing;
     *       any secret is a 400.
     * </ul>
     */
    public CreateSourceParams signingSecret(String signingSecret) {
        return set("signingSecret", signingSecret);
    }

    /** Meta providers ({@code whatsapp}) only; a 400 for any other. Omit it and the API generates one. */
    public CreateSourceParams verifyToken(String verifyToken) {
        return set("verifyToken", verifyToken);
    }

    /** Reject events whose signature fails verification. Defaults to false. */
    public CreateSourceParams rejectInvalidSignatures(boolean rejectInvalidSignatures) {
        return set("rejectInvalidSignatures", rejectInvalidSignatures);
    }

    public CreateSourceParams rateLimitPerMinute(Integer rateLimitPerMinute) {
        return set("rateLimitPerMinute", rateLimitPerMinute);
    }

    /** {@code none}, {@code allowlist}, {@code denylist} or {@code both}. */
    public CreateSourceParams ipFilterMode(String ipFilterMode) {
        return set("ipFilterMode", ipFilterMode);
    }

    public CreateSourceParams ipAllowlist(List<String> ipAllowlist) {
        return set("ipAllowlist", ipAllowlist);
    }

    public CreateSourceParams ipDenylist(List<String> ipDenylist) {
        return set("ipDenylist", ipDenylist);
    }

    /** JSONPath expressions whose values are encrypted at rest. */
    public CreateSourceParams encryptFields(List<String> encryptFields) {
        return set("encryptFields", encryptFields);
    }

    /** JSONPath expressions whose values are masked in stored payloads. */
    public CreateSourceParams maskFields(List<String> maskFields) {
        return set("maskFields", maskFields);
    }

    public CreateSourceParams dedupEnabled(boolean dedupEnabled) {
        return set("dedupEnabled", dedupEnabled);
    }

    /** {@code auto} (the default), {@code provider_id}, {@code payload_hash}, {@code idempotency_key} or {@code none}. */
    public CreateSourceParams dedupStrategy(String dedupStrategy) {
        return set("dedupStrategy", dedupStrategy);
    }

    /** Deduplication window in hours, 1 to 168. */
    public CreateSourceParams dedupWindowHours(int dedupWindowHours) {
        return set("dedupWindowHours", dedupWindowHours);
    }

    /** Header to deduplicate on when {@code dedupStrategy} is {@code idempotency_key}. */
    public CreateSourceParams dedupCustomHeader(String dedupCustomHeader) {
        return set("dedupCustomHeader", dedupCustomHeader);
    }

    /** Payloads are never stored at rest. */
    public CreateSourceParams transientMode(boolean transientMode) {
        return set("transientMode", transientMode);
    }

    /**
     * Restrict the ingest endpoint to these HTTP verbs ({@code GET}, {@code POST}, {@code PUT},
     * {@code PATCH}, {@code DELETE}, {@code HEAD}). Omit or pass an empty list to accept any method.
     */
    public CreateSourceParams allowedMethods(List<String> allowedMethods) {
        return set("allowedMethods", allowedMethods);
    }
}
