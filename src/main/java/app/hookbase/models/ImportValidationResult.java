package app.hookbase.models;

import java.util.List;

/**
 * How one item of an import was (or would be) handled.
 *
 * @param index the item's position in the list sent
 * @param slug sources and destinations only
 * @param status {@code valid}, {@code error}, {@code skip}, {@code rename} or {@code overwrite}
 * @param resolvedSlug sources/destinations under the {@code rename} strategy: the slug it gets
 * @param resolvedName routes under the {@code rename} strategy: the name it gets
 */
public record ImportValidationResult(
        int index,
        String name,
        String slug,
        String status,
        List<String> errors,
        List<String> warnings,
        String resolvedSlug,
        String resolvedName) {

    public ImportValidationResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
