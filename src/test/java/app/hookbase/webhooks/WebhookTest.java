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
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Ported from python-sdk/tests/test_webhook.py, which mirrors api/src/utils/outbound-signature.ts. */
class WebhookTest {

    private static final long NOW = 1_760_000_000L;
    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(NOW), ZoneOffset.UTC);
    private static final String SECRET_RAW = Base64.getEncoder().encodeToString("test-secret-key-for-webhook-sign".getBytes(StandardCharsets.UTF_8));
    private static final String SECRET = "whsec_" + SECRET_RAW;

    private static final byte[] KEY_BYTES = new byte[32];

    static {
        for (int i = 0; i < 32; i++) KEY_BYTES[i] = (byte) i;
    }

    private final Webhook wh = new Webhook(SECRET, CLOCK);

    /** An independent signer, as the API does it. */
    private static String apiSign(byte[] key, String id, long ts, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal((id + "." + ts + "." + body).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, String> headers(String id, String ts, String sig) {
        Map<String, String> h = new LinkedHashMap<>();
        if (id != null) h.put("webhook-id", id);
        if (ts != null) h.put("webhook-timestamp", ts);
        if (sig != null) h.put("webhook-signature", sig);
        return h;
    }

    @Test
    void verifiesValidSignature() {
        String payload = "{\"event\":\"test\",\"data\":{\"id\":1}}";
        JsonNode result = wh.verify(payload, wh.generateTestHeaders(payload));
        assertThat(result.get("event").asText()).isEqualTo("test");
        assertThat(result.at("/data/id").asInt()).isEqualTo(1);
    }

    @Test
    void verifiesWithoutPrefix() {
        Webhook bare = new Webhook(SECRET_RAW, CLOCK);
        String payload = "{\"ok\":true}";
        assertThat(bare.verify(payload, wh.generateTestHeaders(payload)).get("ok").asBoolean()).isTrue();
    }

    @Test
    void verifiesBytesPayload() {
        String payload = "{\"key\":\"value\"}";
        assertThat(wh.verify(payload.getBytes(StandardCharsets.UTF_8), wh.generateTestHeaders(payload)).get("key").asText()).isEqualTo("value");
    }

    @Test
    void bindsToType() {
        record Event(String type, int id) {}
        String payload = "{\"type\":\"order.created\",\"id\":42,\"extra\":true}";
        assertThat(wh.verify(payload, wh.generateTestHeaders(payload), Event.class)).isEqualTo(new Event("order.created", 42));
    }

    @Test
    void missingHeaders() {
        assertThatThrownBy(() -> wh.verify("{}", headers(null, "123", "v1,abc"))).isInstanceOf(WebhookVerificationException.class).hasMessageContaining("Missing webhook-id");
        assertThatThrownBy(() -> wh.verify("{}", headers("msg_1", null, "v1,abc"))).hasMessageContaining("Missing webhook-timestamp");
        assertThatThrownBy(() -> wh.verify("{}", headers("msg_1", "123", null))).hasMessageContaining("Missing webhook-signature");
    }

    @Test
    void invalidTimestamp() {
        assertThatThrownBy(() -> wh.verify("{}", headers("msg_1", "not-a-number", "v1,abc")))
                .isInstanceOf(WebhookVerificationException.class).hasMessageContaining("Invalid timestamp");
    }

    @Test
    void expiredAndFutureTimestamps() {
        String payload = "{\"old\":true}";
        assertThatThrownBy(() -> wh.verify(payload, wh.generateTestHeaders(payload, null, NOW - 600))).hasMessageContaining("outside tolerance");
        assertThatThrownBy(() -> wh.verify(payload, wh.generateTestHeaders(payload, null, NOW + 600))).hasMessageContaining("outside tolerance");
        // Exactly at the tolerance is still accepted.
        assertThat(wh.verify(payload, wh.generateTestHeaders(payload, null, NOW - 300))).isNotNull();
    }

    @Test
    void customTolerance() {
        String payload = "{\"data\":1}";
        Map<String, String> h = wh.generateTestHeaders(payload, null, NOW - 60);
        assertThat(wh.verify(payload, h, Duration.ofSeconds(120)).get("data").asInt()).isEqualTo(1);
        assertThatThrownBy(() -> wh.verify(payload, h, Duration.ofSeconds(30))).hasMessageContaining("outside tolerance");
    }

    @Test
    void badSignature() {
        assertThatThrownBy(() -> wh.verify("{\"data\":1}", headers("msg_test", Long.toString(NOW), "v1,aW52YWxpZHNpZ25hdHVyZQ==")))
                .isInstanceOf(WebhookVerificationException.class).hasMessageContaining("verification failed");
    }

    @Test
    void tamperedBodyFails() {
        String payload = "{\"amount\":1}";
        Map<String, String> h = wh.generateTestHeaders(payload);
        assertThatThrownBy(() -> wh.verify("{\"amount\":100}", h)).hasMessageContaining("verification failed");
    }

    @Test
    void noValidSignatures() {
        assertThatThrownBy(() -> wh.verify("{}", headers("msg_test", Long.toString(NOW), "invalid-format")))
                .hasMessageContaining("No valid signatures");
    }

    @Test
    void invalidJson() {
        String payload = "not valid json";
        assertThatThrownBy(() -> wh.verify(payload, wh.generateTestHeaders(payload))).hasMessageContaining("Invalid JSON");
    }

    @Test
    void caseInsensitiveHeaders() {
        String payload = "{\"ci\":true}";
        Map<String, String> mixed = new LinkedHashMap<>();
        wh.generateTestHeaders(payload).forEach((k, v) -> mixed.put(k.substring(0, 1).toUpperCase(Locale.ROOT) + k.substring(1), v));
        assertThat(wh.verify(payload, mixed).get("ci").asBoolean()).isTrue();
    }

    @Test
    void generateTestHeadersShapeAndOverrides() {
        Map<String, String> h = wh.generateTestHeaders("{}");
        assertThat(h).containsOnlyKeys("webhook-id", "webhook-timestamp", "webhook-signature");
        assertThat(h.get("webhook-id")).matches("msg_[A-Za-z0-9]{24}");
        assertThat(h.get("webhook-timestamp")).isEqualTo(Long.toString(NOW));
        assertThat(h.get("webhook-signature")).startsWith("v1,");

        Map<String, String> custom = wh.generateTestHeaders("{}", "msg_custom", 1_000_000L);
        assertThat(custom).containsEntry("webhook-id", "msg_custom").containsEntry("webhook-timestamp", "1000000");
    }

    @Test
    void emptyOrUndecodableSecretIsRejectedWithoutEchoingIt() {
        assertThatThrownBy(() -> new Webhook("")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("required");
        assertThatThrownBy(() -> new Webhook("whsec_!!!not*base64"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("not*base64");
        assertThat(wh.toString()).doesNotContain(SECRET_RAW);
    }

    static Stream<String> secretForms() {
        String b64 = Base64.getEncoder().encodeToString(KEY_BYTES);
        String hex = HexFormat.of().formatHex(KEY_BYTES);
        return Stream.of("whsec_" + b64, "whsec_op_" + b64, "whsec_" + hex, "whsec_" + hex.toUpperCase(Locale.ROOT), hex);
    }

    @ParameterizedTest
    @MethodSource("secretForms")
    void decodesEverySecretFormTheApiSignsWith(String secret) {
        String body = "{\"ok\":true}";
        String sig = apiSign(KEY_BYTES, "msg_1", NOW, body);
        assertThat(new Webhook(secret, CLOCK).verify(body, headers("msg_1", Long.toString(NOW), "v1," + sig)).get("ok").asBoolean()).isTrue();
    }

    @Test
    void legacyHexSecretIsNotBase64Decoded() {
        String hex = HexFormat.of().formatHex(KEY_BYTES);
        byte[] wrongKey = Base64.getDecoder().decode(hex);
        String sig = apiSign(wrongKey, "msg_1", NOW, "{}");
        assertThatThrownBy(() -> new Webhook("whsec_" + hex, CLOCK).verify("{}", headers("msg_1", Long.toString(NOW), "v1," + sig)))
                .isInstanceOf(WebhookVerificationException.class);
    }

    @Test
    void acceptsAnyOfSeveralSpaceSeparatedSignatures() {
        String secret = "whsec_" + Base64.getEncoder().encodeToString(KEY_BYTES);
        byte[] other = new byte[32];
        java.util.Arrays.fill(other, (byte) 1);
        String body = "{\"rotated\":true}";
        String good = apiSign(KEY_BYTES, "msg_1", NOW, body);
        String stale = apiSign(other, "msg_1", NOW, body);
        Webhook w = new Webhook(secret, CLOCK);
        for (String header : new String[] {"v1," + stale + " v1," + good, "v1," + good + " v1," + stale, "v2,xyz v1," + good}) {
            assertThat(w.verify(body, headers("msg_1", Long.toString(NOW), header)).get("rotated").asBoolean()).isTrue();
        }
        assertThatThrownBy(() -> w.verify(body, headers("msg_1", Long.toString(NOW), "v1," + stale + " v1," + stale)))
                .isInstanceOf(WebhookVerificationException.class);
    }

    @Test
    void nonUtf8BytesFailVerification() {
        Map<String, String> h = wh.generateTestHeaders("x");
        assertThatThrownBy(() -> wh.verify(new byte[] {(byte) 0xff, (byte) 0xfe}, h)).isInstanceOf(WebhookVerificationException.class);
    }

    @Test
    void signsRawBytesExactly() {
        // Bytes that are not valid UTF-8 still verify when signed as received.
        byte[] raw = {'{', '}', (byte) 0xff};
        String sig;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(KEY_BYTES, "HmacSHA256"));
            mac.update(("msg_1." + NOW + ".").getBytes(StandardCharsets.UTF_8));
            sig = Base64.getEncoder().encodeToString(mac.doFinal(raw));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        Webhook w = new Webhook("whsec_" + Base64.getEncoder().encodeToString(KEY_BYTES), CLOCK);
        w.verifyHeaders(raw, headers("msg_1", Long.toString(NOW), "v1," + sig), Webhook.DEFAULT_TOLERANCE);
    }
}
