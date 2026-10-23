package app.hookbase.resources;

import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.Hookbase;
import app.hookbase.RequestOptions;
import app.hookbase.params.ListApplicationsParams;
import app.hookbase.params.ListEndpointsParams;
import app.hookbase.params.ListEventTypesParams;
import app.hookbase.params.ListFiltersParams;
import app.hookbase.params.ListSubscriptionsParams;
import app.hookbase.testing.MockServer;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The shorter overloads (no params, no options) must send the same request as the full form. Each
 * one is checked for its method, path and the query or body it sends with nothing set.
 */
class ConvenienceOverloadsTest {

    private MockServer server;
    private Hookbase hookbase;

    @BeforeEach
    void start() {
        server = new MockServer();
        hookbase = server.client();
    }

    @AfterEach
    void stop() {
        server.close();
    }

    static Stream<Arguments> overloads() {
        return Stream.of(
                // numbered lists start at page 1; listAll() reads until a page comes back empty
                req("sources.listAll()", h -> h.sources().listAll().stream().toList(), "GET", "/api/sources", Map.of("page", "1")),
                req("destinations.listAll()", h -> h.destinations().listAll().stream().toList(), "GET", "/api/destinations", Map.of("page", "1")),
                req("routes.listAll()", h -> h.routes().listAll().stream().toList(), "GET", "/api/routes", Map.of("page", "1")),
                req("transforms.listAll()", h -> h.transforms().listAll().stream().toList(), "GET", "/api/transforms", Map.of("page", "1")),
                req("filters.listAll()", h -> h.filters().listAll().stream().toList(), "GET", "/api/filters", Map.of("page", "1")),
                req("filters.list(params)", h -> h.filters().list(new ListFiltersParams().pageSize(5)), "GET", "/api/filters", Map.of("pageSize", "5")),
                // cursor lists default to 100 per page when walking everything
                req("events.listAll()", h -> h.events().listAll().stream().toList(), "GET", "/api/events", Map.of("limit", "100")),
                req("applications.listAll(params)", h -> h.applications().listAll(new ListApplicationsParams().search("acme")).stream().toList(),
                        "GET", "/api/webhook-applications", Map.of("search", "acme", "limit", "100")),
                req("eventTypes.listAll(params)", h -> h.eventTypes().listAll(new ListEventTypesParams().category("orders")).stream().toList(),
                        "GET", "/api/event-types", Map.of("category", "orders", "limit", "100")),
                req("endpoints.listAll(app, params)", h -> h.endpoints().listAll("app_1", new ListEndpointsParams().isDisabled(false)).stream().toList(),
                        "GET", "/api/webhook-endpoints", Map.of("applicationId", "app_1", "isDisabled", "false", "limit", "100")),
                req("subscriptions.listAll(app, params)", h -> h.subscriptions().listAll("app_1", new ListSubscriptionsParams().endpointId("ep_1")).stream().toList(),
                        "GET", "/api/webhook-subscriptions", Map.of("applicationId", "app_1", "endpointId", "ep_1", "limit", "100")),
                // exports with no ids export everything
                req("sources.export()", h -> h.sources().export(), "GET", "/api/sources/export", Map.of()),
                req("destinations.export()", h -> h.destinations().export(), "GET", "/api/destinations/export", Map.of()),
                req("routes.export()", h -> h.routes().export(), "GET", "/api/routes/export", Map.of()),
                req("routes.export(ids)", h -> h.routes().export(List.of("r1", "r2")), "GET", "/api/routes/export", Map.of("ids", "r1,r2")),
                req("events.export()", h -> h.events().export(), "GET", "/api/events/export", Map.of()),
                req("events.exportCsv()", h -> h.events().exportCsv(), "GET", "/api/events/export", Map.of("format", "csv")),
                // options-only overloads send no body
                req("endpoints.rotateSecret(app, ep, options)",
                        h -> h.endpoints().rotateSecret("app_1", "ep_1", RequestOptions.builder().retries(0).build()),
                        "POST", "/api/webhook-endpoints/ep_1/rotate-secret", Map.of()),
                req("endpoints.test(app, ep, options)", h -> h.endpoints().test("app_1", "ep_1", RequestOptions.NONE),
                        "POST", "/api/webhook-endpoints/ep_1/test", Map.of()));
    }

    private static Arguments req(String name, Consumer<Hookbase> call, String method, String path, Map<String, String> query) {
        return Arguments.of(name, call, method, path, query);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("overloads")
    void sendsTheSameRequestAsTheFullForm(String name, Consumer<Hookbase> call, String method, String path, Map<String, String> query) {
        call.accept(hookbase);
        MockServer.Request req = server.requests().getFirst();
        assertThat(req.method()).isEqualTo(method);
        assertThat(req.path()).isEqualTo(path);
        assertThat(req.query()).isEqualTo(query);
        if (method.equals("POST")) assertThat(req.rawBody()).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("imports")
    void importsWithoutParamsSkipConflicts(String name, Consumer<Hookbase> call, String path, String key) {
        call.accept(hookbase);
        MockServer.Request req = server.last();
        assertThat(req.method()).isEqualTo("POST");
        assertThat(req.path()).isEqualTo(path);
        assertThat(req.body().get(key).get(0).get("name").asText()).isEqualTo("a");
        assertThat(req.body().get("conflictStrategy").asText()).isEqualTo("skip");
        assertThat(req.body().has("validateOnly")).isFalse();
    }

    static Stream<Arguments> imports() {
        List<Map<String, String>> items = List.of(Map.of("name", "a"));
        return Stream.of(
                Arguments.of("destinations", (Consumer<Hookbase>) h -> h.destinations().importDestinations(items), "/api/destinations/import", "destinations"),
                Arguments.of("routes", (Consumer<Hookbase>) h -> h.routes().importRoutes(items), "/api/routes/import", "routes"),
                Arguments.of("sources", (Consumer<Hookbase>) h -> h.sources().importSources(items), "/api/sources/import", "sources"));
    }
}
