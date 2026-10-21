package app.hookbase.params;

/** Options for {@code importSources}, {@code importDestinations} and {@code importRoutes}. */
public final class ImportParams extends Params<ImportParams> {
    /**
     * What to do with an item whose slug (routes: name) already exists: {@code skip},
     * {@code rename} or {@code overwrite}. The API requires one; the SDK sends {@code skip} when
     * it is unset, as the Python SDK does.
     */
    public ImportParams conflictStrategy(String conflictStrategy) {
        return set("conflictStrategy", conflictStrategy);
    }

    /** Validate and report what would happen, without changing anything. */
    public ImportParams validateOnly(boolean validateOnly) {
        return set("validateOnly", validateOnly);
    }
}
