package app.hookbase.resources;

import static app.hookbase.resources.OutboundTestSupport.cursorPage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.hookbase.Page;
import app.hookbase.errors.NotFoundException;
import app.hookbase.models.EventType;
import app.hookbase.params.CreateEventTypeParams;
import app.hookbase.params.ListEventTypesParams;
import app.hookbase.params.UpdateEventTypeParams;
import app.hookbase.testing.MockServer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventTypesResourceTest {

    static final String ET = """
            {"id":"et_1","organizationId":"org_1","name":"order.created","displayName":"Order created",
             "description":"d","category":"orders","schema":{"type":"object"},"schemaVersion":2,
             "examplePayload":{"id":1},"documentationUrl":"https://docs.example","defaultPriority":2,
             "isEnabled":true,"isDeprecated":false,"deprecatedAt":null,"deprecatedMessage":null,"isPublic":true,
             "publishedAt":null,"subscriptionCount":4,"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-02T00:00:00Z"}""";

    private MockServer server;
    private EventTypesResource eventTypes;

    @BeforeEach
    void start() {
        server = new MockServer();
        eventTypes = new EventTypesResource(OutboundTestSupport.transport(server));
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void listSendsFiltersAndBindsFields() {
        server.enqueue(200, cursorPage(ET, "c2"));
        Page<EventType> page = eventTypes.list(new ListEventTypesParams().category("orders").isEnabled(true).search("order").limit(5).cursor("c1"));
        assertThat(server.last().path()).isEqualTo("/api/event-types");
        assertThat(server.last().query()).containsExactlyInAnyOrderEntriesOf(
                Map.of("category", "orders", "isEnabled", "true", "search", "order", "limit", "5", "cursor", "c1"));
        EventType et = page.data().getFirst();
        assertThat(et.schema().get("type").asText()).isEqualTo("object");
        assertThat(et.examplePayload().get("id").asInt()).isEqualTo(1);
        assertThat(et.schemaVersion()).isEqualTo(2);
        assertThat(et.defaultPriority()).isEqualTo(2);
        assertThat(et.isPublic()).isTrue();
        assertThat(et.subscriptionCount()).isEqualTo(4);
        assertThat(page.cursor()).isEqualTo("c2");
    }

    @Test
    void listAllFollowsCursorsWithDefaultLimit100() {
        server.enqueue(200, cursorPage(ET, "next"));
        server.enqueue(200, cursorPage(ET.replace("et_1", "et_2"), null));
        assertThat(eventTypes.listAll().stream().map(EventType::id).toList()).containsExactly("et_1", "et_2");
        assertThat(server.requests().get(0).query()).containsEntry("limit", "100").doesNotContainKey("cursor");
        assertThat(server.requests().get(1).query()).containsEntry("cursor", "next");
    }

    @Test
    void getDecodesSchemaAndExampleSentAsJsonText() {
        server.enqueue(200, "{\"data\":{\"id\":\"et_1\",\"schema\":\"{\\\"type\\\":\\\"object\\\"}\",\"examplePayload\":\"{\\\"id\\\":1}\"}}");
        EventType et = eventTypes.get("et_1");
        assertThat(server.last().path()).isEqualTo("/api/event-types/et_1");
        assertThat(et.schema().get("type").asText()).isEqualTo("object");
        assertThat(et.examplePayload().get("id").asInt()).isEqualTo(1);

        server.enqueue(200, "{\"data\":{\"id\":\"et_1\",\"schema\":null,\"examplePayload\":\"plain text\"}}");
        EventType plain = eventTypes.get("et_1");
        assertThat(plain.schema()).isNull();
        assertThat(plain.examplePayload().asText()).isEqualTo("plain text");
    }

    @Test
    void createSendsSchemaAndExampleAsJsonStrings() {
        server.enqueue(201, "{\"data\":" + ET + "}");
        eventTypes.create(new CreateEventTypeParams().name("order.created").schema(Map.of("type", "object"))
                .examplePayload(List.of(1)).documentationUrl("https://docs.example/order").defaultPriority(1).isEnabled(false));
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().body()).isEqualTo(MockServer.json("""
                {"name":"order.created","schema":"{\\"type\\":\\"object\\"}","examplePayload":"[1]",
                 "documentationUrl":"https://docs.example/order","defaultPriority":1,"isEnabled":false}"""));
    }

    @Test
    void updatePassesAStringSchemaThroughAndSendsNullToClear() {
        server.enqueue(200, "{\"data\":" + ET + "}").enqueue(200, "{\"data\":" + ET + "}");
        eventTypes.update("et_1", new UpdateEventTypeParams().schema("{\"type\":\"object\"}").isPublic(true).deprecatedMessage("use v2"));
        assertThat(server.last().method()).isEqualTo("PATCH");
        assertThat(server.last().body()).isEqualTo(MockServer.json(
                "{\"schema\":\"{\\\"type\\\":\\\"object\\\"}\",\"isPublic\":true,\"deprecatedMessage\":\"use v2\"}"));

        eventTypes.update("et_1", new UpdateEventTypeParams().schema(null).category(null));
        assertThat(server.last().rawBody()).isEqualTo("{\"schema\":null,\"category\":null}");
    }

    @Test
    void deleteArchiveUnarchive() {
        server.enqueue(204, null);
        eventTypes.delete("et_1");
        assertThat(server.last().method()).isEqualTo("DELETE");
        assertThat(server.last().path()).isEqualTo("/api/event-types/et_1");

        server.enqueue(200, "{\"data\":" + ET + "}").enqueue(200, "{\"data\":" + ET + "}");
        eventTypes.archive("et_1");
        assertThat(server.last().rawBody()).isEqualTo("{\"isEnabled\":false}");
        eventTypes.unarchive("et_1");
        assertThat(server.last().rawBody()).isEqualTo("{\"isEnabled\":true}");
    }

    @Test
    void getByNameFindsTheExactNamePastPageOneAndThrowsWhenAbsent() {
        server.enqueue(200, cursorPage("{\"id\":\"et_1\",\"name\":\"order.created\"}", "next"));
        server.enqueue(200, cursorPage("{\"id\":\"et_2\",\"name\":\"order\"}", null));
        assertThat(eventTypes.getByName("order").id()).isEqualTo("et_2");
        assertThat(server.requests().get(0).query()).containsEntry("search", "order").containsEntry("limit", "100");

        server.enqueue(200, cursorPage("{\"id\":\"et_1\",\"name\":\"order.created\"}", null));
        assertThatThrownBy(() -> eventTypes.getByName("order")).isInstanceOf(NotFoundException.class).hasMessageContaining("order");
    }

    @Test
    void getOrCreateCreatesOnlyWhenMissing() {
        server.enqueue(200, cursorPage("{\"id\":\"et_1\",\"name\":\"order.created\"}", null));
        assertThat(eventTypes.getOrCreate("order.created", new CreateEventTypeParams()).id()).isEqualTo("et_1");
        assertThat(server.requests()).hasSize(1);

        server.enqueue(200, cursorPage("", null));
        server.enqueue(201, "{\"data\":" + ET + "}");
        eventTypes.getOrCreate("order.created", new CreateEventTypeParams().name("ignored").schema(Map.of("type", "object")));
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"name\":\"order.created\",\"schema\":\"{\\\"type\\\":\\\"object\\\"}\"}"));
    }
}
