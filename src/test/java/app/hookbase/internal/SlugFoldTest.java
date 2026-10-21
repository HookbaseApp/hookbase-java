package app.hookbase.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.hookbase.errors.HookbaseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The cross-SDK slug contract. Node, Python, Go and .NET carry this exact table, so a change to any
 * one implementation fails here rather than quietly deriving a different slug from the same
 * destination name (and a slug cannot be changed after creation). Keep it identical to:
 *
 * <ul>
 *   <li>node-sdk/src/__tests__/wire-format.test.ts (cross-SDK contract block)
 *   <li>python-sdk/tests/test_slug_contract.py (CROSS_SDK_SLUG_CASES)
 *   <li>go-sdk/destinations_fields_test.go (crossSDKSlugCases)
 *   <li>dotnet-sdk/tests/Hookbase.Tests/DestinationRequestSerializationTests.cs (CrossSdkSlugCases)
 * </ul>
 *
 * Æ/Ø/Đ/Ł do not decompose under NFKD and so are dropped rather than folded ({@code Ærø Ømega} ->
 * {@code r-mega}); that is agreed-upon behaviour, not an accident to fix in one SDK alone.
 */
class SlugFoldTest {

    static Stream<Arguments> crossSdkCases() {
        return Stream.of(
                Arguments.of("Café EU", "cafe-eu"),
                Arguments.of("Acme Orders (EU)", "acme-orders-eu"),
                Arguments.of("My Backend (EU)", "my-backend-eu"),
                Arguments.of("  spaced  out  ", "spaced-out"),
                Arguments.of("UPPER CASE", "upper-case"),
                Arguments.of("ünïcödé nämes", "unicode-names"),
                Arguments.of("a-very-long-destination-name-that-runs-well-past-the-fifty-character-limit",
                        "a-very-long-destination-name-that-runs-well-past-t"),
                Arguments.of("trailing---hyphens---", "trailing-hyphens"),
                Arguments.of("123 numeric", "123-numeric"),
                Arguments.of("Ærø Ømega", "r-mega"),
                Arguments.of("ﬁle ligature", "file-ligature"),
                Arguments.of("Mixed 123 ABC xyz", "mixed-123-abc-xyz"),
                Arguments.of("don't stop", "don-t-stop"),
                Arguments.of("a", "a"),
                // A combining mark outside the U+0300-U+036F block: the strip is the whole Mn category.
                Arguments.of("aٍb", "ab"),
                Arguments.of("हिन्दी name", "name"),
                Arguments.of("אָ hebrew", "hebrew"),
                // Not marks themselves (Lm) but their NFKD is one, so decomposing before stripping
                // makes them vanish rather than separate.
                Arguments.of("aﾞb", "ab"),
                Arguments.of("aﾟb", "ab"),
                // Mn only in newer Unicode (17; unassigned on JDK 25): stripped via the additions table.
                Arguments.of("a᫏b", "ab"),
                // The same, outside the BMP (Mn from Unicode 16; .NET once read it as two surrogates).
                Arguments.of("a𞗮b", "ab"),
                // The other direction: Mn in Unicode 15.0 and Mc from 15.1. A spacing mark separates.
                Arguments.of("a𑜞b", "a-b"),
                // Unassigned before Unicode 16, where it decomposes to "A".
                Arguments.of("a𜳖b", "aab"),
                // A noncharacter: separates, and must not throw.
                Arguments.of("a￾b", "a-b"));
    }

    @ParameterizedTest
    @MethodSource("crossSdkCases")
    void crossSdkContract(String name, String expected) {
        String slug = SlugFold.deriveDestinationSlug(name);
        assertThat(slug).isEqualTo(expected);
        assertThat(slug).matches("[a-z0-9-]+").hasSizeLessThanOrEqualTo(50);
    }

    @Test
    void aNameWithNoAlphanumericsThrowsNamingTheFieldToSet() {
        assertThatThrownBy(() -> SlugFold.deriveDestinationSlug("☃☃☃"))
                .isExactlyInstanceOf(HookbaseException.class)
                .hasMessage("Cannot derive a destination slug from name \"☃☃☃\": pass `slug` explicitly, "
                        + "as a string matching ^[a-z0-9-]+$ (max 50 characters).");
        assertThatThrownBy(() -> SlugFold.deriveDestinationSlug("→→→")).hasMessageContaining("pass `slug`");
        assertThatThrownBy(() -> SlugFold.deriveDestinationSlug("")).isInstanceOf(HookbaseException.class);
    }

    /** node wire-format.test.ts "derives %j as %j" and go TestCreateDestinationDerivesSlugFromName. */
    @ParameterizedTest
    @MethodSource("deriveCases")
    void derivesFromNames(String name, String expected) {
        assertThat(SlugFold.deriveDestinationSlug(name)).isEqualTo(expected);
    }

    static Stream<Arguments> deriveCases() {
        return Stream.of(
                Arguments.of("Acme Orders", "acme-orders"),
                Arguments.of("  Trailing and leading  ", "trailing-and-leading"),
                Arguments.of("Multiple   ---   separators", "multiple-separators"),
                Arguments.of("UPPER_snake.case", "upper-snake-case"),
                Arguments.of("v2 API 2026", "v2-api-2026"),
                Arguments.of("already-a-slug", "already-a-slug"),
                Arguments.of("Acme Orders (EU) — Production!", "acme-orders-eu-production"),
                Arguments.of("S3 Archive", "s3-archive"),
                Arguments.of("Order__Events!!", "order-events"),
                Arguments.of("Stripe → BigQuery", "stripe-bigquery"),
                Arguments.of("Ünïcödé Sink", "unicode-sink"),
                Arguments.of("v2.1 / prod (eu-west)", "v2-1-prod-eu-west"),
                Arguments.of("A very long destination name that runs past the fifty character limit",
                        "a-very-long-destination-name-that-runs-past-the-fi"),
                Arguments.of("Hyphen-ends-here---", "hyphen-ends-here"));
    }

    @Test
    void truncationIsAPlainCutAt50ThatLeavesNoTrailingHyphen() {
        String cut = SlugFold.deriveDestinationSlug(
                "A very long destination name that runs well past the fifty character limit");
        assertThat(cut).isEqualTo("a-very-long-destination-name-that-runs-well-past-t").hasSize(50);
        // 49 characters of slug, so the 50th character the cut keeps is a separator.
        String onSeparator = SlugFold.deriveDestinationSlug("Webhook destination for the acme europe orders eu queue");
        assertThat(onSeparator).isEqualTo("webhook-destination-for-the-acme-europe-orders-eu").doesNotEndWith("-");
    }

    /**
     * The 75 marks the reference strips that Unicode 15.0 does not know, written out independently
     * of SlugFold's table so a typo in one range is a failure. Same list, same order as the other
     * SDKs' tables.
     */
    private static final String NEWER_UNICODE_MARKS =
            "0897 1ACF-1ADD 1AE0-1AEB 10D69-10D6D 10EFA-10EFC 113BB-113C0 113CE 113D0 113D2 "
                    + "113E1-113E2 11B60 11B62-11B64 11B66 11F5A 1611E-16129 1612D-1612F 1E5EE-1E5EF 1E6E3 "
                    + "1E6E6 1E6EE-1E6EF 1E6F5";

    private static List<Integer> newerUnicodeMarks() {
        List<Integer> marks = new ArrayList<>();
        for (String span : NEWER_UNICODE_MARKS.split(" ")) {
            String[] bounds = span.split("-");
            int low = Integer.parseInt(bounds[0], 16);
            int high = Integer.parseInt(bounds[bounds.length - 1], 16);
            for (int cp = low; cp <= high; cp++) marks.add(cp);
        }
        return marks;
    }

    @Test
    void stripsMarksANewerUnicodeAddedWhateverThisJdkKnows() {
        List<Integer> marks = newerUnicodeMarks();
        assertThat(marks).hasSize(75);
        for (int cp : marks) {
            assertThat(SlugFold.deriveDestinationSlug("a" + Character.toString(cp) + "b"))
                    .as("U+%04X", cp).isEqualTo("ab");
        }
    }

    /**
     * The test above passes on a JDK whose own Mn covers a mark whether or not the table is right,
     * so this one holds the table itself to its contents.
     */
    @Test
    void theAdditionsTableCoversEveryNewerMarkAndNothingElse() {
        List<Integer> marks = newerUnicodeMarks();
        for (int cp : marks) {
            assertThat(SlugFold.isMarkAddition(cp)).as("U+%04X missing from MARK_ADDITIONS", cp).isTrue();
        }
        int inTable = 0;
        for (int cp = 0; cp <= Character.MAX_CODE_POINT; cp++) {
            if (SlugFold.isMarkAddition(cp)) inTable++;
        }
        assertThat(inTable).isEqualTo(75);
        // A widened range would strip a character the other SDKs keep.
        assertThat(SlugFold.isMarkAddition(SlugFold.MARK_RECLASSIFIED)).isFalse();
        assertThat(SlugFold.isMarkAddition(0x1ADE)).isFalse();
        assertThat(SlugFold.isMarkAddition('a')).isFalse();
    }

    @Test
    void theReclassifiedAhomSignIsNeverAMark() {
        assertThat(SlugFold.isSlugMark(SlugFold.MARK_RECLASSIFIED)).isFalse();
        assertThat(SlugFold.deriveDestinationSlug("a" + Character.toString(SlugFold.MARK_RECLASSIFIED) + "b"))
                .isEqualTo("a-b");
        assertThat(SlugFold.isSlugMark(0x0300)).isTrue();
    }

    @Test
    void foldsTheCharactersANewerUnicodeDecomposesToAscii() {
        assertThat(SlugFold.deriveDestinationSlug("a꟱b")).isEqualTo("asb");
        String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
        for (int offset = 0; offset < alphabet.length(); offset++) {
            int cp = 0x1CCD6 + offset;
            assertThat(SlugFold.isFoldAddition(cp)).isTrue();
            assertThat(SlugFold.deriveDestinationSlug("a" + Character.toString(cp) + "b"))
                    .as("U+%04X", cp).isEqualTo("a" + alphabet.charAt(offset) + "b");
        }
        assertThat(SlugFold.isFoldAddition(0x1CCD5)).isFalse();
        assertThat(SlugFold.isFoldAddition(0x1CCFA)).isFalse();
    }
}
