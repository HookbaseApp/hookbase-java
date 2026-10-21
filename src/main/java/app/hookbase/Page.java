package app.hookbase;

import java.util.List;

/**
 * One page of a list, in the same shape for the three kinds of pagination the API uses.
 *
 * <ul>
 *   <li><b>Page-numbered</b> lists (sources, destinations, routes, transforms, filters): {@code
 *       total} is the size of the whole result set, {@code limit} the page size, {@code offset}
 *       where this page starts, and {@code cursor} is {@code null}.
 *   <li><b>Offset</b> lists (deliveries): the API reports no total, so {@code total} is the size of
 *       this page; {@code offset} is where it starts and {@code hasMore} means the page was full.
 *   <li><b>Cursor</b> lists (applications, endpoints, event types, subscriptions, messages, DLQ,
 *       events): {@code cursor} is the value to pass as the next request's {@code cursor}, {@code
 *       null} once {@code hasMore} is false, and {@code offset} is 0. {@code total} is the size of
 *       this page, except for events listed with {@code includeTotal}, where it is the whole count.
 * </ul>
 *
 * <p>Every paginated list method has a {@code listAll} counterpart that follows the pages for you.
 * Lists the API returns whole (schemas, API keys, tunnels, cron jobs) return a plain {@code List}.
 */
public record Page<T>(List<T> data, int total, int limit, int offset, boolean hasMore, String cursor) {
    public Page {
        data = data == null ? List.of() : List.copyOf(data);
    }
}
