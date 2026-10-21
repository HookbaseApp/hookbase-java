package app.hookbase.models;

import java.util.List;

/** The provider catalog, from {@code providers().list()}. Not paginated: every match comes back. */
public record ProviderCatalogList(List<CatalogProvider> providers, List<String> categories, int total) {
    public ProviderCatalogList {
        providers = providers == null ? List.of() : List.copyOf(providers);
        categories = categories == null ? List.of() : List.copyOf(categories);
    }
}
