package app.hookbase;

import java.util.List;

/**
 * One page of a list, in the same shape for both kinds of pagination the API uses (Node's
 * {@code PaginatedResponse} / {@code CursorPaginatedResponse}).
 *
 * <ul>
 *   <li><b>Page-numbered</b> lists (sources, destinations, routes, events, deliveries, transforms,
 *       filters, schemas, …): {@code total} is the size of the whole result set, {@code limit} the
 *       page size, {@code offset} where this page starts, and {@code cursor} is {@code null}.
 *   <li><b>Cursor</b> lists (applications, endpoints, event types, subscriptions, messages, DLQ):
 *       {@code cursor} is the value to pass as the next request's {@code cursor}, {@code null} once
 *       {@code hasMore} is false; {@code total} is the size of this page and {@code offset} is 0.
 * </ul>
 *
 * <p>Every list method has a {@code listAll} counterpart that follows the pages for you.
 */
public record Page<T>(List<T> data, int total, int limit, int offset, boolean hasMore, String cursor) {
    public Page {
        data = data == null ? List.of() : List.copyOf(data);
    }
}
