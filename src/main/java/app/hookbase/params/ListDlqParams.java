package app.hookbase.params;

/** Filters for {@code dlq().list()}. */
public final class ListDlqParams extends Params<ListDlqParams> {
    public ListDlqParams limit(int limit) {
        return set("limit", limit);
    }

    /** The {@code cursor} from the previous page. */
    public ListDlqParams cursor(String cursor) {
        return set("cursor", cursor);
    }

    public ListDlqParams endpointId(String endpointId) {
        return set("endpointId", endpointId);
    }

    public ListDlqParams applicationId(String applicationId) {
        return set("applicationId", applicationId);
    }

    /** The DLQ reason, e.g. {@code max_retries_exceeded}. */
    public ListDlqParams reason(String reason) {
        return set("reason", reason);
    }

    public ListDlqParams eventType(String eventType) {
        return set("eventType", eventType);
    }

    /** ISO timestamp; DLQ messages moved there at or after this. */
    public ListDlqParams startDate(String startDate) {
        return set("startDate", startDate);
    }

    /** ISO timestamp; DLQ messages moved there at or before this. */
    public ListDlqParams endDate(String endDate) {
        return set("endDate", endDate);
    }
}
