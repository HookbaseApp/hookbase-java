package app.hookbase.params;

/** Filters for {@code applications().list()}. */
public final class ListApplicationsParams extends Params<ListApplicationsParams> {
    /** Page size (default 50 on {@code list}, 100 on {@code listAll}). */
    public ListApplicationsParams limit(int limit) {
        return set("limit", limit);
    }

    /** The {@code cursor} from the previous page. */
    public ListApplicationsParams cursor(String cursor) {
        return set("cursor", cursor);
    }

    /** Match on name or external id. */
    public ListApplicationsParams search(String search) {
        return set("search", search);
    }

    /** Only disabled ({@code true}) or only enabled ({@code false}) applications. */
    public ListApplicationsParams isDisabled(boolean isDisabled) {
        return set("isDisabled", isDisabled);
    }

    /** {@code desc} (newest first, the default) or {@code asc}. */
    public ListApplicationsParams order(String order) {
        return set("order", order);
    }
}
