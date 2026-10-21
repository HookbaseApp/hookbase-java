package app.hookbase.resources;

import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.models.CronJob;
import app.hookbase.models.CronJobExecution;
import app.hookbase.models.Delivery;
import app.hookbase.models.Event;
import app.hookbase.models.EventDetail;
import app.hookbase.models.FilterTestResult;
import app.hookbase.models.Schema;
import app.hookbase.models.SchemaValidationResult;
import app.hookbase.models.Transform;
import app.hookbase.models.TransformTestResult;
import app.hookbase.params.CreateApiKeyParams;
import app.hookbase.params.CreateCronJobParams;
import app.hookbase.params.CreateFilterParams;
import app.hookbase.params.CreateSchemaParams;
import app.hookbase.params.CreateTransformParams;
import app.hookbase.params.ListEventsParams;
import app.hookbase.params.TestFilterParams;
import app.hookbase.params.TestTransformParams;
import app.hookbase.params.UpdateCronJobParams;
import app.hookbase.testing.MockServer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for the inbound-data and admin resources, ported from node-sdk
 * ({@code inbound-resources.test.ts}, {@code cron-jobs.test.ts}), go-sdk ({@code apikeys_fields_test.go})
 * and python-sdk ({@code tests/resources/test_cron_jobs.py}).
 */
class DataAdminContractTest {

    private MockServer server;

    @BeforeEach
    void start() {
        server = new MockServer();
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Nested
    class Events {
        EventsResource events() {
            return new EventsResource(DataAdminTestSupport.transport(server));
        }

        @Test
        void listsWithKeysetPagination() {
            server.enqueue(200, "{\"events\":[{\"id\":\"evt_1\",\"eventType\":\"push\",\"status\":\"delivered\"},"
                    + "{\"id\":\"evt_2\",\"eventType\":\"push\",\"status\":\"pending\"}],\"total\":100,\"limit\":50,"
                    + "\"hasMore\":true,\"nextCursor\":\"c2\"}");
            var page = events().list(new ListEventsParams().limit(50));
            assertThat(page.data()).hasSize(2);
            assertThat(page.total()).isEqualTo(100);
            assertThat(page.hasMore()).isTrue();
        }

        @Test
        void getsDetailWithDeliveries() {
            server.enqueue(200, "{\"event\":{\"id\":\"evt_1\",\"eventType\":\"push\"},\"payload\":{\"ref\":\"main\"},"
                    + "\"deliveries\":[{\"id\":\"del_1\",\"status\":\"delivered\",\"destinationName\":\"Slack\"}]}");
            EventDetail d = events().get("evt_1");
            assertThat(d.id()).isEqualTo("evt_1");
            assertThat(d.deliveries()).hasSize(1);
            assertThat(d.payload().get("ref").asText()).isEqualTo("main");
        }

        @Test
        void getsDebugCurlCommand() {
            server.enqueue(200, "{\"event\":{\"id\":\"evt_1\",\"eventType\":\"push\"},\"debug\":{\"curlCommand\":\"curl -X POST ...\"}}");
            assertThat(events().debug("evt_1").curlCommand()).isNotBlank();
        }

        @Test
        void iteratesAllEventsAcrossPages() {
            server.enqueue(200, "{\"events\":[{\"id\":\"evt_1\"},{\"id\":\"evt_2\"}],\"limit\":2,\"hasMore\":true,\"nextCursor\":\"c2\"}");
            server.enqueue(200, "{\"events\":[{\"id\":\"evt_3\"}],\"limit\":2,\"hasMore\":false,\"nextCursor\":null}");
            List<String> ids = events().listAll(new ListEventsParams().limit(2)).stream().map(Event::id).toList();
            assertThat(ids).containsExactly("evt_1", "evt_2", "evt_3");
            assertThat(server.requests()).hasSize(2);
        }
    }

    @Nested
    class Deliveries {
        DeliveriesResource deliveries() {
            return new DeliveriesResource(DataAdminTestSupport.transport(server));
        }

        @Test
        void listsGetsAndReplays() {
            server.enqueue(200, "{\"deliveries\":[{\"id\":\"del_1\",\"status\":\"delivered\"}],\"limit\":50,\"offset\":0}");
            assertThat(deliveries().list().data()).hasSize(1);

            server.enqueue(200, "{\"delivery\":{\"id\":\"del_1\",\"status\":\"delivered\",\"responseStatus\":200}}");
            Delivery d = deliveries().get("del_1");
            assertThat(d.id()).isEqualTo("del_1");
            assertThat(d.responseStatus()).isEqualTo(200);

            server.enqueue(200, "{\"deliveryId\":\"del_new\",\"message\":\"Replay queued\"}");
            assertThat(deliveries().replay("del_1").deliveryId()).isEqualTo("del_new");

            server.enqueue(200, "{\"message\":\"3 deliveries queued for replay\",\"queued\":3,\"skipped\":0,\"results\":[]}");
            assertThat(deliveries().bulkReplay(List.of("del_1", "del_2", "del_3")).queued()).isEqualTo(3);
            assertThat(server.last().method()).isEqualTo("POST");
            assertThat(server.last().path()).isEqualTo("/api/deliveries/bulk-replay");

            server.enqueue(200, "{\"message\":\"2 failed deliveries queued for replay\",\"queued\":2,\"skipped\":1,\"results\":[]}");
            var r = deliveries().bulkReplayEvents(List.of("evt_1", "evt_2"));
            assertThat(r.queued()).isEqualTo(2);
            assertThat(r.skipped()).isEqualTo(1);
        }
    }

    @Nested
    class Transforms {
        TransformsResource transforms() {
            return new TransformsResource(DataAdminTestSupport.transport(server));
        }

        @Test
        void listGetCreateTestDelete() {
            server.enqueue(200, "{\"transforms\":[{\"id\":\"tr_1\",\"name\":\"Strip headers\"}],\"pagination\":{\"total\":1,\"page\":1,\"pageSize\":20}}");
            assertThat(transforms().list().data()).hasSize(1);

            server.enqueue(200, "{\"transform\":{\"id\":\"tr_1\",\"name\":\"Strip headers\",\"code\":\"$\"}}");
            assertThat(transforms().get("tr_1").name()).isEqualTo("Strip headers");

            server.enqueue(200, "{\"transform\":{\"id\":\"tr_new\",\"name\":\"New Transform\",\"transform_type\":\"jsonata\",\"code\":\"$\"}}");
            Transform t = transforms().create(new CreateTransformParams().name("New Transform").transformType("jsonata").code("$"));
            assertThat(t.id()).isEqualTo("tr_new");

            server.enqueue(200, "{\"success\":true,\"result\":{\"transformed\":true},\"contentType\":\"application/json\"}");
            TransformTestResult r = transforms().test(new TestTransformParams().transformType("jsonata").code("$.data")
                    .payload(Map.of("data", Map.of("transformed", true))));
            assertThat(r.success()).isTrue();
            assertThat(r.result()).isEqualTo(MockServer.json("{\"transformed\":true}"));

            server.enqueue(204, null);
            transforms().delete("tr_1");
            assertThat(server.last().method()).isEqualTo("DELETE");
            assertThat(server.last().path()).isEqualTo("/api/transforms/tr_1");
        }
    }

    @Nested
    class Filters {
        FiltersResource filters() {
            return new FiltersResource(DataAdminTestSupport.transport(server));
        }

        @Test
        void listGetCreateTest() {
            server.enqueue(200, "{\"filters\":[{\"id\":\"fl_1\",\"name\":\"Only pushes\"}],\"pagination\":{\"total\":1,\"page\":1,\"pageSize\":20}}");
            assertThat(filters().list().data()).hasSize(1);

            server.enqueue(200, "{\"filter\":{\"id\":\"fl_1\",\"conditions\":[{\"field\":\"type\",\"operator\":\"equals\",\"value\":\"push\"}]}}");
            assertThat(filters().get("fl_1").conditions()).hasSize(1);

            server.enqueue(200, "{\"filter\":{\"id\":\"fl_new\",\"name\":\"New Filter\",\"logic\":\"AND\"}}");
            assertThat(filters().create(new CreateFilterParams().name("New Filter")
                    .conditions(List.of(Map.of("field", "type", "operator", "equals", "value", "push")))).id()).isEqualTo("fl_new");

            server.enqueue(200, "{\"matches\":true,\"results\":[{\"passed\":true}],\"logic\":\"AND\"}");
            FilterTestResult r = filters().test(new TestFilterParams()
                    .conditions(List.of(Map.of("field", "type", "operator", "equals", "value", "push")))
                    .payload(Map.of("type", "push")));
            assertThat(r.matches()).isTrue();
            assertThat(r.results()).hasSize(1);
        }
    }

    @Nested
    class Schemas {
        SchemasResource schemas() {
            return new SchemasResource(DataAdminTestSupport.transport(server));
        }

        @Test
        void listGetCreateValidate() {
            server.enqueue(200, "{\"schemas\":[{\"id\":\"sch_1\",\"name\":\"Webhook Event\"}]}");
            assertThat(schemas().list()).hasSize(1);

            server.enqueue(200, "{\"schema\":{\"id\":\"sch_1\",\"name\":\"Webhook Event\",\"jsonSchema\":{\"type\":\"object\"}}}");
            assertThat(schemas().get("sch_1").name()).isEqualTo("Webhook Event");

            server.enqueue(201, "{\"schema\":{\"id\":\"sch_new\",\"name\":\"New Schema\"}}");
            Schema s = schemas().create(new CreateSchemaParams().name("New Schema")
                    .jsonSchema(Map.of("type", "object", "properties", Map.of("event", Map.of("type", "string")))));
            assertThat(s.name()).isEqualTo("New Schema");

            server.enqueue(200, "{\"valid\":false,\"errors\":[{\"path\":\"event\",\"message\":\"Missing required field: event\"}]}");
            SchemaValidationResult r = schemas().validate("sch_1", Map.of("foo", "bar"));
            assertThat(r.valid()).isFalse();
            assertThat(r.errors()).hasSize(1);
        }
    }

    /** go-sdk apikeys_fields_test.go: what POST /api/api-keys reads, and so what must leave the process. */
    @Nested
    class ApiKeyCreateBody {
        static final Set<String> FIELDS_THE_API_READS = Set.of("name", "scopes", "expiresIn");

        JsonNode capture(CreateApiKeyParams params) {
            server.enqueue(200, "{\"apiKey\":{\"id\":\"11111111-1111-4111-8111-111111111111\",\"key\":\"whr_4f3c2a1b9d8e7f6a5b4c3d2e1f0a9b8c\"}}");
            new ApiKeysResource(DataAdminTestSupport.transport(server)).create(params);
            JsonNode body = server.last().body();
            assertThat(body).as("request body").isNotNull();
            return body;
        }

        @Test
        void convertsExpiresInDaysToSeconds() {
            JsonNode body = capture(new CreateApiKeyParams().name("CI/CD Pipeline").scopes(List.of("read", "write")).expiresInDays(90));
            assertThat(body.has("expiresIn")).isTrue();
            assertThat(body.get("expiresIn").asLong()).isEqualTo(90L * 24 * 60 * 60);
        }

        @Test
        void neverSendsExpiresInDays() {
            JsonNode body = capture(new CreateApiKeyParams().name("CI").expiresInDays(1));
            assertThat(body.has("expiresInDays")).isFalse();
            assertThat(body.get("expiresIn").asLong()).isEqualTo(86400L);
        }

        @Test
        void omitsExpiryWhenNoneRequested() {
            JsonNode body = capture(new CreateApiKeyParams().name("Permanent"));
            assertThat(body.has("expiresIn")).isFalse();
            assertThat(body.get("name").asText()).isEqualTo("Permanent");
        }

        @Test
        void bodyHasOnlyFieldsTheApiReads() {
            JsonNode body = capture(new CreateApiKeyParams().name("Full").scopes(List.of("read", "write", "delete")).expiresInDays(30));
            List<String> keys = new ArrayList<>();
            for (Iterator<String> it = body.fieldNames(); it.hasNext(); ) keys.add(it.next());
            assertThat(FIELDS_THE_API_READS).containsAll(keys);
        }
    }

    /** node-sdk cron-jobs.test.ts and python-sdk test_cron_jobs.py. */
    @Nested
    class CronJobs {
        static final String JOB = """
                {"id":"cron_1","organizationId":"org_1","groupId":null,"name":"Nightly sync","description":null,
                 "cronExpression":"0 0 * * *","timezone":"UTC","url":"https://example.com/sync","method":"POST",
                 "headers":null,"payload":null,"timeoutMs":30000,"useStaticIp":false,"isActive":true,
                 "notifyOnFailure":true,"notifyOnSuccess":false,"notifyEmails":null,"consecutiveFailures":0,
                 "lastRunAt":"2026-09-01T00:00:00Z","nextRunAt":"2026-09-02T00:00:00Z",
                 "createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-09-01T00:00:00Z"}""";

        CronJobsResource cron() {
            return new CronJobsResource(DataAdminTestSupport.transport(server));
        }

        @Test
        void listUsesRealFieldNames() {
            server.enqueue(200, "{\"cronJobs\":[" + JOB + "]}");
            var page = cron().list();
            assertThat(page).hasSize(1);
            CronJob job = page.getFirst();
            assertThat(job.cronExpression()).isEqualTo("0 0 * * *");
            assertThat(job.nextRunAt()).isEqualTo("2026-09-02T00:00:00Z");
            assertThat(job.lastRunAt()).isEqualTo("2026-09-01T00:00:00Z");
            assertThat(job.consecutiveFailures()).isZero();
            assertThat(job.payload()).isNull();
        }

        @Test
        void getUsesRealFieldNames() {
            server.enqueue(200, "{\"cronJob\":" + JOB + "}");
            CronJob job = cron().get("cron_1");
            assertThat(server.last().path()).isEqualTo("/api/cron/cron_1");
            assertThat(job.cronExpression()).isEqualTo("0 0 * * *");
            assertThat(job.timeoutMs()).isEqualTo(30000);
            assertThat(job.notifyOnFailure()).isTrue();
            assertThat(job.notifyOnSuccess()).isFalse();
            assertThat(job.consecutiveFailures()).isZero();
        }

        @Test
        void createSendsPayloadUnderTheKeyTheApiReads() {
            server.enqueue(200, "{\"cronJob\":" + JOB + "}");
            CronJob job = cron().create(new CreateCronJobParams().name("Nightly sync").url("https://example.com/sync")
                    .cronExpression("0 0 * * *").payload("{\"foo\": \"bar\"}"));
            JsonNode body = server.last().body();
            assertThat(body.get("payload").asText()).isEqualTo("{\"foo\": \"bar\"}");
            assertThat(body.has("body")).isFalse();
            assertThat(job.cronExpression()).isEqualTo("0 0 * * *");
        }

        @Test
        void updateAndDelete() {
            server.enqueue(200, "{\"success\":true}");
            cron().update("cron_1", new UpdateCronJobParams().cronExpression("*/5 * * * *").payload("{}"));
            assertThat(server.last().method()).isEqualTo("PATCH");
            assertThat(server.last().body()).isEqualTo(MockServer.json("{\"cronExpression\":\"*/5 * * * *\",\"payload\":\"{}\"}"));

            server.enqueue(200, "{\"success\":true}");
            cron().delete("cron_1");
            assertThat(server.last().method()).isEqualTo("DELETE");
            assertThat(server.last().path()).isEqualTo("/api/cron/cron_1");
        }

        @Test
        void triggerReturnsExecution() {
            server.enqueue(200, "{\"execution\":{\"id\":\"exec_1\",\"status\":\"success\",\"responseStatus\":200,\"latencyMs\":142}}");
            CronJobExecution r = cron().trigger("cron_1");
            assertThat(r).isEqualTo(new CronJobExecution("exec_1", "success", 200, 142));
            assertThat(server.last().method()).isEqualTo("POST");
            assertThat(server.last().path()).isEqualTo("/api/cron/cron_1/trigger");
        }

        @Test
        void triggerFailedExecution() {
            server.enqueue(200, "{\"execution\":{\"id\":\"exec_2\",\"status\":\"failed\",\"responseStatus\":null,\"latencyMs\":30000}}");
            CronJobExecution r = cron().trigger("cron_1");
            assertThat(r.status()).isEqualTo("failed");
            assertThat(r.responseStatus()).isNull();
        }
    }
}
