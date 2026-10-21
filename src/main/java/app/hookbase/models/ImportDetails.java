package app.hookbase.models;

import java.util.List;

/** Per-item outcome of an applied source or destination import, by name. */
public record ImportDetails(List<String> imported, List<String> skipped, List<String> overwritten, List<ImportFailure> failed) {
    public ImportDetails {
        imported = imported == null ? List.of() : List.copyOf(imported);
        skipped = skipped == null ? List.of() : List.copyOf(skipped);
        overwritten = overwritten == null ? List.of() : List.copyOf(overwritten);
        failed = failed == null ? List.of() : List.copyOf(failed);
    }
}
