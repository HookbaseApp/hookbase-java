package app.hookbase.resources;

import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.models.AnalyticsDashboard;
import app.hookbase.models.AnalyticsOverviewStats;
import app.hookbase.params.AnalyticsDashboardParams;
import app.hookbase.testing.MockServer;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnalyticsResourceTest {

    private MockServer server;
    private AnalyticsResource analytics;

    @BeforeEach
    void start() {
        server = new MockServer();
        analytics = new AnalyticsResource(DataAdminTestSupport.transport(server));
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void dashboardSendsRangeAndBindsBareResponse() {
        server.enqueue(200, """
                {"overview":{"total_events":100,"successful_deliveries":90,"failed_deliveries":10,"avg_latency":12.5},
                 "timeline":[{"period":"2026-01-01","events":5,"successful":4,"failed":1}],"latency":{"p50":10},
                 "latencyDistribution":[],"latencyTrends":[],"errors":{},"retries":{},"sources":[],"destinations":[],
                 "routes":[],"payloadSizes":null,"range":"24h","interval":"hour"}""");
        AnalyticsDashboard d = analytics.dashboard(new AnalyticsDashboardParams().range("24h").sourceId("src_1"));
        assertThat(server.last().path()).isEqualTo("/api/analytics/dashboard");
        assertThat(server.last().query()).containsExactlyInAnyOrderEntriesOf(Map.of("range", "24h", "sourceId", "src_1"));
        assertThat(d.overview().totalEvents()).isEqualTo(100);
        assertThat(d.overview().avgLatency()).isEqualTo(12.5);
        assertThat(d.overview().sourceCount()).isNull();
        assertThat(d.timeline().get(0).get("failed").asInt()).isEqualTo(1);
        assertThat(d.interval()).isEqualTo("hour");
        assertThat(d.payloadSizes().isNull()).isTrue();

        server.enqueue(200, "{\"data\":{\"range\":\"7d\"}}");
        assertThat(analytics.dashboard().range()).isEqualTo("7d");
        assertThat(server.last().query()).isEmpty();
    }

    @Test
    void overviewUnwrapsStats() {
        server.enqueue(200, "{\"stats\":{\"total_events\":7,\"successful_deliveries\":6,\"failed_deliveries\":1,"
                + "\"avg_latency\":null,\"source_count\":2,\"destination_count\":3}}");
        AnalyticsOverviewStats s = analytics.overview();
        assertThat(server.last().path()).isEqualTo("/api/analytics/overview");
        assertThat(s.totalEvents()).isEqualTo(7);
        assertThat(s.avgLatency()).isNull();
        assertThat(s.destinationCount()).isEqualTo(3L);
    }
}
