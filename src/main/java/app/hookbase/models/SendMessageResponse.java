package app.hookbase.models;

import java.util.List;

/**
 * What {@code messages().send()} returns.
 *
 * @param eventId the outbound event created (or, for a repeated idempotency key, the original one)
 * @param messagesQueued how many outbound messages were queued: one per targeted endpoint
 * @param endpoints the endpoints the event was queued for. These are endpoints, not messages: list
 *     the messages to get message ids
 * @param messagesHeld how many of the queued messages are {@code held} instead of sent, because
 *     their endpoint's circuit is open; they go out once it recovers. 0 when none are
 */
public record SendMessageResponse(String eventId, int messagesQueued, List<SendMessageEndpoint> endpoints, int messagesHeld) {
    public SendMessageResponse {
        endpoints = endpoints == null ? List.of() : List.copyOf(endpoints);
    }

    /**
     * A response with no held messages: the constructor this record had before holds, kept so code
     * that builds one (a test stub, say) still compiles.
     */
    public SendMessageResponse(String eventId, int messagesQueued, List<SendMessageEndpoint> endpoints) {
        this(eventId, messagesQueued, endpoints, 0);
    }
}
