package app.hookbase.models;

/**
 * The destination an {@link EventDebugDelivery} went to.
 *
 * @param url empty for queue, warehouse and bridge destinations
 * @param method the HTTP method, for {@code http} destinations
 */
public record EventDebugDestination(String name, String url, String method) {}
