package app.hookbase.params;

/** Filters for {@code sources().list()} and {@code listAll()}. */
public final class ListSourcesParams extends Params<ListSourcesParams> {
    /** Page number, from 1 (the default). On {@code listAll}, the page to start from. */
    public ListSourcesParams page(int page) {
        return set("page", page);
    }

    /** Items per page (default 20, max 100). */
    public ListSourcesParams pageSize(int pageSize) {
        return set("pageSize", pageSize);
    }

    /**
     * @deprecated The API does not filter sources by this yet: the list comes back unfiltered, so
     *     filter the returned items yourself.
     */
    @Deprecated
    public ListSourcesParams search(String search) {
        return set("search", search);
    }

    /**
     * @deprecated The API does not filter sources by this yet: the list comes back unfiltered, so
     *     filter the returned items yourself.
     */
    @Deprecated
    public ListSourcesParams provider(String provider) {
        return set("provider", provider);
    }

    /**
     * @deprecated The API does not filter sources by this yet: the list comes back unfiltered, so
     *     filter the returned items yourself.
     */
    @Deprecated
    public ListSourcesParams isActive(boolean isActive) {
        return set("isActive", isActive);
    }
}
