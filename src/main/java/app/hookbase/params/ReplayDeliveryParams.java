package app.hookbase.params;

import app.hookbase.models.ReplayTransformOverride;
import java.util.Map;

/**
 * Body for {@code deliveries().replay()}: what to change about the replayed delivery. Every field
 * is optional, and a replay with none set re-sends the delivery as it was.
 *
 * <p>Set at most one of {@code modifiedPayload} and {@code payloadPatch}: the SDK refuses a replay
 * with both before sending it, as the API would.
 *
 * <p>Unlike other params classes, a {@code null} argument here means "no change": the field is left
 * out of the request, and an earlier value for it is dropped. The API treats any field that is
 * present, JSON {@code null} included, as an edit, so a {@code null} sent as JSON {@code null} would
 * replay a {@code null} body ({@code modifiedPayload}) or be rejected (the others).
 */
public final class ReplayDeliveryParams extends Params<ReplayDeliveryParams> {
    /**
     * Replay with this payload instead of the stored one: any JSON value (a {@code Map}, a list, a
     * Jackson {@code JsonNode}, ...). For a source that masks or encrypts fields, use
     * {@link #payloadPatch} instead: the payload you can read is the masked view. {@code null}
     * leaves the stored payload as it is.
     */
    public ReplayDeliveryParams modifiedPayload(Object modifiedPayload) {
        return setOrUnset("modifiedPayload", modifiedPayload);
    }

    /**
     * Replay with the stored payload changed by this JSON merge patch (RFC 7386): keys set to a
     * value replace or add it, keys set to {@code null} remove it, nested objects merge. Sends only
     * what changed, so it works on sources that mask or encrypt fields, though it may not touch an
     * encrypted field (422). The stored payload must be a JSON object (422 otherwise). {@code null}
     * sends no patch.
     */
    public ReplayDeliveryParams payloadPatch(Map<String, ?> payloadPatch) {
        return setOrUnset("payloadPatch", payloadPatch);
    }

    /**
     * Replay to this destination (id or slug, in the same organization) instead of the original.
     * {@code null} keeps the original destination.
     */
    public ReplayDeliveryParams destinationOverride(String destinationOverride) {
        return setOrUnset("destinationOverride", destinationOverride);
    }

    /**
     * Run this transform for the replay instead of the route's own. {@code null} keeps the route's
     * transform.
     */
    public ReplayDeliveryParams transformOverride(ReplayTransformOverride transformOverride) {
        return setOrUnset("transformOverride", transformOverride);
    }

    /**
     * Headers to add to, or replace on, the replayed request: at most 50. The headers Hookbase sets
     * itself (framing, signatures) cannot be overridden (400). {@code null} sends the original
     * headers.
     */
    public ReplayDeliveryParams headersOverride(Map<String, String> headersOverride) {
        return setOrUnset("headersOverride", headersOverride);
    }

    /**
     * Also save {@code transformOverride} as the route's transform. Needs {@code transformOverride},
     * a route that already has a transform, and permission to update transforms.
     */
    public ReplayDeliveryParams persistTransform(boolean persistTransform) {
        return set("persistTransform", persistTransform);
    }

    private ReplayDeliveryParams setOrUnset(String key, Object value) {
        return value == null ? unset(key) : set(key, value);
    }
}
