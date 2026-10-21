package app.hookbase.models;

import java.util.List;

/**
 * What {@code messages().send()} returns.
 *
 * @param eventId the outbound event created (or, for a repeated idempotency key, the original one)
 * @param messagesQueued how many outbound messages were queued: one per targeted endpoint
 * @param endpoints the endpoints the event was queued for. These are endpoints, not messages: list
 *     the messages to get message ids
 */
public record SendMessageResponse(String eventId, int messagesQueued, List<SendMessageEndpoint> endpoints) {
    public SendMessageResponse {
        endpoints = endpoints == null ? List.of() : List.copyOf(endpoints);
    }
}
