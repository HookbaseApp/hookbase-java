package app.hookbase.params;

/** Filters for {@code providers().list()}. */
public final class ListProvidersParams extends Params<ListProvidersParams> {
    public ListProvidersParams category(String category) {
        return set("category", category);
    }

    /** Matched against name, description and category. */
    public ListProvidersParams search(String search) {
        return set("search", search);
    }
}
