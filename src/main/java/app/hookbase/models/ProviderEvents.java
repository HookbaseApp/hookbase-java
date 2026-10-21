package app.hookbase.models;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * A provider's event types and sample payloads, from {@code providers().events()}.
 *
 * @param samplePayloads example payloads keyed by event type
 */
public record ProviderEvents(String providerId, String providerName, List<ProviderEventType> eventTypes, JsonNode samplePayloads) {
    public ProviderEvents {
        eventTypes = eventTypes == null ? List.of() : List.copyOf(eventTypes);
    }
}
