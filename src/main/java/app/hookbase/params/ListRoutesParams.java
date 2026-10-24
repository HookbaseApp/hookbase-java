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

    /**
     * @deprecated The API does not filter routes by this yet: the list comes back unfiltered, so
     *     filter the returned items yourself.
     */
    @Deprecated
    public ListRoutesParams sourceId(String sourceId) {
        return set("sourceId", sourceId);
    }

    /**
     * @deprecated The API does not filter routes by this yet: the list comes back unfiltered, so
     *     filter the returned items yourself.
     */
    @Deprecated
    public ListRoutesParams destinationId(String destinationId) {
        return set("destinationId", destinationId);
    }

    /**
     * @deprecated The API does not filter routes by this yet: the list comes back unfiltered, so
     *     filter the returned items yourself.
     */
    @Deprecated
    public ListRoutesParams isActive(boolean isActive) {
        return set("isActive", isActive);
    }
}
