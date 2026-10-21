package app.hookbase.resources;

import app.hookbase.internal.HttpTransport;
import app.hookbase.testing.MockServer;
import java.net.http.HttpClient;
import java.time.Duration;

/** Builds a transport against a MockServer with no retries, for constructing resources directly. */
final class DataAdminTestSupport {
    private DataAdminTestSupport() {}

    static HttpTransport transport(MockServer server) {
        return new HttpTransport("whr_test", server.url(), Duration.ofSeconds(5), 0, HttpClient.newHttpClient(), false, d -> {});
    }
}
