package app.hookbase.models;

/**
 * The secret fields a source-create flow needs, as catalog list items carry them.
 *
 * @param kind {@code hmac}, {@code shared_secret}, {@code bespoke} or {@code asymmetric} (open
 *     ended); {@code null} for entries with no scheme (those that fall back to {@code custom})
 * @param secretSource who produces the credential: {@code provider} (the vendor issues it, paste
 *     it as {@code signingSecret}), {@code operator} (Hookbase generates it when omitted, paste it
 *     into the vendor), {@code public_key} (paste the vendor's public key), {@code built_in}
 *     (nothing to enter, events are still verified) or {@code none} (the vendor does not sign).
 *     Open ended
 * @param secretAfterRegistration the vendor issues the secret only after the source's URL is
 *     registered with it
 */
public record ProviderSignatureVerificationSummary(
        boolean supported,
        String kind,
        String secretSource,
        String secretLabel,
        String secretPlaceholder,
        boolean secretAfterRegistration) {}
