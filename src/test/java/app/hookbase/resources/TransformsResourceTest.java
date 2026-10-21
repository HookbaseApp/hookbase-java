package app.hookbase.resources;

import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.Page;
import app.hookbase.models.Transform;
import app.hookbase.models.TransformTestResult;
import app.hookbase.params.CreateTransformParams;
import app.hookbase.params.ListTransformsParams;
import app.hookbase.params.TestTransformParams;
import app.hookbase.params.UpdateTransformParams;
import app.hookbase.testing.MockServer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransformsResourceTest {

    static final String TRANSFORM = """
            {"id":"tr_1","organizationId":"org_1","name":"Strip","slug":"strip","description":null,"code":"$",
             "transformType":"jsonata","inputFormat":"json","outputFormat":"xml","isActive":true,
             "createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-02T00:00:00Z"}""";

    private MockServer server;
    private TransformsResource transforms;

    @BeforeEach
    void start() {
        server = new MockServer();
        transforms = new TransformsResource(DataAdminTestSupport.transport(server));
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void listIsPageNumbered() {
        server.enqueue(200, "{\"transforms\":[" + TRANSFORM + "],\"pagination\":{\"total\":5,\"page\":2,\"pageSize\":2}}");
        Page<Transform> page = transforms.list(new ListTransformsParams().page(2).pageSize(2));
        assertThat(server.last().path()).isEqualTo("/api/transforms");
        assertThat(server.last().query()).containsExactlyInAnyOrderEntriesOf(Map.of("page", "2", "pageSize", "2"));
        assertThat(page.total()).isEqualTo(5);
        assertThat(page.offset()).isEqualTo(2);
        assertThat(page.limit()).isEqualTo(2);
        assertThat(page.hasMore()).isTrue();
        Transform t = page.data().getFirst();
        assertThat(t.transformType()).isEqualTo("jsonata");
        assertThat(t.outputFormat()).isEqualTo("xml");
        assertThat(t.isActive()).isTrue();
    }

    @Test
    void listAllWalksPagesKeepingPageSize() {
        server.enqueue(200, "{\"transforms\":[" + TRANSFORM + "," + TRANSFORM + "],\"pagination\":{\"total\":3,\"page\":1,\"pageSize\":2}}");
        server.enqueue(200, "{\"transforms\":[" + TRANSFORM + "],\"pagination\":{\"total\":3,\"page\":2,\"pageSize\":2}}");
        List<Transform> all = transforms.listAll(new ListTransformsParams().pageSize(2)).stream().toList();
        assertThat(all).hasSize(3);
        assertThat(server.requests().get(0).query()).containsExactlyInAnyOrderEntriesOf(Map.of("page", "1", "pageSize", "2"));
        assertThat(server.requests().get(1).query()).containsEntry("page", "2");
    }

    @Test
    void listAllStartsAtCallersPage() {
        server.enqueue(200, "{\"transforms\":[" + TRANSFORM + "],\"pagination\":{\"total\":21,\"page\":3,\"pageSize\":10}}");
        assertThat(transforms.listAll(new ListTransformsParams().page(3).pageSize(10)).stream().toList()).hasSize(1);
        assertThat(server.last().query()).containsEntry("page", "3");
    }

    @Test
    void crudPathsBodiesAndSnakeCaseCreateEcho() {
        server.enqueue(200, "{\"transform\":" + TRANSFORM + "}");
        assertThat(transforms.get("strip").slug()).isEqualTo("strip");
        assertThat(server.last().path()).isEqualTo("/api/transforms/strip");

        // POST echoes transform_type/input_format/output_format in snake_case.
        server.enqueue(200, "{\"transform\":{\"id\":\"tr_new\",\"name\":\"N\",\"slug\":\"n\",\"code\":\"$\","
                + "\"transform_type\":\"liquid\",\"input_format\":\"json\",\"output_format\":\"text\"}}");
        Transform created = transforms.create(new CreateTransformParams().name("N").transformType("liquid").code("$")
                .inputFormat("json").outputFormat("text"));
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().body()).isEqualTo(MockServer.json(
                "{\"name\":\"N\",\"transformType\":\"liquid\",\"code\":\"$\",\"inputFormat\":\"json\",\"outputFormat\":\"text\"}"));
        assertThat(created.transformType()).isEqualTo("liquid");
        assertThat(created.outputFormat()).isEqualTo("text");

        server.enqueue(200, "{\"success\":true}");
        transforms.update("tr_1", new UpdateTransformParams().code("$.a").description(null));
        assertThat(server.last().method()).isEqualTo("PATCH");
        assertThat(server.last().path()).isEqualTo("/api/transforms/tr_1");
        assertThat(server.last().rawBody()).isEqualTo("{\"code\":\"$.a\",\"description\":null}");

        server.enqueue(204, null);
        transforms.delete("tr_1");
        assertThat(server.last().method()).isEqualTo("DELETE");
        assertThat(server.last().path()).isEqualTo("/api/transforms/tr_1");
    }

    @Test
    void testReturnsResultAndContentType() {
        server.enqueue(200, "{\"success\":true,\"result\":{\"transformed\":true},\"contentType\":\"application/json\"}");
        TransformTestResult r = transforms.test(new TestTransformParams().transformType("jsonata").code("$.data")
                .payload(Map.of("data", Map.of("transformed", true))));
        assertThat(server.last().path()).isEqualTo("/api/transforms/test");
        assertThat(server.last().body()).isEqualTo(MockServer.json(
                "{\"transformType\":\"jsonata\",\"code\":\"$.data\",\"payload\":{\"data\":{\"transformed\":true}}}"));
        assertThat(r.success()).isTrue();
        assertThat(r.result().get("transformed").asBoolean()).isTrue();
        assertThat(r.contentType()).isEqualTo("application/json");

        server.enqueue(200, "{\"success\":false,\"error\":\"Unexpected token\"}");
        TransformTestResult failed = transforms.test(new TestTransformParams().code("$(").payload("raw"));
        assertThat(failed.success()).isFalse();
        assertThat(failed.error()).isEqualTo("Unexpected token");
        assertThat(failed.result()).isNull();
    }
}
