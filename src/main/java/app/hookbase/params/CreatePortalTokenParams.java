package app.hookbase.params;

import java.util.List;

/** Body for {@code portalTokens().create()}. Every field is optional. */
public final class CreatePortalTokenParams extends Params<CreatePortalTokenParams> {
    public CreatePortalTokenParams name(String name) {
        return set("name", name);
    }

    /** {@code read} and/or {@code write} (the API defaults to both). */
    public CreatePortalTokenParams scopes(List<String> scopes) {
        return set("scopes", scopes);
    }

    /** Token lifetime in days, 1-365 (the API defaults to 30). */
    public CreatePortalTokenParams expiresInDays(int expiresInDays) {
        return set("expiresInDays", expiresInDays);
    }

    /** IPs allowed to use the token. */
    public CreatePortalTokenParams allowedIps(List<String> allowedIps) {
        return set("allowedIps", allowedIps);
    }
}
