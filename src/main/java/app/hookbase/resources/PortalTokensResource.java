package app.hookbase.resources;

import app.hookbase.RequestOptions;
import app.hookbase.internal.HttpTransport;
import app.hookbase.models.PortalToken;
import app.hookbase.params.CreatePortalTokenParams;
import java.util.List;

/** Tokens for the embeddable customer portal, each scoped to one application. */
public final class PortalTokensResource extends BaseResource {

    public PortalTokensResource(HttpTransport http) {
        super(http);
    }

    private static String tokensPath(String applicationId) {
        return "/api/portal/webhook-applications/" + seg(applicationId) + "/tokens";
    }

    /** Create a portal token. The response is the only one that carries the plaintext {@code token}. */
    public PortalToken create(String applicationId, CreatePortalTokenParams params, RequestOptions options) {
        return unwrap(post(tokensPath(applicationId), body(params), options), "data", PortalToken.class);
    }

    public PortalToken create(String applicationId, CreatePortalTokenParams params) {
        return create(applicationId, params, null);
    }

    public PortalToken create(String applicationId) {
        return create(applicationId, null, null);
    }

    /** Every portal token of an application (the API does not paginate this list). */
    public List<PortalToken> list(String applicationId, RequestOptions options) {
        return list(get(tokensPath(applicationId), null, options), "data", PortalToken.class);
    }

    public List<PortalToken> list(String applicationId) {
        return list(applicationId, null);
    }

    /**
     * Revoke a portal token.
     *
     * @param applicationId the token's application (not needed by the API, which finds the token by id)
     */
    public void revoke(String applicationId, String tokenId, RequestOptions options) {
        delete("/api/portal/tokens/" + seg(tokenId), null, options);
    }

    public void revoke(String applicationId, String tokenId) {
        revoke(applicationId, tokenId, null);
    }
}
