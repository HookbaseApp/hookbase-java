package app.hookbase.resources;

import app.hookbase.internal.HttpTransport;
import app.hookbase.testing.MockServer;
import java.net.http.HttpClient;
import java.time.Duration;

/** Builds a transport pointed at a {@link MockServer}, for constructing the outbound resources directly. */
final class OutboundTestSupport {
    private OutboundTestSupport() {}

    static HttpTransport transport(MockServer server) {
        return transport(server, 3);
    }

    /** Retries never sleep. */
    static HttpTransport transport(MockServer server, int retries) {
        return new HttpTransport("whr_test", server.url(), Duration.ofSeconds(5), retries,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), false, d -> {});
    }

    static String cursorPage(String items, String nextCursor) {
        return "{\"data\":[" + items + "],\"pagination\":{\"hasMore\":" + (nextCursor != null)
                + ",\"nextCursor\":" + (nextCursor == null ? "null" : "\"" + nextCursor + "\"") + "}}";
    }
}
