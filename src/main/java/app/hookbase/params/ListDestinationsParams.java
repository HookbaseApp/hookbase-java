package app.hookbase.params;

/** Filters for {@code destinations().list()} and {@code listAll()}. */
public final class ListDestinationsParams extends Params<ListDestinationsParams> {
    /** Page number, from 1 (the default). On {@code listAll}, the page to start from. */
    public ListDestinationsParams page(int page) {
        return set("page", page);
    }

    /** Items per page (default 20, max 100). */
    public ListDestinationsParams pageSize(int pageSize) {
        return set("pageSize", pageSize);
    }

    /**
     * @deprecated The API does not filter destinations by this yet: the list comes back unfiltered, so
     *     filter the returned items yourself.
     */
    @Deprecated
    public ListDestinationsParams search(String search) {
        return set("search", search);
    }

    /**
     * @deprecated The API does not filter destinations by this yet: the list comes back unfiltered, so
     *     filter the returned items yourself.
     */
    @Deprecated
    public ListDestinationsParams isActive(boolean isActive) {
        return set("isActive", isActive);
    }
}
