package app.hookbase.internal;

import app.hookbase.errors.HookbaseException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Derives a destination slug from its name, byte for byte the way every other Hookbase SDK does
 * ({@code deriveDestinationSlug} in node-sdk {@code src/resources/wire.ts}, {@code _wire.py},
 * {@code slug_fold.go}, and the .NET {@code Destination.cs}). Internal: not part of the public API.
 *
 * <p>The steps: fold the {@link #isFoldAddition newer-Unicode ASCII decompositions}, NFKD, drop the
 * non-spacing marks the decomposition leaves behind, lower-case, turn each run of characters outside
 * {@code [a-z0-9]} into one hyphen, trim hyphens, cut at 50 and trim trailing hyphens again.
 *
 * <h2>Why the tables</h2>
 *
 * The answer to "is this a non-spacing mark" and "what does NFKD make of this" depends on the
 * Unicode version the runtime ships, and the SDKs' runtimes disagree (CPython 3.12 and Go 1.22 are
 * on Unicode 15.0, current Node on 17.0, JDK 25 on 16.0). Three tables pin the
 * fold to one answer, the reference's, regardless of runtime. Change them only together with the
 * same tables in the other four SDKs.
 *
 * <ul>
 *   <li>{@link #isMarkAddition MARK_ADDITIONS}: 75 code points that are {@code Mn} in the reference
 *       (Unicode 17) but unassigned in Unicode 15.0. They are stripped whatever this JDK's
 *       {@link Character#getType} says. JDK 25 is on Unicode 16.0: it already reports the 36
 *       Unicode 16 ones as {@code Mn} (U+0897, U+10D69-U+10D6D, U+10EFC, U+113BB..., U+11F5A,
 *       U+1611E..., U+1E5EE-U+1E5EF), so for those the table is a no-op, but the 39 Unicode 17 ones
 *       (U+1ACF-U+1ADD, U+1AE0-U+1AEB, U+10EFA-U+10EFB, U+11B60..., U+1E6E3...) are still
 *       unassigned here, and without the table they would become separators ({@code "a-b"}) where
 *       Node, Python, Go and .NET give {@code "ab"}. The table only ever adds to the strip set, so
 *       on a later JDK it stays harmless.
 *   <li>{@link #MARK_RECLASSIFIED}: U+1171E AHOM CONSONANT SIGN MEDIAL RA, {@code Mn} in Unicode
 *       15.0 and {@code Mc} (a spacing mark) from 15.1. The correction runs the other way: it is
 *       never stripped, so it separates. JDK 25 already reports it as
 *       {@link Character#COMBINING_SPACING_MARK}, so the check is a no-op here; it keeps a JDK on
 *       Unicode 15.0 (JDK 21) agreeing.
 *   <li>{@link #isFoldAddition FOLD_ADDITIONS}: U+A7F1 and U+1CCD6-U+1CCF9, which older Unicode
 *       leaves unassigned and the reference decomposes to ASCII. They are folded before NFKD, to
 *       exactly what the reference's NFKD gives. JDK 25 already decomposes the U+1CCD6 run (Unicode
 *       16), so that half is a no-op here; U+A7F1 is a Unicode 17 addition JDK 25 does not know, so
 *       without the table it would separate ({@code "a-b"}) instead of folding to {@code "asb"}.
 * </ul>
 */
public final class SlugFold {

    /** Max length of a destination slug, as the API's {@code createDestinationSchema} enforces it. */
    public static final int MAX_LENGTH = 50;

    /** AHOM CONSONANT SIGN MEDIAL RA: {@code Mc} since Unicode 15.1, so never stripped. */
    public static final int MARK_RECLASSIFIED = 0x1171E;

    /**
     * Inclusive ranges of the 75 marks the reference strips that Unicode 15.0 does not know. Same
     * list, same order as {@code SLUG_MARK_ADDITIONS} (Node), {@code _SLUG_MARK_ADDITIONS}
     * (Python), {@code slugMarkAdditions} (Go) and {@code SlugMarkAdditions} (.NET).
     */
    private static final int[][] MARK_ADDITIONS = {
        {0x0897, 0x0897},
        {0x1ACF, 0x1ADD},
        {0x1AE0, 0x1AEB},
        {0x10D69, 0x10D6D},
        {0x10EFA, 0x10EFC},
        {0x113BB, 0x113C0},
        {0x113CE, 0x113CE},
        {0x113D0, 0x113D0},
        {0x113D2, 0x113D2},
        {0x113E1, 0x113E2},
        {0x11B60, 0x11B60},
        {0x11B62, 0x11B64},
        {0x11B66, 0x11B66},
        {0x11F5A, 0x11F5A},
        {0x1611E, 0x16129},
        {0x1612D, 0x1612F},
        {0x1E5EE, 0x1E5EF},
        {0x1E6E3, 0x1E6E3},
        {0x1E6E6, 0x1E6E6},
        {0x1E6EE, 0x1E6EF},
        {0x1E6F5, 0x1E6F5},
    };

    /** U+A7F1 (Latin Extended-D) decomposes to {@code S} in the reference (a Unicode 17 addition). */
    private static final int FOLD_S = 0xA7F1;

    /**
     * U+1CCD6-U+1CCF9: 36 contiguous Unicode 16 additions in Symbols for Legacy Computing
     * Supplement, decomposing to A-Z and then 0-9 in order.
     */
    private static final int FOLD_RUN_START = 0x1CCD6;

    private static final String FOLD_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final Pattern SEPARATORS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("^-+|-+$");
    private static final Pattern TRAILING_HYPHENS = Pattern.compile("-+$");

    private SlugFold() {}

    /**
     * A slug matching {@code ^[a-z0-9-]+$}, at most 50 characters, derived from a destination
     * name. Accents are folded first, so {@code "Café EU"} gives {@code "cafe-eu"}.
     *
     * @throws HookbaseException if the name has no alphanumeric characters to derive from
     */
    public static String deriveDestinationSlug(String name) {
        String source = name == null ? "" : name;
        String decomposed = Normalizer.normalize(foldAdditions(source), Normalizer.Form.NFKD);

        StringBuilder stripped = new StringBuilder(decomposed.length());
        decomposed.codePoints().filter(cp -> !isSlugMark(cp)).forEach(stripped::appendCodePoint);

        String slug = stripped.toString().toLowerCase(Locale.ROOT);
        slug = SEPARATORS.matcher(slug).replaceAll("-");
        slug = EDGE_HYPHENS.matcher(slug).replaceAll("");
        // Only [a-z0-9-] is left, so a char cut is a code-point cut.
        if (slug.length() > MAX_LENGTH) slug = slug.substring(0, MAX_LENGTH);
        // Truncation can land on a separator and leave a trailing hyphen behind.
        slug = TRAILING_HYPHENS.matcher(slug).replaceAll("");

        if (slug.isEmpty()) {
            throw new HookbaseException("Cannot derive a destination slug from name " + Json.write(name)
                    + ": pass `slug` explicitly, as a string matching ^[a-z0-9-]+$ (max 50 characters).");
        }
        return slug;
    }

    /** Whether {@code codePoint} is a combining mark the fold drops (the category plus the tables). */
    public static boolean isSlugMark(int codePoint) {
        if (codePoint == MARK_RECLASSIFIED) return false;
        return Character.getType(codePoint) == Character.NON_SPACING_MARK || isMarkAddition(codePoint);
    }

    /** Whether {@code codePoint} is in the 75-mark additions table itself (whatever this JDK knows). */
    public static boolean isMarkAddition(int codePoint) {
        for (int[] range : MARK_ADDITIONS) {
            if (codePoint >= range[0] && codePoint <= range[1]) return true;
        }
        return false;
    }

    /** Whether {@code codePoint} is one of the 37 characters folded to ASCII before NFKD. */
    public static boolean isFoldAddition(int codePoint) {
        return codePoint == FOLD_S
                || (codePoint >= FOLD_RUN_START && codePoint < FOLD_RUN_START + FOLD_ALPHABET.length());
    }

    private static String foldAdditions(String name) {
        StringBuilder out = new StringBuilder(name.length());
        name.codePoints().forEach(cp -> {
            if (cp == FOLD_S) {
                out.append('S');
            } else if (isFoldAddition(cp)) {
                out.append(FOLD_ALPHABET.charAt(cp - FOLD_RUN_START));
            } else {
                out.appendCodePoint(cp);
            }
        });
        return out.toString();
    }
}
