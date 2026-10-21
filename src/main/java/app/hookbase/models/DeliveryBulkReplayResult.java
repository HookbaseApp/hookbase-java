package app.hookbase.models;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * The result of {@code deliveries().bulkReplay()} / {@code bulkReplayEvents()}.
 *
 * @param eventsProcessed set by {@code bulkReplayEvents} only
 * @param overrides the overrides applied, set by {@code bulkReplayEvents} only
 */
public record DeliveryBulkReplayResult(
        String message,
        int queued,
        int skipped,
        Integer eventsProcessed,
        JsonNode overrides,
        List<DeliveryBulkReplayItem> results) {}
