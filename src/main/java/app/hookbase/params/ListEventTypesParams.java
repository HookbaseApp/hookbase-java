package app.hookbase.params;

/** Filters for {@code eventTypes().list()}. */
public final class ListEventTypesParams extends Params<ListEventTypesParams> {
    /** Page size (default 50 on {@code list}, 100 on {@code listAll}). */
    public ListEventTypesParams limit(int limit) {
        return set("limit", limit);
    }

    /** The {@code cursor} from the previous page. */
    public ListEventTypesParams cursor(String cursor) {
        return set("cursor", cursor);
    }

    public ListEventTypesParams category(String category) {
        return set("category", category);
    }

    public ListEventTypesParams isEnabled(boolean isEnabled) {
        return set("isEnabled", isEnabled);
    }

    /** Substring match on the name. */
    public ListEventTypesParams search(String search) {
        return set("search", search);
    }
}
