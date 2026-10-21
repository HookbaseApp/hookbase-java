package app.hookbase.models;

import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.internal.Json;
import org.junit.jupiter.api.Test;

/** Logging a model must never write a credential. */
class RedactionTest {

    private static <T> T read(String json, Class<T> type) throws Exception {
        return Json.MAPPER.readValue(json, type);
    }

    @Test
    void secretsAreRedacted() throws Exception {
        String s = read("{\"id\":\"k1\",\"key\":\"whr_supersecret\",\"keyPrefix\":\"whr_sup\"}", ApiKeyWithSecret.class).toString();
        assertThat(s).doesNotContain("supersecret").contains("key=<redacted>").contains("keyPrefix=whr_sup").startsWith("ApiKeyWithSecret[");

        assertThat(read("{\"id\":\"s1\",\"signingSecret\":\"whsec_abc\",\"verifyToken\":\"vt_1\"}", SourceWithSecret.class).toString())
                .doesNotContain("whsec_abc").doesNotContain("vt_1");
        assertThat(read("{\"id\":\"e1\",\"secret\":\"whsec_xyz\"}", Endpoint.class).toString()).doesNotContain("whsec_xyz");
        assertThat(read("{\"secret\":\"whsec_new\",\"secretVersion\":2}", EndpointRotateSecretResult.class).toString())
                .doesNotContain("whsec_new").contains("secretVersion=2");
        assertThat(read("{\"id\":\"t1\",\"token\":\"pt_live\"}", PortalToken.class).toString()).doesNotContain("pt_live");
        assertThat(read("{\"id\":\"t1\",\"auth_token\":\"tun_tok\"}", Tunnel.class).toString()).doesNotContain("tun_tok");
        assertThat(read("{\"curlCommand\":\"curl -H 'Authorization: Bearer x'\"}", EventDebugCommands.class).toString())
                .doesNotContain("Bearer");
    }

    @Test
    void nullSecretsPrintAsNull() throws Exception {
        assertThat(read("{\"id\":\"e1\"}", Endpoint.class).toString()).contains("secret=null");
    }

    @Test
    void credentialHeadersAreMaskedOthersKept() throws Exception {
        String s = read("{\"id\":\"d1\",\"headers\":{\"Authorization\":\"Bearer abc\",\"X-Tenant\":\"acme\"},"
                + "\"authConfig\":{\"token\":\"t0p\"},\"config\":{\"password\":\"pw\"}}", Destination.class).toString();
        assertThat(s).doesNotContain("Bearer abc").doesNotContain("t0p").doesNotContain("=pw")
                .contains("X-Tenant=acme").contains("token=<redacted>");

        assertThat(read("{\"headers\":{\"webhook-signature\":\"v1,sig\",\"content-type\":\"application/json\"}}", EventDebugRequest.class).toString())
                .doesNotContain("v1,sig").contains("content-type=application/json");
        assertThat(new EndpointHeader("X-Api-Key", "k123").toString()).doesNotContain("k123");
        assertThat(new EndpointHeader("X-Tenant", "acme").toString()).contains("acme");
    }

    @Test
    void equalityIsUnchanged() throws Exception {
        String json = "{\"id\":\"k1\",\"key\":\"whr_a\"}";
        assertThat(read(json, ApiKeyWithSecret.class)).isEqualTo(read(json, ApiKeyWithSecret.class));
        assertThat(read(json, ApiKeyWithSecret.class).key()).isEqualTo("whr_a");
    }
}
