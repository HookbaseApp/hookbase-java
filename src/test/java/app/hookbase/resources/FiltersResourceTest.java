package app.hookbase.resources;

import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.Page;
import app.hookbase.models.Filter;
import app.hookbase.models.FilterCondition;
import app.hookbase.models.FilterTestResult;
import app.hookbase.params.CreateFilterParams;
import app.hookbase.params.ListFiltersParams;
import app.hookbase.params.TestFilterParams;
import app.hookbase.params.UpdateFilterParams;
import app.hookbase.testing.MockServer;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FiltersResourceTest {

    // List/get send conditions as the stored JSON text.
    static final String FILTER = """
            {"id":"fl_1","organizationId":"org_1","name":"Only pushes","slug":"only-pushes","description":null,
             "conditions":"[{\\"field\\":\\"type\\",\\"operator\\":\\"equals\\",\\"value\\":\\"push\\"}]","logic":"AND",
             "isActive":true,"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z"}""";

    private MockServer server;
    private FiltersResource filters;

    @BeforeEach
    void start() {
        server = new MockServer();
        filters = new FiltersResource(DataAdminTestSupport.transport(server));
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void listParsesConditionsSentAsText() {
        server.enqueue(200, "{\"filters\":[" + FILTER + "],\"pagination\":{\"total\":1,\"page\":1,\"pageSize\":20}}");
        Page<Filter> page = filters.list();
        assertThat(server.last().path()).isEqualTo("/api/filters");
        assertThat(server.last().query()).isEmpty();
        assertThat(page.hasMore()).isFalse();
        Filter f = page.data().getFirst();
        assertThat(f.conditions()).hasSize(1);
        assertThat(f.conditions().getFirst().operator()).isEqualTo("equals");
        assertThat(f.conditions().getFirst().value().asText()).isEqualTo("push");
        assertThat(f.logic()).isEqualTo("AND");
    }

    @Test
    void listAllWalksPages() {
        server.enqueue(200, "{\"filters\":[" + FILTER + "],\"pagination\":{\"total\":2,\"page\":1,\"pageSize\":1}}");
        server.enqueue(200, "{\"filters\":[" + FILTER + "],\"pagination\":{\"total\":2,\"page\":2,\"pageSize\":1}}");
        assertThat(filters.listAll(new ListFiltersParams().pageSize(1)).stream().toList()).hasSize(2);
        assertThat(server.requests().get(1).query()).containsExactlyInAnyOrderEntriesOf(Map.of("page", "2", "pageSize", "1"));
    }

    @Test
    void crudAndCreateWithArrayConditions() {
        server.enqueue(200, "{\"filter\":" + FILTER + "}");
        assertThat(filters.get("fl_1").conditions()).hasSize(1);
        assertThat(server.last().path()).isEqualTo("/api/filters/fl_1");

        server.enqueue(200, "{\"filter\":{\"id\":\"fl_new\",\"name\":\"New\",\"slug\":\"new\",\"description\":null,"
                + "\"conditions\":[{\"field\":\"amount\",\"operator\":\"greater_than\",\"value\":100}],\"logic\":\"OR\"}}");
        Filter created = filters.create(new CreateFilterParams().name("New")
                .conditions(List.of(new FilterCondition("amount", "greater_than", MockServer.json("100")),
                        Map.of("field", "type", "operator", "exists", "value", true)))
                .logic("OR"));
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"name\":\"New\",\"conditions\":["
                + "{\"field\":\"amount\",\"operator\":\"greater_than\",\"value\":100},"
                + "{\"field\":\"type\",\"operator\":\"exists\",\"value\":true}],\"logic\":\"OR\"}"));
        assertThat(created.conditions().getFirst().value().asInt()).isEqualTo(100);

        server.enqueue(200, "{\"success\":true}");
        filters.update("fl_1", new UpdateFilterParams().name("Renamed").description(null));
        assertThat(server.last().method()).isEqualTo("PATCH");
        assertThat(server.last().rawBody()).isEqualTo("{\"name\":\"Renamed\",\"description\":null}");

        server.enqueue(200, "{\"success\":true}");
        filters.delete("fl_1");
        assertThat(server.last().method()).isEqualTo("DELETE");
        assertThat(server.last().path()).isEqualTo("/api/filters/fl_1");
    }

    @Test
    void testReportsEachCondition() {
        server.enqueue(200, "{\"matches\":true,\"results\":[{\"field\":\"type\",\"operator\":\"equals\",\"value\":\"push\","
                + "\"fieldValue\":\"push\",\"passed\":true}],\"logic\":\"AND\"}");
        FilterTestResult r = filters.test(new TestFilterParams()
                .conditions(List.of(Map.of("field", "type", "operator", "equals", "value", "push")))
                .payload(Map.of("type", "push")));
        assertThat(server.last().path()).isEqualTo("/api/filters/test");
        assertThat(server.last().body()).isEqualTo(MockServer.json(
                "{\"conditions\":[{\"field\":\"type\",\"operator\":\"equals\",\"value\":\"push\"}],\"payload\":{\"type\":\"push\"}}"));
        assertThat(r.matches()).isTrue();
        assertThat(r.results().getFirst().passed()).isTrue();
        assertThat(r.results().getFirst().fieldValue()).isEqualTo(TextNode.valueOf("push"));
    }
}
