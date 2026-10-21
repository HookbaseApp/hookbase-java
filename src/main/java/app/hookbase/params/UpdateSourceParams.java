package app.hookbase.params;

import java.util.List;

/**
 * Body for {@code sources().update()}. Only the fields you set are changed. Set a nullable field
 * ({@code description}, {@code provider}, {@code signingSecret}, {@code verifyToken},
 * {@code rateLimitPerMinute}, the four lists, {@code dedupStrategy}, {@code dedupWindowHours},
 * {@code dedupCustomHeader}, {@code allowedMethods}) to {@code null} to clear it.
 *
 * <p>The slug cannot be changed. The setters are exactly the fields {@code PATCH /api/sources/:id}
 * accepts; its schema is strict, so an unknown key is a 400.
 */
public final class UpdateSourceParams extends Params<UpdateSourceParams> {
    public UpdateSourceParams name(String name) {
        return set("name", name);
    }

    public UpdateSourceParams description(String description) {
        return set("description", description);
    }

    /** Any string; see {@code KnownSourceProviders}. */
    public UpdateSourceParams provider(String provider) {
        return set("provider", provider);
    }

    public UpdateSourceParams isActive(boolean isActive) {
        return set("isActive", isActive);
    }

    /** {@code null} removes the secret. */
    public UpdateSourceParams signingSecret(String signingSecret) {
        return set("signingSecret", signingSecret);
    }

    /** Meta providers ({@code whatsapp}) only. {@code ""} or {@code null} generates a fresh one. */
    public UpdateSourceParams verifyToken(String verifyToken) {
        return set("verifyToken", verifyToken);
    }

    public UpdateSourceParams rejectInvalidSignatures(boolean rejectInvalidSignatures) {
        return set("rejectInvalidSignatures", rejectInvalidSignatures);
    }

    public UpdateSourceParams rateLimitPerMinute(Integer rateLimitPerMinute) {
        return set("rateLimitPerMinute", rateLimitPerMinute);
    }

    /** {@code none}, {@code allowlist}, {@code denylist} or {@code both}. */
    public UpdateSourceParams ipFilterMode(String ipFilterMode) {
        return set("ipFilterMode", ipFilterMode);
    }

    public UpdateSourceParams ipAllowlist(List<String> ipAllowlist) {
        return set("ipAllowlist", ipAllowlist);
    }

    public UpdateSourceParams ipDenylist(List<String> ipDenylist) {
        return set("ipDenylist", ipDenylist);
    }

    public UpdateSourceParams encryptFields(List<String> encryptFields) {
        return set("encryptFields", encryptFields);
    }

    public UpdateSourceParams maskFields(List<String> maskFields) {
        return set("maskFields", maskFields);
    }

    public UpdateSourceParams dedupEnabled(boolean dedupEnabled) {
        return set("dedupEnabled", dedupEnabled);
    }

    public UpdateSourceParams dedupStrategy(String dedupStrategy) {
        return set("dedupStrategy", dedupStrategy);
    }

    public UpdateSourceParams dedupWindowHours(Integer dedupWindowHours) {
        return set("dedupWindowHours", dedupWindowHours);
    }

    public UpdateSourceParams dedupCustomHeader(String dedupCustomHeader) {
        return set("dedupCustomHeader", dedupCustomHeader);
    }

    public UpdateSourceParams transientMode(boolean transientMode) {
        return set("transientMode", transientMode);
    }

    /** Pass an empty list to accept any method again. */
    public UpdateSourceParams allowedMethods(List<String> allowedMethods) {
        return set("allowedMethods", allowedMethods);
    }
}
