package app.hookbase;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Every item of a paginated list, fetched lazily one page at a time as it is iterated. Returned by
 * the {@code listAll} methods. Iterating it twice fetches the pages twice.
 *
 * <pre>{@code
 * for (Source source : client.sources().listAll()) { ... }
 * client.endpoints().listAll(params).stream().filter(e -> e.isDisabled()).toList();
 * }</pre>
 */
public final class Pager<T> implements Iterable<T> {

    /**
     * Makes, per iteration, a function that fetches the page after {@code previous} ({@code null}
     * for the first page). Per iteration so two iterations of the same Pager don't share a page
     * counter.
     */
    private final Supplier<Function<Page<T>, Page<T>>> fetcher;
    private final Predicate<Page<T>> isLast;

    private Pager(Supplier<Function<Page<T>, Page<T>>> fetcher, Predicate<Page<T>> isLast) {
        this.fetcher = fetcher;
        this.isLast = isLast;
    }

    /**
     * A page-numbered list: page 1, 2, … until {@code hasMore} is false (or a page comes back empty,
     * so a total that disagrees with the data can't loop forever).
     *
     * @param firstPage the page number to start at
     * @param fetchPage fetches page {@code n}
     */
    public static <T> Pager<T> numbered(int firstPage, Function<Integer, Page<T>> fetchPage) {
        return new Pager<>(
                () -> {
                    int[] next = {firstPage};
                    return previous -> fetchPage.apply(next[0]++);
                },
                page -> !page.hasMore() || page.data().isEmpty());
    }

    /**
     * A cursor list: follows {@code cursor} until {@code hasMore} is false or no cursor comes back
     * (a page that says there is more but gives no cursor is the last one reachable).
     *
     * @param startCursor the cursor to start from, or {@code null} for the beginning
     * @param fetchPage fetches the page at the given cursor ({@code null} for the first page)
     */
    public static <T> Pager<T> cursor(String startCursor, Function<String, Page<T>> fetchPage) {
        return new Pager<>(
                () -> previous -> fetchPage.apply(previous == null ? startCursor : previous.cursor()),
                page -> !page.hasMore() || page.cursor() == null);
    }

    @Override
    public Iterator<T> iterator() {
        Function<Page<T>, Page<T>> fetch = fetcher.get();
        return new Iterator<>() {
            private Page<T> page;
            private Iterator<T> items;
            private boolean done;

            @Override
            public boolean hasNext() {
                while (items == null || !items.hasNext()) {
                    if (done) return false;
                    page = fetch.apply(page);
                    items = page.data().iterator();
                    if (isLast.test(page)) done = true;
                }
                return true;
            }

            @Override
            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                return items.next();
            }
        };
    }

    /** A sequential stream over every item; pages are fetched as the stream is consumed. */
    public Stream<T> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED), false);
    }
}
