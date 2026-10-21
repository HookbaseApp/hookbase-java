package app.hookbase.models;

/**
 * Counts from an import. Which ones are set depends on the resource and on {@code validateOnly};
 * see {@link ImportResult}. Unset counts are {@code null}.
 *
 * @param failed sources/destinations: items that failed
 * @param errors validate-only, and route imports: items with errors
 * @param valid validate-only route imports: items that would be created
 * @param overwrite validate-only route imports: items that would be overwritten
 */
public record ImportSummary(
        Integer total,
        Integer imported,
        Integer skipped,
        Integer overwritten,
        Integer failed,
        Integer errors,
        Integer toCreate,
        Integer toOverwrite,
        Integer toSkip,
        Integer valid,
        Integer overwrite) {}
