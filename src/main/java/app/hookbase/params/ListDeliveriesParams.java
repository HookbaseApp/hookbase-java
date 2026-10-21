package app.hookbase.params;

/** Filters for {@code deliveries().list()} / {@code listAll()}. */
public final class ListDeliveriesParams extends Params<ListDeliveriesParams> {
    /** Page size, at most 100 (default 50). */
    public ListDeliveriesParams limit(int limit) {
        return set("limit", limit);
    }

    /** Rows to skip. On {@code listAll} it is where iteration starts. */
    public ListDeliveriesParams offset(int offset) {
        return set("offset", offset);
    }

    public ListDeliveriesParams eventId(String eventId) {
        return set("eventId", eventId);
    }

    public ListDeliveriesParams routeId(String routeId) {
        return set("routeId", routeId);
    }

    public ListDeliveriesParams destinationId(String destinationId) {
        return set("destinationId", destinationId);
    }

    /** e.g. {@code pending}, {@code delivered}, {@code failed}, {@code retrying}. */
    public ListDeliveriesParams status(String status) {
        return set("status", status);
    }
}
