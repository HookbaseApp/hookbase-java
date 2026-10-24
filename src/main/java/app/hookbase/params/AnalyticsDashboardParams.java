package app.hookbase.params;

/** Query for {@code analytics().dashboard()}. All optional. */
public final class AnalyticsDashboardParams extends Params<AnalyticsDashboardParams> {
    /** e.g. {@code 24h}, {@code 7d} (default), {@code 30d}; ignored when start and end dates are given. */
    public AnalyticsDashboardParams range(String range) {
        return set("range", range);
    }

    public AnalyticsDashboardParams startDate(String startDate) {
        return set("startDate", startDate);
    }

    public AnalyticsDashboardParams endDate(String endDate) {
        return set("endDate", endDate);
    }

    /** Scope the dashboard to one source. */
    public AnalyticsDashboardParams sourceId(String sourceId) {
        return set("sourceId", sourceId);
    }

    /**
     * IANA time zone the timeline is bucketed in, such as {@code Europe/Berlin} (sent as {@code
     * tz}). Default UTC; a name the API doesn't recognise falls back to UTC.
     */
    public AnalyticsDashboardParams timeZone(String timeZone) {
        return set("tz", timeZone);
    }

    /** Scope the dashboard to one destination. */
    public AnalyticsDashboardParams destinationId(String destinationId) {
        return set("destinationId", destinationId);
    }
}
