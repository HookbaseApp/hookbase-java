package app.hookbase.resources;

import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.models.Schema;
import app.hookbase.models.SchemaValidationResult;
import app.hookbase.params.CreateSchemaParams;
import app.hookbase.params.UpdateSchemaParams;
import app.hookbase.testing.MockServer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchemasResourceTest {

    private MockServer server;
    private SchemasResource schemas;

    @BeforeEach
    void start() {
        server = new MockServer();
        schemas = new SchemasResource(DataAdminTestSupport.transport(server));
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void listParsesSchemaText() {
        server.enqueue(200, "{\"schemas\":[{\"id\":\"sch_1\",\"name\":\"Order\",\"slug\":\"order\","
                + "\"jsonSchema\":\"{\\\"type\\\":\\\"object\\\"}\"},{\"id\":\"sch_2\",\"name\":\"B\",\"jsonSchema\":{\"type\":\"array\"}}]}");
        List<Schema> page = schemas.list();
        assertThat(server.last().path()).isEqualTo("/api/schemas");
        assertThat(page).hasSize(2);
        assertThat(page.get(0).jsonSchema().get("type").asText()).isEqualTo("object");
        assertThat(page.get(1).jsonSchema().get("type").asText()).isEqualTo("array");
    }

    @Test
    void crudAndValidate() {
        server.enqueue(200, "{\"schema\":{\"id\":\"sch_1\",\"name\":\"Order\",\"jsonSchema\":{\"type\":\"object\"}}}");
        assertThat(schemas.get("order").jsonSchema().get("type").asText()).isEqualTo("object");
        assertThat(server.last().path()).isEqualTo("/api/schemas/order");

        // POST echoes the schema as json_schema.
        server.enqueue(201, "{\"schema\":{\"id\":\"sch_new\",\"name\":\"New\",\"slug\":\"new\",\"json_schema\":{\"type\":\"object\"}}}");
        Schema created = schemas.create(new CreateSchemaParams().name("New")
                .jsonSchema(Map.of("type", "object")));
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"name\":\"New\",\"jsonSchema\":{\"type\":\"object\"}}"));
        assertThat(created.jsonSchema().get("type").asText()).isEqualTo("object");

        server.enqueue(200, "{\"success\":true}");
        schemas.update("sch_1", new UpdateSchemaParams().description(null));
        assertThat(server.last().method()).isEqualTo("PUT");
        assertThat(server.last().rawBody()).isEqualTo("{\"description\":null}");

        server.enqueue(200, "{\"success\":true}");
        schemas.delete("sch_1");
        assertThat(server.last().method()).isEqualTo("DELETE");

        server.enqueue(200, "{\"valid\":false,\"errors\":[{\"path\":\"event\",\"message\":\"Missing required field: event\"}],\"payload\":{\"foo\":\"bar\"}}");
        SchemaValidationResult r = schemas.validate("sch_1", Map.of("foo", "bar"));
        assertThat(server.last().path()).isEqualTo("/api/schemas/sch_1/validate");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"payload\":{\"foo\":\"bar\"}}"));
        assertThat(r.valid()).isFalse();
        assertThat(r.errors().getFirst().message()).isEqualTo("Missing required field: event");
        assertThat(r.payload().get("foo").asText()).isEqualTo("bar");
    }
}
