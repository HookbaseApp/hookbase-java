package app.hookbase.models;

/**
 * A provider's signature scheme in full, from {@code providers().get()}. The first six fields are
 * those of {@link ProviderSignatureVerificationSummary}.
 *
 * @param secretInstruction one sentence on where the value comes from or goes, in the vendor's own
 *     UI wording
 * @param secretFormat a required shape: {@code whsec} ({@code whsec_}-prefixed base64) or
 *     {@code basic_auth} (a generated HTTP Basic auth pair); {@code null} when free-form
 * @param verifyToken present for Meta providers, which also use a verify token
 */
public record ProviderSignatureVerification(
        boolean supported,
        String kind,
        String secretSource,
        String secretLabel,
        String secretPlaceholder,
        boolean secretAfterRegistration,
        String algorithm,
        String headerName,
        String secretLocationHint,
        String secretInstruction,
        String secretFormat,
        ProviderVerifyTokenInfo verifyToken) {}
