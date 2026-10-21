package app.hookbase.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Headline inbound counts. The API sends these keys in snake_case ({@code total_events}, …).
 *
 * @param avgLatency average delivery latency in ms; {@code null} when there were no deliveries
 * @param sourceCount {@code null} where the endpoint does not report it
 */
public record AnalyticsOverviewStats(
        @JsonProperty("total_events") long totalEvents,
        @JsonProperty("successful_deliveries") long successfulDeliveries,
        @JsonProperty("failed_deliveries") long failedDeliveries,
        @JsonProperty("avg_latency") Double avgLatency,
        @JsonProperty("source_count") Long sourceCount,
        @JsonProperty("destination_count") Long destinationCount) {}
