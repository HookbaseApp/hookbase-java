package app.hookbase.models;

/**
 * Delivery statistics of one endpoint, computed from its counters by {@code endpoints().getStats()}.
 *
 * @param successRate successes as a percentage of all messages (0-100); 0 when there are none
 * @param averageLatency the endpoint's average response time in milliseconds; 0 when unknown
 * @param recentFailures always 0: the API has no such counter
 */
public record EndpointStats(
        long totalMessages,
        long totalSuccesses,
        long totalFailures,
        double successRate,
        double averageLatency,
        int recentFailures) {}
