package app.hookbase.models;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The analytics dashboard for a time range ({@code analytics().dashboard()}). The chart series are
 * left as JSON: their shapes follow the dashboard and change with it.
 *
 * @param timeline events and successful/failed deliveries per {@code interval}
 * @param latency latency percentiles
 * @param errors failures broken down by status code and error type
 * @param retries retry statistics
 * @param sources per-source performance
 * @param destinations per-destination performance
 * @param routes per-route performance
 * @param payloadSizes payload size distribution, {@code null} when unavailable
 * @param range the range that was used, e.g. {@code 7d}
 * @param interval the bucket size of the series, e.g. {@code hour} or {@code day}
 */
public record AnalyticsDashboard(
        AnalyticsOverviewStats overview,
        JsonNode timeline,
        JsonNode latency,
        JsonNode latencyDistribution,
        JsonNode latencyTrends,
        JsonNode errors,
        JsonNode retries,
        JsonNode sources,
        JsonNode destinations,
        JsonNode routes,
        JsonNode payloadSizes,
        String range,
        String interval) {}
