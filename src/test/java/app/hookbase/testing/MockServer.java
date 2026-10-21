package app.hookbase.testing;

import app.hookbase.Hookbase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An in-process HTTP server that records every request and answers from a queue (Go's httptest,
 * in effect). When the queue is empty it answers 200 {@code {}}.
 */
public final class MockServer implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** One recorded request. {@code body} is null when there was none. */
    public record Request(String method, String path, Map<String, String> query, Map<String, List<String>> headers, String rawBody) {
        public JsonNode body() {
            try {
                return rawBody == null || rawBody.isEmpty() ? null : MAPPER.readTree(rawBody);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public String header(String name) {
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                if (e.getKey().equalsIgnoreCase(name) && !e.getValue().isEmpty()) return e.getValue().getFirst();
            }
            return null;
        }
    }

    public record Response(int status, String body, Map<String, String> headers) {}

    private final HttpServer server;
    private final List<Request> requests = new ArrayList<>();
    private final Deque<Response> responses = new ArrayDeque<>();
    private final AtomicReference<Response> fallback = new AtomicReference<>(new Response(200, "{}", Map.of()));

    public MockServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/", exchange -> {
            String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> query = new LinkedHashMap<>();
            String q = exchange.getRequestURI().getRawQuery();
            if (q != null) {
                for (String pair : q.split("&")) {
                    int eq = pair.indexOf('=');
                    query.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                            URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
                }
            }
            Response response;
            synchronized (this) {
                requests.add(new Request(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath(), query,
                        Map.copyOf(exchange.getRequestHeaders()), raw.isEmpty() ? null : raw));
                response = responses.isEmpty() ? fallback.get() : responses.poll();
            }
            boolean json = !response.headers().keySet().stream().anyMatch(k -> k.equalsIgnoreCase("content-type"));
            if (json) exchange.getResponseHeaders().add("Content-Type", "application/json");
            response.headers().forEach((k, v) -> exchange.getResponseHeaders().add(k, v));
            byte[] bytes = response.body() == null ? new byte[0] : response.body().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(response.status(), response.status() == 204 || bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0 && response.status() != 204) exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** A client pointed at this server, with retries that don't sleep. */
    public Hookbase client() {
        return TestClients.noSleep(url(), 3);
    }

    public synchronized MockServer enqueue(int status, String body) {
        responses.add(new Response(status, body, Map.of()));
        return this;
    }

    public synchronized MockServer enqueue(int status, String body, Map<String, String> headers) {
        responses.add(new Response(status, body, headers));
        return this;
    }

    /** The answer once the queue is empty. */
    public MockServer otherwise(int status, String body) {
        fallback.set(new Response(status, body, Map.of()));
        return this;
    }

    public synchronized List<Request> requests() {
        return List.copyOf(requests);
    }

    public synchronized Request last() {
        if (requests.isEmpty()) throw new AssertionError("no requests recorded");
        return requests.getLast();
    }

    public static JsonNode json(String text) {
        try {
            return MAPPER.readTree(text);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
