package app.hookbase.params;

import java.util.Map;

/** Body for {@code endpoints().test()}. Every field is optional; the API sends a sample event when none is given. */
public final class TestEndpointParams extends Params<TestEndpointParams> {
    public TestEndpointParams eventType(String eventType) {
        return set("eventType", eventType);
    }

    /** A JSON object to send as the test event's payload. */
    public TestEndpointParams payload(Map<String, ?> payload) {
        return set("payload", payload);
    }
}
