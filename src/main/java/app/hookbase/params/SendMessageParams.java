package app.hookbase.params;

import java.util.List;
import java.util.Map;

/**
 * Body for {@code messages().send()}. {@code eventType} and {@code payload} are required; the
 * application comes from the call.
 */
public final class SendMessageParams extends Params<SendMessageParams> {
    /** The event type's name, e.g. {@code order.created}. */
    public SendMessageParams eventType(String eventType) {
        return set("eventType", eventType);
    }

    /** The event payload: a {@code Map}, a list, or anything else Jackson serializes. */
    public SendMessageParams payload(Object payload) {
        return set("payload", payload);
    }

    /**
     * Deliver to these endpoints only (max 100) instead of every subscribed endpoint. Sent as
     * {@code endpointIds}, and also as {@code endpointId} when there is exactly one.
     */
    public SendMessageParams endpointIds(List<String> endpointIds) {
        return set("endpointIds", endpointIds);
    }

    /** Deliver to this one endpoint only. Merged with {@code endpointIds}. */
    public SendMessageParams endpointId(String endpointId) {
        return set("endpointId", endpointId);
    }

    /**
     * Makes the send safe to repeat: the API returns the first send's result for a repeated key
     * instead of sending again. Max 255 characters. Wins over {@code RequestOptions.idempotencyKey}.
     * When neither is set the SDK generates one per {@code send} call, so its own retries cannot
     * send twice.
     */
    public SendMessageParams idempotencyKey(String idempotencyKey) {
        return set("idempotencyKey", idempotencyKey);
    }

    /** Flat key/value pairs; values must be strings (max 1000), numbers, booleans or null. Max 50 keys. */
    public SendMessageParams metadata(Map<String, ?> metadata) {
        return set("metadata", metadata);
    }

    /** Labels that subscriptions' label filters match on; values max 255 characters. */
    public SendMessageParams labels(Map<String, String> labels) {
        return set("labels", labels);
    }

    /** 0=Critical, 1=High, 2=Normal, 3=Low, 4=Bulk. */
    public SendMessageParams priority(int priority) {
        return set("priority", priority);
    }
}
