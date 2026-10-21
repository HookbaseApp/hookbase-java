package app.hookbase;

import app.hookbase.internal.HttpTransport;
import app.hookbase.internal.SdkVersion;
import app.hookbase.resources.ApplicationsResource;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Hookbase API client. Create one per API key and share it: it is immutable and thread-safe.
 *
 * <pre>{@code
 * Hookbase hookbase = new Hookbase(System.getenv("HOOKBASE_API_KEY"));
 *
 * hookbase.messages().send("app_123", new SendMessageParams()
 *         .eventType("order.created")
 *         .payload(Map.of("orderId", "123")));
 * }</pre>
 *
 * Every call is blocking; run calls on virtual threads for concurrency.
 */
public final class Hookbase {

    /** The SDK version, as sent in the User-Agent. */
    public static final String VERSION = SdkVersion.VERSION;

    private final HttpTransport http;

    // RESOURCE FIELDS
    private final ApplicationsResource applications;

    /** A client with default settings: {@code https://api.hookbase.app}, 30 s timeout, 3 retries. */
    public Hookbase(String apiKey) {
        this(builder().apiKey(apiKey));
    }

    private Hookbase(Builder b) {
        HttpClient client = b.httpClient != null
                ? b.httpClient
                : HttpClient.newBuilder()
                        .connectTimeout(b.timeout)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
        this.http = new HttpTransport(b.apiKey, b.baseUrl, b.timeout, b.maxRetries, client, b.debug, b.sleeper);
        // RESOURCE INIT
        this.applications = new ApplicationsResource(http);
    }

    // RESOURCE ACCESSORS
    /** Outbound applications (your customers/tenants). */
    public ApplicationsResource applications() {
        return applications;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Make a raw API request, for an endpoint this SDK version doesn't wrap yet. Gets the same
     * auth, retries and error mapping as every other call.
     *
     * @param path the path from the API root, e.g. {@code "/api/alert-rules"}
     * @param query query parameters; {@code null} values are left out
     * @param body serialized as JSON; {@code null} for none
     * @return the parsed response, a text node for a non-JSON body, or {@code null} for 204
     */
    public JsonNode request(String method, String path, Map<String, ?> query, Object body, RequestOptions options) {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(path, "path");
        return http.request(method, path.startsWith("/") ? path : "/" + path, query, body, options);
    }

    /** Builder for {@link Hookbase}. */
    public static final class Builder {
        private String apiKey;
        private String baseUrl = HttpTransport.DEFAULT_BASE_URL;
        private Duration timeout = HttpTransport.DEFAULT_TIMEOUT;
        private int maxRetries = HttpTransport.DEFAULT_MAX_RETRIES;
        private HttpClient httpClient;
        private boolean debug;
        private HttpTransport.Sleeper sleeper = d -> Thread.sleep(d);

        private Builder() {}

        /** Required. An API key ({@code whr_...}) from Settings → API Keys. */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /** API base URL. Default {@code https://api.hookbase.app}. */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
            return this;
        }

        /** Per-request timeout. Default 30 seconds. */
        public Builder timeout(Duration timeout) {
            this.timeout = Objects.requireNonNull(timeout, "timeout");
            return this;
        }

        /** How many times a failed request is retried (see the class docs for which). Default 3. */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * A preconfigured {@link HttpClient} (proxy, SSL context, executor). Its own connect timeout
         * applies; {@link #timeout} still bounds each request.
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Log each request's method and URL, and each retry, to the {@code app.hookbase}
         * {@link System.Logger} at INFO. Bodies and headers are never logged.
         */
        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        /** Replace the sleep between retries. For tests. */
        Builder sleeper(HttpTransport.Sleeper sleeper) {
            this.sleeper = Objects.requireNonNull(sleeper, "sleeper");
            return this;
        }

        /**
         * @throws IllegalArgumentException if the API key is missing, or retries/timeout are invalid
         */
        public Hookbase build() {
            return new Hookbase(this);
        }
    }
}
