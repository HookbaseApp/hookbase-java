package app.hookbase.models;

import app.hookbase.internal.Redact;
import java.util.Map;

/** The inbound request behind an event: its headers, payload size in bytes and content type. */
public record EventDebugRequest(Map<String, String> headers, Long payloadSize, String contentType) {
    /** Credentials are printed as {@code <redacted>}, so logging this record is safe. */
    @Override
    public String toString() {
        return Redact.toString(this, Map.of("headers", Redact.Rule.HEADERS));
    }
}
