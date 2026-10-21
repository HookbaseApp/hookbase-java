package app.hookbase.models;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * One provider in full, from {@code providers().get()}.
 *
 * @param hookbaseProvider the value to use as a source's {@code provider}
 * @param signatureVerification {@code null} when the provider does not sign
 * @param samplePayloads example payloads keyed by event type
 * @param ipRanges the vendor's published sending ranges, or {@code null}
 */
public record CatalogProviderDetail(
        String id,
        String name,
        String category,
        String description,
        String website,
        String docsUrl,
        String icon,
        String hookbaseProvider,
        ProviderSignatureVerification signatureVerification,
        List<ProviderEventType> eventTypes,
        JsonNode samplePayloads,
        List<String> ipRanges,
        List<String> setupGuide) {

    public CatalogProviderDetail {
        eventTypes = eventTypes == null ? List.of() : List.copyOf(eventTypes);
        ipRanges = ipRanges == null ? null : List.copyOf(ipRanges);
        setupGuide = setupGuide == null ? List.of() : List.copyOf(setupGuide);
    }
}
