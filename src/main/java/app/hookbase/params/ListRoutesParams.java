package app.hookbase.params;

/** Filters for {@code routes().list()} and {@code listAll()}. */
public final class ListRoutesParams extends Params<ListRoutesParams> {
    /** Page number, from 1 (the default). On {@code listAll}, the page to start from. */
    public ListRoutesParams page(int page) {
        return set("page", page);
    }

    /** Items per page (default 20, max 100). */
    public ListRoutesParams pageSize(int pageSize) {
        return set("pageSize", pageSize);
    }

    public ListRoutesParams sourceId(String sourceId) {
        return set("sourceId", sourceId);
    }

    public ListRoutesParams destinationId(String destinationId) {
        return set("destinationId", destinationId);
    }

    public ListRoutesParams isActive(boolean isActive) {
        return set("isActive", isActive);
    }
}
