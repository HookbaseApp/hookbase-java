package app.hookbase.resources;

import app.hookbase.RequestOptions;
import app.hookbase.internal.HttpTransport;
import app.hookbase.models.CatalogProviderDetail;
import app.hookbase.models.ProviderCatalogList;
import app.hookbase.models.ProviderEvents;
import app.hookbase.params.ListProvidersParams;

/**
 * The public provider catalog: what each provider signs with and which secret a source for it
 * needs. The API requires no authentication here, though the client's key is still sent.
 */
public final class ProvidersResource extends BaseResource {

    private static final String PATH = "/api/catalog/providers";

    public ProvidersResource(HttpTransport http) {
        super(http);
    }

    /** Catalog providers, optionally narrowed by category or a name/description search. Not paginated. */
    public ProviderCatalogList list(ListProvidersParams params, RequestOptions options) {
        return as(get(PATH, query(params), options), ProviderCatalogList.class);
    }

    public ProviderCatalogList list(ListProvidersParams params) {
        return list(params, null);
    }

    public ProviderCatalogList list() {
        return list(null, null);
    }

    /** One provider in full, by catalog id. 404 for an unknown id. */
    public CatalogProviderDetail get(String id, RequestOptions options) {
        return unwrap(get(PATH + "/" + seg(id), null, options), "provider", CatalogProviderDetail.class);
    }

    public CatalogProviderDetail get(String id) {
        return get(id, null);
    }

    /** A provider's event types and sample payloads. 404 for an unknown id. */
    public ProviderEvents events(String id, RequestOptions options) {
        return as(get(PATH + "/" + seg(id) + "/events", null, options), ProviderEvents.class);
    }

    public ProviderEvents events(String id) {
        return events(id, null);
    }
}
