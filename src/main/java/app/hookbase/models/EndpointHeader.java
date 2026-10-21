package app.hookbase.models;

/**
 * One custom delivery header, in the shape the API stores and sends it ({@code {"name", "value"}}).
 * Endpoint inputs also accept a {@code Map<String, String>}, converted to this shape in insertion
 * order.
 */
public record EndpointHeader(String name, String value) {}
