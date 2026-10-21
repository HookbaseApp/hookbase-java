package app.hookbase;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PagerTest {

    @Test
    void numberedFollowsPagesUntilHasMoreIsFalse() {
        List<Integer> fetched = new ArrayList<>();
        Pager<String> pager = Pager.numbered(1, n -> {
            fetched.add(n);
            return new Page<>(List.of("p" + n), 3, 1, n - 1, n < 3, null);
        });
        assertThat(pager.stream().toList()).containsExactly("p1", "p2", "p3");
        assertThat(pager).containsExactly("p1", "p2", "p3"); // iterating again starts over
        assertThat(fetched).containsExactly(1, 2, 3, 1, 2, 3);
    }

    @Test
    void numberedStopsOnEmptyPage() {
        Pager<String> pager = Pager.numbered(1, n -> new Page<>(List.of(), 99, 20, 0, true, null));
        assertThat(pager).isEmpty();
    }

    @Test
    void cursorFollowsCursorAndStopsWithoutOne() {
        List<String> cursors = new ArrayList<>();
        Pager<String> pager = Pager.cursor("start", c -> {
            cursors.add(c);
            return switch (c) {
                case "start" -> new Page<>(List.of("a", "b"), 2, 2, 0, true, "c2");
                case "c2" -> new Page<>(List.of("c"), 1, 2, 0, true, null);
                default -> throw new AssertionError(c);
            };
        });
        assertThat(pager).containsExactly("a", "b", "c");
        assertThat(cursors).containsExactly("start", "c2");
    }

    @Test
    void lazyFetchesOnlyWhatIsConsumed() {
        List<Integer> fetched = new ArrayList<>();
        Pager<Integer> pager = Pager.numbered(1, n -> {
            fetched.add(n);
            return new Page<>(List.of(n), 100, 1, 0, true, null);
        });
        assertThat(pager.stream().limit(2).toList()).containsExactly(1, 2);
        assertThat(fetched).containsExactly(1, 2);
    }
}
