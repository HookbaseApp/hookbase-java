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
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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
    @ParameterizedTest
    @ValueSource(strings = {"+1760000000", "-1760000000", " 1760000000", "1760000000 ", "\u0661\u0667\u0666\u0660000000", "9223372036854775807", ""})
    void rejectsNonPlainTimestamps(String ts) {
        // The format is checked before the age or signature, including the overflow case.
        Map<String, String> h = headers("msg_1", ts.isEmpty() ? "x" : ts, "v1,AAAA");
        assertThatThrownBy(() -> wh.verify("{}", h))
                .isInstanceOf(WebhookVerificationException.class).hasMessageContaining("Invalid timestamp");
    }

    @Test
    void secretWithSurroundingWhitespaceIsAccepted() {
        Webhook w = new Webhook(SECRET + "\n", CLOCK);
        assertThat(w.verify("{\"a\":1}", wh.generateTestHeaders("{\"a\":1}")).get("a").asInt()).isEqualTo(1);
    }

    @Test
    void nullInputsAreVerificationFailuresNotNpes() {
        Map<String, String> h = wh.generateTestHeaders("{}");
        assertThatThrownBy(() -> wh.verify((byte[]) null, h)).isInstanceOf(WebhookVerificationException.class);
        assertThatThrownBy(() -> wh.verify("{}", null)).isInstanceOf(WebhookVerificationException.class);
        assertThatThrownBy(() -> wh.verifySignature((String) null, "a", "1", "v1,x")).isInstanceOf(WebhookVerificationException.class);
    }

    @Test
    void rejectsInvalidTolerance() {
        Map<String, String> h = wh.generateTestHeaders("{}");
        assertThatThrownBy(() -> wh.verify("{}", h, (Duration) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wh.verify("{}", h, Duration.ofSeconds(-1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bytesOverloadsTakeToleranceAndType() {
        byte[] body = "{\"data\":7}".getBytes(StandardCharsets.UTF_8);
        Map<String, String> h = wh.generateTestHeaders("{\"data\":7}", "msg_1", NOW - 100);
        assertThat(wh.verify(body, h, Duration.ofSeconds(120)).get("data").asInt()).isEqualTo(7);
        assertThatThrownBy(() -> wh.verify(body, h, Duration.ofSeconds(30))).hasMessageContaining("outside tolerance");
        record Payload(int data) {}
        assertThat(wh.verify(body, wh.generateTestHeaders("{\"data\":7}"), Payload.class).data()).isEqualTo(7);
    }

    @Test
    void latin1DecodedBodyFailsButBytesPass() {
        String body = "{\"name\":\"Caf\u00e9\"}";
        Map<String, String> h = wh.generateTestHeaders(body);
        byte[] raw = body.getBytes(StandardCharsets.UTF_8);
        String misdecoded = new String(raw, StandardCharsets.ISO_8859_1);
        assertThatThrownBy(() -> wh.verify(misdecoded, h)).isInstanceOf(WebhookVerificationException.class);
        assertThat(wh.verify(raw, h).get("name").asText()).isEqualTo("Caf\u00e9");
    }

    /** Standard Webhooks v1a (Ed25519), as api/src/utils/outbound-signature.ts signs it. */
    @Nested
    class Ed25519 {

        private static final String ID = "msg_2Lh9KQjrsGAMfEvaCjdhyRHXcgT";
        private static final String BODY = "{\"event\":\"order.created\",\"data\":{\"id\":\"ord_1\",\"name\":\"Zoë\"}}";

        // RFC 8032 test-1 key; SIG was produced by Node WebCrypto importing the key as JWK, exactly as
        // the API's computeEd25519Signature does, over "<ID>.<NOW>.<BODY>".
        private static final String SK = "whsk_nWGxne/9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2DXWpgBgrEKt9VL/tPJZAc6DuFy89qmIyWvAhpo9wdRGg==";
        private static final String PK = "whpk_11qYAYKxCrfVS/7TyWQHOg7hcvPapiMlrwIaaPcHURo=";
        private static final String SIG = "LoDxAYFHFhw5FfMLHETDZbvLOHwHchZ82rokrvSbN9+7ALIpQ6zDnHu17Ev0y0F2DrPQOJ7Xd23QLDqCTwctBA==";

        /** An independent signer: the 32-byte seed of a whsk_ key, signed with the JDK directly. */
        private static String serverSign(String secretKey, String id, long ts, String body) {
            try {
                byte[] raw = Base64.getDecoder().decode(secretKey.substring("whsk_".length()));
                byte[] der = HexFormat.of().parseHex("302e020100300506032b657004220420");
                byte[] pkcs8 = Arrays.copyOf(der, der.length + 32);
                System.arraycopy(raw, 0, pkcs8, der.length, 32);
                Signature signer = Signature.getInstance("Ed25519");
                signer.initSign(KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(pkcs8)));
                signer.update((id + "." + ts + "." + body).getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(signer.sign());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Test
        void verifiesASignatureProducedByTheApisWebCryptoPath() {
            Webhook verifier = new Webhook(PK, CLOCK);
            JsonNode event = verifier.verify(BODY.getBytes(StandardCharsets.UTF_8), headers(ID, Long.toString(NOW), "v1a," + SIG));
            assertThat(event.at("/data/name").asText()).isEqualTo("Zoë");
            assertThat(verifier.signatureScheme()).isEqualTo(Webhook.SCHEME_ED25519);
            assertThat(new Webhook(SK, CLOCK).sign(ID, NOW, BODY)).isEqualTo("v1a," + SIG);
        }

        @Test
        void verifiesAFreshKeyPairSignedLikeTheServer() {
            Webhook.Ed25519KeyPair pair = Webhook.generateEd25519KeyPair();
            assertThat(pair.secretKey()).startsWith("whsk_");
            assertThat(pair.publicKey()).startsWith("whpk_");
            assertThat(pair.toString()).doesNotContain(pair.secretKey().substring(5));
            String sig = "v1a," + serverSign(pair.secretKey(), ID, NOW, BODY);
            new Webhook(pair.publicKey(), CLOCK).verifyHeaders(BODY, headers(ID, Long.toString(NOW), sig), Webhook.DEFAULT_TOLERANCE);
            // and a whsk_ key verifies with its public half
            new Webhook(pair.secretKey(), CLOCK).verifyHeaders(BODY, headers(ID, Long.toString(NOW), sig), Webhook.DEFAULT_TOLERANCE);
        }

        @Test
        void generateTestHeadersRoundTrips() {
            Webhook.Ed25519KeyPair pair = Webhook.generateEd25519KeyPair();
            Map<String, String> h = new Webhook(pair.secretKey(), CLOCK).generateTestHeaders(BODY);
            assertThat(h.get("webhook-signature")).startsWith("v1a,");
            assertThat(new Webhook(pair.publicKey(), CLOCK).verify(BODY, h).get("event").asText()).isEqualTo("order.created");
        }

        @Test
        void rejectsTheWrongKey() {
            Webhook other = new Webhook(Webhook.generateEd25519KeyPair().publicKey(), CLOCK);
            assertThatThrownBy(() -> other.verify(BODY, headers(ID, Long.toString(NOW), "v1a," + SIG)))
                    .isInstanceOf(WebhookVerificationException.class)
                    .hasMessage("Webhook signature verification failed");
        }

        @Test
        void rejectsATamperedBodyOrId() {
            Webhook verifier = new Webhook(PK, CLOCK);
            assertThatThrownBy(() -> verifier.verify(BODY.replace("ord_1", "ord_2"), headers(ID, Long.toString(NOW), "v1a," + SIG)))
                    .isInstanceOf(WebhookVerificationException.class);
            assertThatThrownBy(() -> verifier.verify(BODY, headers("msg_other", Long.toString(NOW), "v1a," + SIG)))
                    .isInstanceOf(WebhookVerificationException.class);
        }

        @Test
        void rejectsAnExpiredOrFutureTimestampWithTheSameTolerance() {
            long old = NOW - 301;
            String sig = "v1a," + serverSign(SK, ID, old, BODY);
            Webhook verifier = new Webhook(PK, CLOCK);
            assertThatThrownBy(() -> verifier.verify(BODY, headers(ID, Long.toString(old), sig)))
                    .isInstanceOf(WebhookVerificationException.class)
                    .hasMessageContaining("outside tolerance");
            long future = NOW + 301;
            assertThatThrownBy(() -> verifier.verify(BODY, headers(ID, Long.toString(future), "v1a," + serverSign(SK, ID, future, BODY))))
                    .hasMessageContaining("outside tolerance");
            // at the edge, and with a custom tolerance
            long edge = NOW - 300;
            verifier.verify(BODY, headers(ID, Long.toString(edge), "v1a," + serverSign(SK, ID, edge, BODY)));
            verifier.verify(BODY, headers(ID, Long.toString(old), sig), Duration.ofSeconds(600));
        }

        @Test
        void acceptsAnyMatchingEntryDuringRotation() {
            Webhook.Ed25519KeyPair previous = Webhook.generateEd25519KeyPair();
            String header = "v1a," + serverSign(previous.secretKey(), ID, NOW, BODY) + " v1a," + SIG;
            new Webhook(PK, CLOCK).verify(BODY, headers(ID, Long.toString(NOW), header));
            new Webhook(previous.publicKey(), CLOCK).verify(BODY, headers(ID, Long.toString(NOW), header));
        }

        @Test
        void mixedHeaderEachVerifierPicksItsOwnVersion() {
            String hmac = "v1," + apiSign(Webhook.decodeSecret(SECRET), ID, NOW, BODY);
            String mixed = hmac + " v1a," + SIG;
            Map<String, String> h = headers(ID, Long.toString(NOW), mixed);
            new Webhook(PK, CLOCK).verify(BODY, h);
            wh.verify(BODY, h);
            // A v1 entry never satisfies an Ed25519 verifier, nor a v1a entry an HMAC one.
            assertThatThrownBy(() -> new Webhook(PK, CLOCK).verify(BODY, headers(ID, Long.toString(NOW), hmac)))
                    .hasMessage("Webhook signature verification failed");
            assertThatThrownBy(() -> wh.verify(BODY, headers(ID, Long.toString(NOW), "v1a," + SIG)))
                    .hasMessage("Webhook signature verification failed");
        }

        @ParameterizedTest
        @ValueSource(strings = {"v1a,", "v1a,!!!notbase64", "v1a,AAAA", "garbage"})
        void malformedEntriesDoNotMatch(String sig) {
            assertThatThrownBy(() -> new Webhook(PK, CLOCK).verify(BODY, headers(ID, Long.toString(NOW), sig)))
                    .isInstanceOf(WebhookVerificationException.class);
        }

        @Test
        void aPublicKeyCannotSign() {
            Webhook verifier = new Webhook(PK, CLOCK);
            assertThatThrownBy(() -> verifier.sign(ID, NOW, BODY)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> verifier.generateTestHeaders(BODY)).isInstanceOf(IllegalStateException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"whpk_", "whpk_AAAA", "whpk_not base64!", "whsk_11qYAYKxCrfVS/7TyWQHOg7hcvPapiMlrwIaaPcHURo="})
        void rejectsMalformedKeysWithoutEchoingThem(String key) {
            assertThatThrownBy(() -> new Webhook(key))
                    .isInstanceOf(IllegalArgumentException.class)
                    .satisfies(e -> assertThat(e.getMessage()).doesNotContain(key));
        }

        @Test
        void hmacVerifierReportsItsScheme() {
            assertThat(wh.signatureScheme()).isEqualTo(Webhook.SCHEME_HMAC_SHA256);
        }
    }
}
