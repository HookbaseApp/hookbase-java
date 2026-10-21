package app.hookbase.resources;

import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.Page;
import app.hookbase.models.CronGroup;
import app.hookbase.models.CronJob;
import app.hookbase.models.CronJobExecution;
import app.hookbase.params.CreateCronGroupParams;
import app.hookbase.params.CreateCronJobParams;
import app.hookbase.params.UpdateCronJobParams;
import app.hookbase.testing.MockServer;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CronJobsResourceTest {

    static final String JOB = """
            {"id":"cron_1","organizationId":"org_1","groupId":null,"name":"Nightly sync","description":null,
             "cronExpression":"0 0 * * *","timezone":"UTC","url":"https://example.com/sync","method":"POST",
             "headers":"{\\"Authorization\\":\\"****\\"}","payload":null,"timeoutMs":30000,"useStaticIp":true,
             "isActive":true,"notifyOnFailure":true,"notifyOnSuccess":false,"notifyEmails":null,"consecutiveFailures":0,
             "lastRunAt":"2026-09-01T00:00:00Z","nextRunAt":"2026-09-02T00:00:00Z","createdAt":"2026-01-01T00:00:00Z",
             "updatedAt":"2026-09-01T00:00:00Z"}""";

    private MockServer server;
    private CronJobsResource cron;

    @BeforeEach
    void start() {
        server = new MockServer();
        cron = new CronJobsResource(DataAdminTestSupport.transport(server));
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void listAndListAll() {
        server.enqueue(200, "{\"cronJobs\":[" + JOB + "]}");
        Page<CronJob> page = cron.list();
        assertThat(server.last().path()).isEqualTo("/api/cron");
        assertThat(server.last().query()).isEmpty();
        CronJob job = page.data().getFirst();
        assertThat(job.useStaticIp()).isTrue();
        assertThat(job.headers()).isEqualTo("{\"Authorization\":\"****\"}");
        assertThat(page.hasMore()).isFalse();

        server.enqueue(200, "{\"cronJobs\":[" + JOB + "," + JOB + "]}");
        assertThat(cron.listAll().stream().toList()).hasSize(2);
        assertThat(server.requests()).hasSize(2);
    }

    @Test
    void crudBodies() {
        server.enqueue(200, "{\"cronJob\":" + JOB + "}");
        assertThat(cron.get("cron_1").timeoutMs()).isEqualTo(30000);
        assertThat(server.last().path()).isEqualTo("/api/cron/cron_1");

        server.enqueue(200, "{\"cronJob\":{\"id\":\"cron_2\",\"name\":\"Ping\",\"cronExpression\":\"*/5 * * * *\","
                + "\"url\":\"https://example.com\",\"nextRunAt\":\"2026-09-01T00:05:00Z\",\"groupId\":null,"
                + "\"notifyOnFailure\":true,\"notifyOnSuccess\":false,\"notifyEmails\":null}}");
        CronJob created = cron.create(new CreateCronJobParams().name("Ping").cronExpression("*/5 * * * *")
                .url("https://example.com").method("GET").headers(Map.of("X-Token", "t")).payload("{\"foo\":\"bar\"}")
                .timezone("Europe/London").useStaticIp(false).notifyOnFailure(true));
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"name\":\"Ping\",\"cronExpression\":\"*/5 * * * *\","
                + "\"url\":\"https://example.com\",\"method\":\"GET\",\"headers\":{\"X-Token\":\"t\"},"
                + "\"payload\":\"{\\\"foo\\\":\\\"bar\\\"}\",\"timezone\":\"Europe/London\",\"useStaticIp\":false,\"notifyOnFailure\":true}"));
        assertThat(created.nextRunAt()).isEqualTo("2026-09-01T00:05:00Z");

        server.enqueue(200, "{\"success\":true}");
        cron.update("cron_1", new UpdateCronJobParams().isActive(false).groupId(null));
        assertThat(server.last().method()).isEqualTo("PATCH");
        assertThat(server.last().path()).isEqualTo("/api/cron/cron_1");
        assertThat(server.last().rawBody()).isEqualTo("{\"isActive\":false,\"groupId\":null}");

        server.enqueue(200, "{\"success\":true}");
        cron.delete("cron_1");
        assertThat(server.last().method()).isEqualTo("DELETE");
        assertThat(server.last().path()).isEqualTo("/api/cron/cron_1");
    }

    @Test
    void trigger() {
        server.enqueue(200, "{\"execution\":{\"id\":\"exec_1\",\"status\":\"success\",\"responseStatus\":200,\"latencyMs\":142}}");
        CronJobExecution e = cron.trigger("cron_1");
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/cron/cron_1/trigger");
        assertThat(e.latencyMs()).isEqualTo(142);
    }

    @Test
    void groups() {
        server.enqueue(200, "{\"groups\":[{\"id\":\"grp_1\",\"name\":\"Billing\",\"slug\":\"billing\",\"isCollapsed\":false,"
                + "\"sortOrder\":2,\"jobCount\":4}]}");
        Page<CronGroup> page = cron.listGroups();
        assertThat(server.last().path()).isEqualTo("/api/cron-groups");
        assertThat(page.data().getFirst().jobCount()).isEqualTo(4);
        assertThat(page.data().getFirst().sortOrder()).isEqualTo(2);

        server.enqueue(200, "{\"groups\":[]}");
        assertThat(cron.listAllGroups().stream().toList()).isEmpty();

        server.enqueue(201, "{\"group\":{\"id\":\"grp_2\",\"name\":\"Ops\",\"description\":\"d\"}}");
        CronGroup g = cron.createGroup(new CreateCronGroupParams().name("Ops").description("d"));
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/cron-groups");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"name\":\"Ops\",\"description\":\"d\"}"));
        assertThat(g.id()).isEqualTo("grp_2");
    }
}
