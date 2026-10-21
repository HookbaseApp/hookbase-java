package app.hookbase.models;

import java.util.List;
import java.util.Map;

/**
 * Dead letter queue statistics, from {@code dlq().getStats()}.
 *
 * @param byReason count per DLQ reason; a message with no reason is counted under {@code unknown}
 * @param byEndpoint the ten endpoints with the most DLQ messages, most first
 * @param byEventType the ten event types with the most DLQ messages, most first
 */
public record DlqStats(
        long total,
        Map<String, Long> byReason,
        List<DlqEndpointCount> byEndpoint,
        List<DlqEventTypeCount> byEventType) {
    public DlqStats {
        byReason = byReason == null ? Map.of() : byReason;
        byEndpoint = byEndpoint == null ? List.of() : List.copyOf(byEndpoint);
        byEventType = byEventType == null ? List.of() : List.copyOf(byEventType);
    }
}
