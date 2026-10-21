package app.hookbase.params;

/** Paging for {@code transforms().list()} / {@code listAll()}. */
public final class ListTransformsParams extends Params<ListTransformsParams> {
    /** 1-based page number (default 1). On {@code listAll} it is the page to start from. */
    public ListTransformsParams page(int page) {
        return set("page", page);
    }

    /** Page size, 1–100 (default 20). */
    public ListTransformsParams pageSize(int pageSize) {
        return set("pageSize", pageSize);
    }
}
