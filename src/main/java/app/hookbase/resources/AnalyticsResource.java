package app.hookbase.resources;

import app.hookbase.RequestOptions;
import app.hookbase.internal.HttpTransport;
import app.hookbase.models.AnalyticsDashboard;
import app.hookbase.models.AnalyticsOverviewStats;
import app.hookbase.params.AnalyticsDashboardParams;

/** Inbound analytics: the dashboard series and all-time headline counts. */
public final class AnalyticsResource extends BaseResource {

    private static final String PATH = "/api/analytics";

    public AnalyticsResource(HttpTransport http) {
        super(http);
    }

    /** The analytics dashboard for a range (default the last 7 days). */
    public AnalyticsDashboard dashboard(AnalyticsDashboardParams params, RequestOptions options) {
        return unwrap(get(PATH + "/dashboard", query(params), options), "data", AnalyticsDashboard.class);
    }

    public AnalyticsDashboard dashboard(AnalyticsDashboardParams params) {
        return dashboard(params, null);
    }

    public AnalyticsDashboard dashboard() {
        return dashboard(null, null);
    }

    /** All-time headline counts for the organization. */
    public AnalyticsOverviewStats overview(RequestOptions options) {
        return unwrap(get(PATH + "/overview", null, options), "stats", AnalyticsOverviewStats.class);
    }

    public AnalyticsOverviewStats overview() {
        return overview(null);
    }
}
