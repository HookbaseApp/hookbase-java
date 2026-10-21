package app.hookbase.models;

import app.hookbase.internal.Redact;

/**
 * One custom delivery header, in the shape the API stores and sends it ({@code {"name", "value"}}).
 * Endpoint inputs also accept a {@code Map<String, String>}, converted to this shape in insertion
 * order.
 */
public record EndpointHeader(String name, String value) {
    /** The value is printed as {@code <redacted>} when the name looks like a credential header. */
    @Override
    public String toString() {
        return "EndpointHeader[name=" + name + ", value="
                + (value != null && Redact.isSensitiveHeader(name) ? Redact.REDACTED : value) + "]";
    }
}
