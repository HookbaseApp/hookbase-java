package app.hookbase;

import app.hookbase.internal.HttpTransport;
import app.hookbase.internal.SdkVersion;
import app.hookbase.resources.AnalyticsResource;
import app.hookbase.resources.ApiKeysResource;
import app.hookbase.resources.ApplicationsResource;
import app.hookbase.resources.CronJobsResource;
import app.hookbase.resources.DeliveriesResource;
import app.hookbase.resources.DestinationsResource;
import app.hookbase.resources.DlqResource;
import app.hookbase.resources.EndpointsResource;
import app.hookbase.resources.EventTypesResource;
import app.hookbase.resources.EventsResource;
import app.hookbase.resources.FiltersResource;
import app.hookbase.resources.MessagesResource;
import app.hookbase.resources.PortalTokensResource;
import app.hookbase.resources.ProvidersResource;
import app.hookbase.resources.RoutesResource;
import app.hookbase.resources.SchemasResource;
import app.hookbase.resources.SourcesResource;
import app.hookbase.resources.SubscriptionsResource;
import app.hookbase.resources.TransformsResource;
import app.hookbase.resources.TunnelsResource;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;
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
public final class Hookbase implements ApiClient {

    /** The SDK version, as sent in the User-Agent. */
    public static final String VERSION = SdkVersion.VERSION;

    private final HttpTransport http;

    // RESOURCE FIELDS
    private final ApplicationsResource applications;
    private final EndpointsResource endpoints;
    private final EventTypesResource eventTypes;
    private final SubscriptionsResource subscriptions;
    private final MessagesResource messages;
    private final PortalTokensResource portalTokens;
    private final DlqResource dlq;
    private final SourcesResource sources;
    private final DestinationsResource destinations;
    private final RoutesResource routes;
    private final EventsResource events;
    private final DeliveriesResource deliveries;
    private final TransformsResource transforms;
    private final FiltersResource filters;
    private final SchemasResource schemas;
    private final ProvidersResource providers;
    private final AnalyticsResource analytics;
    private final ApiKeysResource apiKeys;
    private final TunnelsResource tunnels;
    private final CronJobsResource cronJobs;

    /** A client with default settings: {@code https://api.hookbase.app}, 30 s timeout, 3 retries. */
    public Hookbase(String apiKey) {
        this(builder().apiKey(apiKey));
    }

    private Hookbase(Builder b) {
        checkBaseUrl(b.baseUrl);
        if (b.httpClient != null && b.httpClient.followRedirects() != HttpClient.Redirect.NEVER) {
            // The JDK copies request headers onto a followed redirect, Authorization included.
            throw new IllegalArgumentException("httpClient must not follow redirects (use Redirect.NEVER)");
        }
        HttpClient client = b.httpClient != null
                ? b.httpClient
                : HttpClient.newBuilder()
                        .connectTimeout(b.timeout)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
        this.http = new HttpTransport(b.apiKey, b.baseUrl, b.timeout, b.maxRetries, client, b.debug, b.sleeper);
        // RESOURCE INIT
        this.applications = new ApplicationsResource(http);
        this.endpoints = new EndpointsResource(http);
        this.eventTypes = new EventTypesResource(http);
        this.subscriptions = new SubscriptionsResource(http);
        this.messages = new MessagesResource(http);
        this.portalTokens = new PortalTokensResource(http);
        this.dlq = new DlqResource(http);
        this.sources = new SourcesResource(http);
        this.destinations = new DestinationsResource(http);
        this.routes = new RoutesResource(http);
        this.events = new EventsResource(http);
        this.deliveries = new DeliveriesResource(http);
        this.transforms = new TransformsResource(http);
        this.filters = new FiltersResource(http);
        this.schemas = new SchemasResource(http);
        this.providers = new ProvidersResource(http);
        this.analytics = new AnalyticsResource(http);
        this.apiKeys = new ApiKeysResource(http);
        this.tunnels = new TunnelsResource(http);
        this.cronJobs = new CronJobsResource(http);
    }

    // RESOURCE ACCESSORS
    /** Outbound applications (your customers/tenants). */
    public ApplicationsResource applications() {
        return applications;
    }

    /** Outbound endpoints: where your customers receive webhooks. */
    public EndpointsResource endpoints() {
        return endpoints;
    }

    /** Outbound event types that endpoints subscribe to. */
    public EventTypesResource eventTypes() {
        return eventTypes;
    }

    /** Subscriptions: which event types each endpoint receives. */
    public SubscriptionsResource subscriptions() {
        return subscriptions;
    }

    /** Send outbound events, and read the messages and attempts that deliver them. */
    public MessagesResource messages() {
        return messages;
    }

    /** Tokens for the embeddable customer portal. */
    public PortalTokensResource portalTokens() {
        return portalTokens;
    }

    /** The outbound dead letter queue. */
    public DlqResource dlq() {
        return dlq;
    }

    /** Inbound sources: where third parties send you webhooks. */
    public SourcesResource sources() {
        return sources;
    }

    /** Inbound destinations: where routed events are delivered. */
    public DestinationsResource destinations() {
        return destinations;
    }

    /** Inbound routes connecting sources to destinations. */
    public RoutesResource routes() {
        return routes;
    }

    /** Inbound webhook events received by your sources. */
    public EventsResource events() {
        return events;
    }

    /** Inbound deliveries (event to destination), and replaying them. */
    public DeliveriesResource deliveries() {
        return deliveries;
    }

    /** Inbound payload transforms. */
    public TransformsResource transforms() {
        return transforms;
    }

    /** Inbound routing filters. */
    public FiltersResource filters() {
        return filters;
    }

    /** JSON schemas for validating inbound payloads. */
    public SchemasResource schemas() {
        return schemas;
    }

    /** Provider catalog: what each provider signs with and which secret it needs. */
    public ProvidersResource providers() {
        return providers;
    }

    /** Inbound analytics dashboard and overview. */
    public AnalyticsResource analytics() {
        return analytics;
    }

    /** The organization's API keys. */
    public ApiKeysResource apiKeys() {
        return apiKeys;
    }

    /** Local-development tunnels. */
    public TunnelsResource tunnels() {
        return tunnels;
    }

    /** Cron jobs (scheduled HTTP requests) and cron groups. */
    public CronJobsResource cronJobs() {
        return cronJobs;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** HTTPS only, so the API key is never sent in cleartext; plain HTTP is allowed for loopback. */
    private static void checkBaseUrl(String baseUrl) {
        URI uri;
        try {
            uri = new URI(baseUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("baseUrl is not a valid URL");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost();
        if (host == null || !(scheme.equals("https") || scheme.equals("http"))) {
            throw new IllegalArgumentException("baseUrl must be an http(s) URL with a host");
        }
        if (uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("baseUrl must not contain credentials, a query or a fragment");
        }
        boolean loopback = host.equals("localhost") || host.equals("127.0.0.1") || host.equals("[::1]")
                || host.endsWith(".localhost");
        if (scheme.equals("http") && !loopback) {
            throw new IllegalArgumentException("baseUrl must use https (plain http is allowed only for localhost)");
        }
    }

    /**
     * Make a raw API request, for an endpoint this SDK version doesn't wrap yet. Gets the same
     * auth, retries and error mapping as every other call.
     *
     * @param path the path from the API root, e.g. {@code "/api/alert-rules"}
     * @param method the HTTP method, e.g. {@code "GET"}
     * @param query query parameters; {@code null} values are left out and collections are sent
     *     comma-joined
     * @param body serialized as JSON; {@code null} for none
     * @param options per-request overrides, or {@code null}
     * @return the parsed response, a text node for a non-JSON body, or {@code null} for 204
     */
    @Override
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

        /**
         * API base URL. Default {@code https://api.hookbase.app}. Must be {@code https}, except for
         * {@code localhost}/loopback.
         */
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
         * applies; {@link #timeout} still bounds each request. It must not follow redirects ({@link
         * HttpClient.Redirect#NEVER}, the JDK default), since a followed redirect would carry the API
         * key to the new location.
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
         * @throws IllegalArgumentException if the API key is missing, the base URL is not https (or
         *     loopback http), the HTTP client follows redirects, or retries/timeout are invalid
         */
        public Hookbase build() {
            return new Hookbase(this);
        }
    }
}
