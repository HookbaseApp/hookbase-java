package app.hookbase.models;

import java.util.List;

/**
 * Result of {@code importSources}, {@code importDestinations} or {@code importRoutes}.
 *
 * <p>The API answers in a few shapes, all bound here; a field a given call does not send is
 * {@code null} (or an empty list):
 *
 * <ul>
 *   <li>sources and destinations, applied: {@code success}, {@code summary} ({@code imported},
 *       {@code skipped}, {@code overwritten}, {@code failed}) and {@code details} (the names, and
 *       the failures with their error);
 *   <li>sources and destinations with {@code validateOnly}: {@code valid}, {@code validationResults}
 *       and {@code summary} ({@code total}, {@code toCreate}, {@code toOverwrite}, {@code toSkip},
 *       {@code errors});
 *   <li>routes, applied: {@code success}, top-level {@code imported} / {@code skipped} (names) and
 *       {@code errors} (failures), and {@code summary} ({@code total}, {@code imported},
 *       {@code skipped}, {@code errors});
 *   <li>routes with {@code validateOnly}: {@code success}, {@code validateOnly}, {@code
 *       validationResults} and {@code summary} ({@code total}, {@code valid}, {@code skipped},
 *       {@code overwrite}, {@code errors}).
 * </ul>
 *
 * @param valid validate-only source/destination imports: whether no item has status {@code error}
 * @param validateOnly true on a validate-only route import
 * @param imported route imports: names of the routes created
 * @param skipped route imports: names of the routes skipped
 * @param errors route imports: the routes that failed
 */
public record ImportResult(
        boolean success,
        Boolean valid,
        boolean validateOnly,
        ImportSummary summary,
        ImportDetails details,
        List<ImportValidationResult> validationResults,
        List<String> imported,
        List<String> skipped,
        List<ImportFailure> errors) {

    public ImportResult {
        validationResults = validationResults == null ? List.of() : List.copyOf(validationResults);
        imported = imported == null ? List.of() : List.copyOf(imported);
        skipped = skipped == null ? List.of() : List.copyOf(skipped);
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
