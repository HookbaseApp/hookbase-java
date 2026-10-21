package app.hookbase.params;

/** Filters for {@code endpoints().list()}. The application comes from the call. */
public final class ListEndpointsParams extends Params<ListEndpointsParams> {
    /** Page size (default 50 on {@code list}, 100 on {@code listAll}). */
    public ListEndpointsParams limit(int limit) {
        return set("limit", limit);
    }

    /** The {@code cursor} from the previous page. */
    public ListEndpointsParams cursor(String cursor) {
        return set("cursor", cursor);
    }

    /** Only endpoints whose circuit is {@code closed}, {@code open} or {@code half_open}. */
    public ListEndpointsParams circuitState(String circuitState) {
        return set("circuitState", circuitState);
    }

    public ListEndpointsParams isDisabled(boolean isDisabled) {
        return set("isDisabled", isDisabled);
    }
}
