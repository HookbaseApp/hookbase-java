package app.hookbase.params;

/** Filters for {@code messages().list()}. The application comes from the call. */
public final class ListMessagesParams extends Params<ListMessagesParams> {
    /** Page size (default 50 on {@code list}, 100 on {@code listAll}). */
    public ListMessagesParams limit(int limit) {
        return set("limit", limit);
    }

    /** The {@code cursor} from the previous page. */
    public ListMessagesParams cursor(String cursor) {
        return set("cursor", cursor);
    }

    public ListMessagesParams endpointId(String endpointId) {
        return set("endpointId", endpointId);
    }

    /**
     * {@code pending}, {@code processing}, {@code success}, {@code failed}, {@code awaiting_retry},
     * {@code awaiting_pull}, {@code fifo_waiting}, {@code held}, {@code exhausted} or {@code dlq}
     * (see {@link app.hookbase.models.OutboundMessage#status()}).
     */
    public ListMessagesParams status(String status) {
        return set("status", status);
    }

    public ListMessagesParams eventType(String eventType) {
        return set("eventType", eventType);
    }
}
