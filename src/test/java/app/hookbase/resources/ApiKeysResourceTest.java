package app.hookbase.resources;

import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.Page;
import app.hookbase.models.ApiKey;
import app.hookbase.models.ApiKeyWithSecret;
import app.hookbase.params.CreateApiKeyParams;
import app.hookbase.testing.MockServer;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiKeysResourceTest {

    private MockServer server;
    private ApiKeysResource apiKeys;

    @BeforeEach
    void start() {
        server = new MockServer();
        apiKeys = new ApiKeysResource(DataAdminTestSupport.transport(server));
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void listParsesScopesStoredAsText() {
        server.enqueue(200, "{\"apiKeys\":[{\"id\":\"key_1\",\"organizationId\":\"org_1\",\"name\":\"CI\",\"keyPrefix\":\"whr_abcdefgh\","
                + "\"scopes\":\"[\\\"read\\\",\\\"write\\\"]\",\"lastUsedAt\":null,\"expiresAt\":null,\"createdBy\":\"usr_1\","
                + "\"createdAt\":\"2026-01-01T00:00:00Z\"}]}");
        Page<ApiKey> page = apiKeys.list();
        assertThat(server.last().method()).isEqualTo("GET");
        assertThat(server.last().path()).isEqualTo("/api/api-keys");
        ApiKey key = page.data().getFirst();
        assertThat(key.scopes()).containsExactly("read", "write");
        assertThat(key.keyPrefix()).isEqualTo("whr_abcdefgh");
        assertThat(key.expiresAt()).isNull();
        assertThat(page.hasMore()).isFalse();

        server.enqueue(200, "{\"data\":[{\"id\":\"key_2\",\"scopes\":[\"read\"]}]}");
        assertThat(apiKeys.listAll().stream().toList()).extracting(ApiKey::id).containsExactly("key_2");
    }

    @Test
    void createUnwrapsApiKeyWithSecret() {
        server.enqueue(200, "{\"apiKey\":{\"id\":\"key_1\",\"name\":\"CI\",\"key\":\"whr_secret\",\"keyPrefix\":\"whr_secret\","
                + "\"scopes\":[\"read\",\"write\"],\"expiresAt\":\"2026-04-01T00:00:00Z\"}}");
        ApiKeyWithSecret created = apiKeys.create(new CreateApiKeyParams().name("CI").scopes(List.of("read", "write")).expiresInDays(90));
        assertThat(server.last().method()).isEqualTo("POST");
        assertThat(server.last().body()).isEqualTo(MockServer.json(
                "{\"name\":\"CI\",\"scopes\":[\"read\",\"write\"],\"expiresIn\":7776000}"));
        assertThat(created.key()).isEqualTo("whr_secret");
        assertThat(created.scopes()).containsExactly("read", "write");
        assertThat(created.expiresAt()).isEqualTo("2026-04-01T00:00:00Z");
    }

    @Test
    void createPicksUpASecretSentBesideTheKey() {
        server.enqueue(200, "{\"apiKey\":{\"id\":\"key_1\"},\"key\":\"whr_4f3c\"}");
        assertThat(apiKeys.create(new CreateApiKeyParams().name("CI")).key()).isEqualTo("whr_4f3c");
    }

    @Test
    void deleteEncodesId() {
        server.enqueue(200, "{\"success\":true}");
        apiKeys.delete("key/1");
        assertThat(server.last().method()).isEqualTo("DELETE");
        assertThat(server.last().path()).isEqualTo("/api/api-keys/key%2F1");
    }
}
