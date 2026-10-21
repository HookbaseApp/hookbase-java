package app.hookbase.internal;

import app.hookbase.RequestOptions;
import app.hookbase.errors.AuthenticationException;
import app.hookbase.errors.ConflictException;
import app.hookbase.errors.ForbiddenException;
import app.hookbase.errors.HookbaseApiException;
import app.hookbase.errors.HookbaseException;
import app.hookbase.errors.HookbaseNetworkException;
import app.hookbase.errors.HookbaseTimeoutException;
import app.hookbase.errors.NotFoundException;
import app.hookbase.errors.RateLimitException;
import app.hookbase.errors.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Sends requests to the Hookbase API: auth and User-Agent headers, retries with backoff, and mapping
 * error responses to exceptions. Internal: use {@link app.hookbase.Hookbase#request} for raw calls.
 *
 * <p>Retry policy, shared with the other Hookbase SDKs:
 *
 * <ul>
 *   <li>429 is retried after {@code Retry-After} (60 s when absent), for every method: the API
 *       refused the request, so nothing ran.
 *   <li>500, 502, 503, 504, timeouts and dropped connections are retried with exponential backoff
 *       ({@code min(1s·2^n, 10s)} plus up to 1 s of jitter), except on a POST with no
 *       {@code Idempotency-Key}: the POST may already have been carried out, and repeating it can
 *       create the thing twice. Setting {@link RequestOptions#retries()} explicitly opts a POST back
 *       in, as in the Node SDK.
 *   <li>A connection that was never established (refused, DNS failure, connect timeout) is always
 *       retried, POST included: the request never reached the server.
 *   <li>Every other status is the API's answer and is thrown straight away.
 * </ul>
 */
public final class HttpTransport {

    public static final String DEFAULT_BASE_URL = "https://api.hookbase.app";
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    public static final int DEFAULT_MAX_RETRIES = 3;

    private static final Set<Integer> RETRY_STATUSES = Set.of(500, 502, 503, 504);
    private static final Duration DEFAULT_RETRY_AFTER = Duration.ofSeconds(60);
    private static final Pattern JSON_CONTENT_TYPE = Pattern.compile("[/+]json\\b", Pattern.CASE_INSENSITIVE);
    private static final System.Logger LOG = System.getLogger("app.hookbase");

    /** Waits between attempts. Replaced in tests so retries don't sleep. */
    @FunctionalInterface
    public interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private final String apiKey;
    private final String baseUrl;
    private final Duration timeout;
    private final int maxRetries;
    private final HttpClient http;
    private final boolean debug;
    private final Sleeper sleeper;
    private final String userAgent;

    public HttpTransport(
            String apiKey,
            String baseUrl,
            Duration timeout,
            int maxRetries,
            HttpClient http,
            boolean debug,
            Sleeper sleeper) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key is required");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0");
        }
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.timeout = timeout;
        this.maxRetries = maxRetries;
        this.http = http;
        this.debug = debug;
        this.sleeper = sleeper;
        this.userAgent = "hookbase-java/" + SdkVersion.VERSION + " java/" + Runtime.version().feature();
    }

    public String baseUrl() {
        return baseUrl;
    }

    /**
     * Send a request and return the response body: parsed JSON, a {@link TextNode} for a non-JSON
     * body (CSV exports), or {@code null} for an empty one (204).
     *
     * @param query values that are {@code null} are left out; collections are sent comma-joined
     * @param body serialized as JSON; {@code null} means no body
     */
    public JsonNode request(String method, String path, Map<String, ?> query, Object body, RequestOptions options) {
        RequestOptions opts = options == null ? RequestOptions.NONE : options;
        String upper = method.toUpperCase(Locale.ROOT);
        String idempotencyKey = opts.idempotencyKey();
        boolean retryAmbiguousFailures = !upper.equals("POST")
                || (idempotencyKey != null && !idempotencyKey.isEmpty())
                || opts.retries() != null;
        int retries = opts.retries() != null ? opts.retries() : maxRetries;
        Duration requestTimeout = opts.timeout() != null ? opts.timeout() : timeout;

        URI uri = URI.create(baseUrl + path + queryString(query));
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json");
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            builder.header("Idempotency-Key", idempotencyKey);
        }
        if (body != null) {
            builder.header("Content-Type", "application/json");
            builder.method(upper, HttpRequest.BodyPublishers.ofString(Json.write(body), StandardCharsets.UTF_8));
        } else {
            builder.method(upper, HttpRequest.BodyPublishers.noBody());
        }
        HttpRequest request = builder.build();

        HookbaseException last = null;
        for (int attempt = 0; attempt <= retries; attempt++) {
            if (debug) {
                // Method and URL only: bodies can carry signing secrets, and headers the API key.
                LOG.log(System.Logger.Level.INFO, "[Hookbase] {0} {1}", upper, uri);
            }
            Duration wait;
            try {
                return execute(request, requestTimeout);
            } catch (RateLimitException e) {
                last = e;
                wait = e.retryAfter();
            } catch (HookbaseApiException e) {
                if (!RETRY_STATUSES.contains(e.status()) || !retryAmbiguousFailures) throw e;
                last = e;
                wait = backoff(attempt);
            } catch (ConnectFailure e) {
                last = e.exception;
                wait = backoff(attempt);
            } catch (HookbaseTimeoutException | HookbaseNetworkException e) {
                if (!retryAmbiguousFailures) throw e;
                last = e;
                wait = backoff(attempt);
            }
            if (attempt >= retries) break;
            if (debug) {
                LOG.log(System.Logger.Level.INFO, "[Hookbase] {0}; retrying in {1} ms (attempt {2}/{3})",
                        last.getMessage(), wait.toMillis(), attempt + 1, retries);
            }
            try {
                sleeper.sleep(wait);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new HookbaseNetworkException("Interrupted while waiting to retry", ie);
            }
        }
        throw last;
    }

    /** A connection that was never established; carried separately so it is always retried. */
    private static final class ConnectFailure extends Exception {
        private static final long serialVersionUID = 1L;
        final HookbaseNetworkException exception;

        ConnectFailure(HookbaseNetworkException exception) {
            super(exception.getMessage(), null, false, false);
            this.exception = exception;
        }
    }

    private JsonNode execute(HttpRequest request, Duration requestTimeout) throws ConnectFailure {
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpConnectTimeoutException e) {
            throw new ConnectFailure(new HookbaseNetworkException("Connection timed out: " + request.uri().getHost(), e));
        } catch (HttpTimeoutException e) {
            throw new HookbaseTimeoutException("Request timed out after " + requestTimeout.toMillis() + "ms", e);
        } catch (ConnectException | UnknownHostException e) {
            throw new ConnectFailure(new HookbaseNetworkException(describe(e), e));
        } catch (IOException e) {
            if (e.getCause() instanceof UnresolvedAddressException || e.getCause() instanceof ConnectException) {
                throw new ConnectFailure(new HookbaseNetworkException(describe(e), e));
            }
            throw new HookbaseNetworkException(describe(e), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HookbaseNetworkException("Request interrupted", e);
        }

        int status = response.statusCode();
        String text = response.body();
        String requestId = response.headers().firstValue("x-request-id").orElse(null);

        if (status >= 200 && status < 300) {
            if (status == 204 || text == null || text.isEmpty()) return null;
            // CSV exports come back as text/csv.
            String contentType = response.headers().firstValue("content-type").orElse(null);
            if (contentType != null && !JSON_CONTENT_TYPE.matcher(contentType).find()) {
                return TextNode.valueOf(text);
            }
            try {
                return Json.read(text);
            } catch (IOException e) {
                throw new HookbaseApiException("Response was not valid JSON", status, "invalid_response", requestId, null);
            }
        }

        throw errorFor(status, text, requestId, response.headers().firstValue("retry-after").orElse(null));
    }

    private static String describe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    private static Duration backoff(int attempt) {
        long base = Math.min(1000L * (1L << Math.min(attempt, 20)), 10_000L);
        return Duration.ofMillis(base + ThreadLocalRandom.current().nextLong(1000));
    }

    /**
     * Map an error response to its exception. Hookbase sends {@code {"error": "<string>", "code",
     * "details": {"fieldErrors": {...}}}}; {@code error} is a plain string on almost every route, but
     * the nested {@code {"error": {"message", "code", "details"}}} shape is accepted too.
     */
    static HookbaseApiException errorFor(int status, String text, String requestId, String retryAfterHeader) {
        JsonNode body = null;
        try {
            if (text != null && !text.isBlank()) body = Json.read(text);
        } catch (IOException ignored) {
            // Not JSON (a proxy's HTML error page, say): fall back to the per-status default.
        }
        if (body != null && !body.isObject()) body = null;

        Map<String, Object> details = details(body);
        String message;
        switch (status) {
            case 401 -> {
                message = message(body, "Authentication failed");
                return new AuthenticationException(message, requestId, details);
            }
            case 403 -> {
                message = message(body, "Access forbidden");
                return new ForbiddenException(message, requestId, details);
            }
            case 404 -> {
                message = message(body, "Resource not found");
                return new NotFoundException(message, requestId, details);
            }
            case 409 -> {
                message = message(body, "Conflict");
                return new ConflictException(message, requestId, details);
            }
            case 400, 422 -> {
                message = message(body, "Validation failed");
                return new ValidationException(message, status, requestId, details, fieldErrors(details));
            }
            case 429 -> {
                message = message(body, "Rate limit exceeded");
                return new RateLimitException(message, parseRetryAfter(retryAfterHeader), requestId, details);
            }
            default -> {
                message = message(body, "API error: " + status);
                return new HookbaseApiException(message, status, code(body, "unknown_error"), requestId, details);
            }
        }
    }

    private static String message(JsonNode body, String fallback) {
        if (body == null) return fallback;
        JsonNode error = body.get("error");
        if (error != null && error.isTextual() && !error.asText().isEmpty()) return error.asText();
        if (error != null && error.isObject() && error.path("message").isTextual()) return error.get("message").asText();
        if (body.path("message").isTextual()) return body.get("message").asText();
        return fallback;
    }

    private static String code(JsonNode body, String fallback) {
        if (body == null) return fallback;
        JsonNode error = body.get("error");
        if (error != null && error.isObject() && error.path("code").isTextual()) return error.get("code").asText();
        if (body.path("code").isTextual()) return body.get("code").asText();
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> details(JsonNode body) {
        if (body == null) return null;
        JsonNode error = body.get("error");
        JsonNode details = error != null && error.isObject() && error.path("details").isObject()
                ? error.get("details")
                : body.get("details");
        if (details == null || !details.isObject()) return null;
        return Json.MAPPER.convertValue(details, LinkedHashMap.class);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> fieldErrors(Map<String, Object> details) {
        if (details == null || !(details.get("fieldErrors") instanceof Map<?, ?> raw)) return null;
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            List<String> messages = new ArrayList<>();
            if (entry.getValue() instanceof Collection<?> values) {
                for (Object v : values) messages.add(String.valueOf(v));
            } else if (entry.getValue() != null) {
                messages.add(String.valueOf(entry.getValue()));
            }
            out.put(String.valueOf(entry.getKey()), List.copyOf(messages));
        }
        return out;
    }

    static Duration parseRetryAfter(String header) {
        if (header == null || header.isBlank()) return DEFAULT_RETRY_AFTER;
        String value = header.trim();
        try {
            return Duration.ofSeconds(Math.max(0, Long.parseLong(value)));
        } catch (NumberFormatException ignored) {
            // Retry-After may also be an HTTP date.
        }
        try {
            ZonedDateTime at = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
            Duration until = Duration.between(ZonedDateTime.now(at.getZone()), at);
            return until.isNegative() ? Duration.ZERO : until;
        } catch (DateTimeParseException ignored) {
            return DEFAULT_RETRY_AFTER;
        }
    }

    /** Percent-encode one path segment (an id or slug the caller passed). */
    public static String segment(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("id must not be null or empty");
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static String queryString(Map<String, ?> query) {
        if (query == null || query.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ?> entry : query.entrySet()) {
            Object value = entry.getValue();
            if (value == null) continue;
            String text;
            if (value instanceof Collection<?> values) {
                if (values.isEmpty()) continue;
                List<String> parts = new ArrayList<>();
                for (Object v : values) parts.add(String.valueOf(v));
                text = String.join(",", parts);
            } else {
                text = String.valueOf(value);
            }
            sb.append(sb.isEmpty() ? '?' : '&')
                    .append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(text, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
