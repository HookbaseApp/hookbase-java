package app.hookbase.resources;

import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.models.PortalToken;
import app.hookbase.params.CreatePortalTokenParams;
import app.hookbase.testing.MockServer;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PortalTokensResourceTest {

    static final String TOKEN = """
            {"id":"pt_1","applicationId":"app_1","token":"whpt_secret","tokenPrefix":"whpt_sec","name":"Dashboard",
             "scopes":["read","write"],"expiresAt":"2026-11-17T00:00:00Z","createdAt":"2026-10-17T00:00:00Z"}""";

    private MockServer server;
    private PortalTokensResource tokens;

    @BeforeEach
    void start() {
        server = new MockServer();
        tokens = new PortalTokensResource(OutboundTestSupport.transport(server));
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void createPostsTheBodyAndBindsTheToken() {
        server.enqueue(201, "{\"data\":" + TOKEN + "}");
        PortalToken t = tokens.create("app/1", new CreatePortalTokenParams().name("Dashboard").scopes(List.of("read"))
                .expiresInDays(7).allowedIps(List.of("203.0.113.1")));
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().path()).isEqualTo("/api/portal/webhook-applications/app%2F1/tokens");
        assertThat(server.last().body()).isEqualTo(MockServer.json(
                "{\"name\":\"Dashboard\",\"scopes\":[\"read\"],\"expiresInDays\":7,\"allowedIps\":[\"203.0.113.1\"]}"));
        assertThat(t.token()).isEqualTo("whpt_secret");
        assertThat(t.scopes()).containsExactly("read", "write");
        assertThat(t.isRevoked()).isNull();

        server.enqueue(201, "{\"data\":" + TOKEN + "}");
        tokens.create("app_1");
        assertThat(server.last().rawBody()).isEqualTo("{}");
    }

    @Test
    void listAndRevoke() {
        server.enqueue(200, "{\"data\":[" + TOKEN.replace("\"token\":\"whpt_secret\",", "\"isExpired\":false,\"isRevoked\":true,") + "]}");
        List<PortalToken> list = tokens.list("app_1");
        assertThat(server.last().method()).isEqualTo("GET");
        assertThat(server.last().path()).isEqualTo("/api/portal/webhook-applications/app_1/tokens");
        assertThat(list).hasSize(1);
        assertThat(list.getFirst().token()).isNull();
        assertThat(list.getFirst().isRevoked()).isTrue();
        assertThat(list.getFirst().isExpired()).isFalse();

        server.enqueue(200, "{\"success\":true}");
        tokens.revoke("app_1", "pt_1");
        assertThat(server.last().method()).isEqualTo("DELETE");
        assertThat(server.last().path()).isEqualTo("/api/portal/tokens/pt_1");
    }
}
