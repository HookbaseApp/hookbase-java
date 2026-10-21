package app.hookbase.models;

/**
 * A provider as the catalog list returns it.
 *
 * @param id the catalog's id
 * @param hookbaseProvider the value to use as a source's {@code provider}. It differs from
 *     {@code id} for some entries (catalog {@code mollie-nextgen} is provider {@code mollie}, and
 *     several entries map to {@code custom})
 * @param signatureVerification the secret fields a create flow needs; {@code null} when the
 *     provider has none, and on API versions that predate the summary on list items (fall back to
 *     {@code providers().get(id)} then)
 */
public record CatalogProvider(
        String id,
        String name,
        String category,
        String description,
        String icon,
        int eventTypeCount,
        String hookbaseProvider,
        boolean hasSignatureVerification,
        boolean hasIpRanges,
        ProviderSignatureVerificationSummary signatureVerification) {}
