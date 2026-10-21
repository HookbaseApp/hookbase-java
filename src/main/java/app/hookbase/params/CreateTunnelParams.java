package app.hookbase.params;

import java.util.List;

/** Body for {@code tunnels().create()}. {@code name} is required. */
public final class CreateTunnelParams extends Params<CreateTunnelParams> {
    public CreateTunnelParams name(String name) {
        return set("name", name);
    }

    /** The subdomain to request; one is generated when left out. 409 when it is taken. */
    public CreateTunnelParams subdomain(String subdomain) {
        return set("subdomain", subdomain);
    }

    /** {@code inbound} (default) or {@code bidirectional} (Pro and Business plans). */
    public CreateTunnelParams direction(String direction) {
        return set("direction", direction);
    }

    /** Hosts a bidirectional tunnel may reach, at most 20. */
    public CreateTunnelParams allowedHosts(List<String> allowedHosts) {
        return set("allowedHosts", allowedHosts);
    }
}
