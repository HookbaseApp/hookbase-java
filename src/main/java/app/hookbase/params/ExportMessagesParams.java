package app.hookbase.params;

/**
 * Filters for {@code messages().export()} and {@code messages().exportCsv()}. The format is chosen
 * by which of the two you call.
 */
public final class ExportMessagesParams extends Params<ExportMessagesParams> {
    /** {@code events} (the API's default) or {@code messages}. */
    public ExportMessagesParams type(String type) {
        return set("type", type);
    }

    public ExportMessagesParams startDate(String startDate) {
        return set("startDate", startDate);
    }

    public ExportMessagesParams endDate(String endDate) {
        return set("endDate", endDate);
    }

    public ExportMessagesParams status(String status) {
        return set("status", status);
    }

    public ExportMessagesParams eventType(String eventType) {
        return set("eventType", eventType);
    }

    public ExportMessagesParams applicationId(String applicationId) {
        return set("applicationId", applicationId);
    }

    public ExportMessagesParams limit(int limit) {
        return set("limit", limit);
    }
}
