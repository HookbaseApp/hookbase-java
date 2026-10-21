package app.hookbase.params;

/** Filters for {@code events().list()} / {@code listAll()}. */
public final class ListEventsParams extends Params<ListEventsParams> {
    /** Page size, at most 100 (default 50 on {@code list}, 100 on {@code listAll}). */
    public ListEventsParams limit(int limit) {
        return set("limit", limit);
    }

    /** The {@code cursor} from the previous page. */
    public ListEventsParams cursor(String cursor) {
        return set("cursor", cursor);
    }

    /** A source id or slug. One that matches no source is a 404, not an empty list. */
    public ListEventsParams sourceId(String sourceId) {
        return set("sourceId", sourceId);
    }

    public ListEventsParams eventType(String eventType) {
        return set("eventType", eventType);
    }

    public ListEventsParams search(String search) {
        return set("search", search);
    }

    /** ISO date or timestamp. */
    public ListEventsParams fromDate(String fromDate) {
        return set("fromDate", fromDate);
    }

    /** ISO date (inclusive of the whole day) or timestamp. */
    public ListEventsParams toDate(String toDate) {
        return set("toDate", toDate);
    }

    /** Only events whose signature did (true) or did not (false) verify. */
    public ListEventsParams signatureValid(boolean signatureValid) {
        return set("signatureValid", signatureValid ? "1" : "0");
    }

    /** {@code delivered}, {@code failed}, {@code pending}, {@code partial}, {@code duplicate} or {@code no_routes}. */
    public ListEventsParams status(String status) {
        return set("status", status);
    }

    /**
     * Also count every matching event into {@link app.hookbase.Page#total()}. Off by default: the
     * count is expensive on large accounts. Has no effect together with {@code status}.
     */
    public ListEventsParams includeTotal(boolean includeTotal) {
        return includeTotal ? set("includeTotal", "1") : unset("includeTotal");
    }
}
