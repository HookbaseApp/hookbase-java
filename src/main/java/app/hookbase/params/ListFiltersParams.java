package app.hookbase.params;

/** Paging for {@code filters().list()} / {@code listAll()}. */
public final class ListFiltersParams extends Params<ListFiltersParams> {
    /** 1-based page number (default 1). On {@code listAll} it is the page to start from. */
    public ListFiltersParams page(int page) {
        return set("page", page);
    }

    /** Page size, 1–100 (default 20). */
    public ListFiltersParams pageSize(int pageSize) {
        return set("pageSize", pageSize);
    }
}
