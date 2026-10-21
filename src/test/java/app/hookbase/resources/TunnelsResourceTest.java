package app.hookbase.resources;

import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.models.Tunnel;
import app.hookbase.models.TunnelBulkDeleteResult;
import app.hookbase.params.CreateTunnelParams;
import app.hookbase.testing.MockServer;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TunnelsResourceTest {

    static final String TUNNEL = """
            {"id":"tun_1","organizationId":"org_1","userId":"usr_1","name":"dev","subdomain":"brave-otter-1a2b",
             "localPort":3000,"status":"connected","authToken":null,"createdAt":"2026-01-01T00:00:00Z",
             "lastConnectedAt":"2026-01-02T00:00:00Z","lastRequestAt":null,"totalRequests":12,"customDomainId":null,
             "direction":"bidirectional","allowedHosts":"[\\"api.example.com\\"]"}""";

    private MockServer server;
    private TunnelsResource tunnels;

    @BeforeEach
    void start() {
        server = new MockServer();
        tunnels = new TunnelsResource(DataAdminTestSupport.transport(server));
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void list() {
        server.enqueue(200, "{\"tunnels\":[" + TUNNEL + "]}");
        List<Tunnel> page = tunnels.list();
        assertThat(server.last().path()).isEqualTo("/api/tunnels");
        Tunnel t = page.getFirst();
        assertThat(t.localPort()).isEqualTo(3000);
        assertThat(t.totalRequests()).isEqualTo(12);
        assertThat(t.allowedHosts()).containsExactly("api.example.com");
        assertThat(t.authToken()).isNull();
    }

    @Test
    void getAndCreate() {
        server.enqueue(200, "{\"tunnel\":" + TUNNEL + "}");
        assertThat(tunnels.get("tun_1").subdomain()).isEqualTo("brave-otter-1a2b");
        assertThat(server.last().path()).isEqualTo("/api/tunnels/tun_1");

        // Create sends the one-time token as auth_token.
        server.enqueue(201, "{\"tunnel\":{\"id\":\"tun_2\",\"name\":\"dev\",\"subdomain\":\"my-sub\",\"status\":\"disconnected\","
                + "\"allowedHosts\":null,\"auth_token\":\"tok_123\"},\"tunnelUrl\":\"https://x/t/my-sub\",\"wsUrl\":\"wss://x\"}");
        Tunnel created = tunnels.create(new CreateTunnelParams().name("dev").subdomain("my-sub").direction("inbound")
                .allowedHosts(List.of("a.example.com")));
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().body()).isEqualTo(MockServer.json(
                "{\"name\":\"dev\",\"subdomain\":\"my-sub\",\"direction\":\"inbound\",\"allowedHosts\":[\"a.example.com\"]}"));
        assertThat(created.authToken()).isEqualTo("tok_123");
        assertThat(created.allowedHosts()).isNull();
    }

    @Test
    void bulkDeleteSendsIdsInDeleteBody() {
        server.enqueue(200, "{\"success\":true,\"deleted\":2}");
        TunnelBulkDeleteResult r = tunnels.bulkDelete(List.of("tun_1", "tun_2"));
        assertThat(server.last().method()).isEqualTo("DELETE");
        assertThat(server.last().path()).isEqualTo("/api/tunnels/bulk");
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"ids\":[\"tun_1\",\"tun_2\"]}"));
        assertThat(r.success()).isTrue();
        assertThat(r.deleted()).isEqualTo(2);
    }
}
