package app.hookbase.webhooks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.hookbase.errors.WebhookVerificationException;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Fixed vectors produced outside the JVM, with Node's crypto signing exactly as the API does
 * (api/src/utils/outbound-signature.ts: HMAC-SHA256 over the raw key bytes, and WebCrypto Ed25519
 * with the {@code whsk_} seed and public key imported as a JWK), over a payload with non-ASCII
 * characters. They prove the Java verifier agrees with the server byte for byte, not only with
 * itself.
 */
class WebhookInteropTest {

    private static final String ID = "msg_2wXinterop";
    private static final long TS = 1761200000L;
    private static final String PAYLOAD = "{\"type\":\"order.created\",\"data\":{\"id\":\"ord_1\",\"name\":\"Zoë – ✓\"}}";
    private static final String SECRET = "whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=";
    private static final String V1 = "v1,OWY1BWqvlFHUAcjNs0oHwPuoXLOUkebDsQrkw5CYrdk=";
    private static final String WHSK = "whsk_BwcHBwcHBwcHBwcHBwcHBwcHBwcHBwcHBwcHBwcHBwfqSmxj4pxSCr71UHsTLsX5lUd2rr6+e5JCHuppFEbSLA==";
    private static final String WHPK = "whpk_6kpsY+KcUgq+9VB7Ey7F+ZVHdq6+vnuSQh7qaRRG0iw=";
    private static final String V1A = "v1a,+LeT7GHv45PE4raXEMsr6igT2dVYkdErjptlgIHwyFPt4XEtP6RxLhJybOKTUo66QdkuRHxMGcicqPmPKc4uCQ==";

    private static final Clock AT_TS = Clock.fixed(Instant.ofEpochSecond(TS + 10), ZoneOffset.UTC);

    private static Map<String, String> headers(String signature) {
        return Map.of("Webhook-Id", ID, "Webhook-Timestamp", Long.toString(TS), "Webhook-Signature", signature);
    }

    @Test
    void hmacVectorFromTheServerSignerVerifies() {
        JsonNode event = new Webhook(SECRET, AT_TS).verify(PAYLOAD.getBytes(StandardCharsets.UTF_8), headers(V1));
        assertThat(event.path("data").path("name").asText()).isEqualTo("Zoë – ✓");
    }

    @Test
    void javaSignsTheHmacVectorIdentically() {
        assertThat(new Webhook(SECRET).sign(ID, TS, PAYLOAD)).isEqualTo(V1);
        // The same key as legacy hex signs the same way (the API decodes both to the same bytes).
        assertThat(new Webhook("whsec_000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f").sign(ID, TS, PAYLOAD))
                .isEqualTo(V1);
    }

    @Test
    void ed25519VectorFromTheServerSignerVerifiesWithThePublicKey() {
        Webhook verifier = new Webhook(WHPK, AT_TS);
        assertThat(verifier.signatureScheme()).isEqualTo(Webhook.SCHEME_ED25519);
        assertThat(verifier.verify(PAYLOAD.getBytes(StandardCharsets.UTF_8), headers(V1A)).path("type").asText())
                .isEqualTo("order.created");
    }

    @Test
    void javaSignsTheEd25519VectorIdentically() {
        // Ed25519 is deterministic, so the same seed must give the server's exact signature.
        assertThat(new Webhook(WHSK).sign(ID, TS, PAYLOAD)).isEqualTo(V1A);
    }

    @Test
    void graceWindowHeaderVerifiesWithEitherKeyOfItsScheme() {
        String previous = new Webhook("whsec_" + java.util.Base64.getEncoder().encodeToString(new byte[32])).sign(ID, TS, PAYLOAD);
        String dual = previous + " " + V1; // previous first, as the API sends it
        new Webhook(SECRET, AT_TS).verifyHeaders(PAYLOAD, headers(dual), Webhook.DEFAULT_TOLERANCE);

        // An HMAC header never verifies against an Ed25519 key, and the other way round.
        assertThatThrownBy(() -> new Webhook(WHPK, AT_TS).verifyHeaders(PAYLOAD, headers(V1), Duration.ofMinutes(5)))
                .isInstanceOf(WebhookVerificationException.class);
        assertThatThrownBy(() -> new Webhook(SECRET, AT_TS).verifyHeaders(PAYLOAD, headers(V1A), Duration.ofMinutes(5)))
                .isInstanceOf(WebhookVerificationException.class);
    }

    @Test
    void theVectorsExpireLikeAnyDelivery() {
        Clock late = Clock.fixed(Instant.ofEpochSecond(TS + 301), ZoneOffset.UTC);
        assertThatThrownBy(() -> new Webhook(SECRET, late).verify(PAYLOAD, headers(V1)))
                .isInstanceOf(WebhookVerificationException.class)
                .hasMessageContaining("tolerance");
        assertThatThrownBy(() -> new Webhook(WHPK, late).verify(PAYLOAD, headers(V1A)))
                .isInstanceOf(WebhookVerificationException.class)
                .hasMessageContaining("tolerance");
    }

    @Test
    void stringOverloadsAndTypeBinding() {
        record Order(String type) {}
        Webhook webhook = new Webhook(SECRET, AT_TS);
        assertThat(webhook.verify(PAYLOAD, headers(V1), Order.class).type()).isEqualTo("order.created");
        webhook.verifySignature(PAYLOAD, ID, Long.toString(TS), V1);
        assertThatThrownBy(() -> webhook.verifySignature(PAYLOAD + " ", ID, Long.toString(TS), V1))
                .isInstanceOf(WebhookVerificationException.class);
        // A body that is valid JSON but the wrong shape for the type is a verification failure, not a crash.
        record Strict(int type) {}
        assertThatThrownBy(() -> webhook.verify(PAYLOAD, headers(V1), Strict.class))
                .isInstanceOf(WebhookVerificationException.class)
                .hasMessageContaining("Strict");
    }

    @Test
    void secretKeyOfTheWrongLengthIsRejected() {
        assertThatThrownBy(() -> new Webhook("whsk_" + java.util.Base64.getEncoder().encodeToString(new byte[32])))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("64 bytes");
        assertThatThrownBy(() -> new Webhook("whpk_" + java.util.Base64.getEncoder().encodeToString(new byte[31])))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void generatedKeyPairsAreFreshAndRoundTrip() {
        Webhook.Ed25519KeyPair a = Webhook.generateEd25519KeyPair();
        Webhook.Ed25519KeyPair b = Webhook.generateEd25519KeyPair();
        assertThat(a.publicKey()).isNotEqualTo(b.publicKey()).startsWith("whpk_");
        assertThat(a.secretKey()).startsWith("whsk_");
        assertThat(a.toString()).doesNotContain(a.secretKey()).contains("<redacted>");
        Map<String, String> h = new Webhook(a.secretKey()).generateTestHeaders(PAYLOAD);
        assertThat(h.get("webhook-signature")).startsWith("v1a,");
        new Webhook(a.publicKey()).verify(PAYLOAD, h);
        assertThatThrownBy(() -> new Webhook(b.publicKey()).verify(PAYLOAD, h)).isInstanceOf(WebhookVerificationException.class);
    }
}
