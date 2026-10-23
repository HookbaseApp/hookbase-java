package app.hookbase.webhooks;

import app.hookbase.errors.WebhookVerificationException;
import app.hookbase.internal.Json;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verifies the webhooks Hookbase sends to your endpoints (Standard Webhooks signing).
 *
 * <p>Each delivery carries {@code webhook-id}, {@code webhook-timestamp} and {@code
 * webhook-signature} headers. The signature is {@code v1,<base64 HMAC-SHA256>} over
 * {@code "<id>.<timestamp>.<raw body>"}; during a secret rotation the header holds several
 * space-separated signatures and any one matching is enough.
 *
 * <p>An endpoint created with {@code signatureScheme("ed25519")} is signed asymmetrically instead
 * (Standard Webhooks {@code v1a}): each entry is {@code v1a,<base64 Ed25519 signature>} over the same
 * content, and you verify with the endpoint's public key, {@code whpk_<base64>}, from {@link
 * app.hookbase.models.Endpoint#publicKey()} or the rotate response. Pass it to the same constructor:
 * {@code new Webhook("whpk_...")}. Timestamp tolerance and multi-signature handling are identical.
 *
 * <pre>{@code
 * Webhook webhook = new Webhook(System.getenv("HOOKBASE_WEBHOOK_SECRET")); // whsec_...
 *
 * // Spring: take the body as bytes, exactly as received.
 * @PostMapping("/webhooks/hookbase")
 * ResponseEntity<Void> receive(@RequestBody byte[] body, @RequestHeader HttpHeaders headers) {
 *     JsonNode event = webhook.verify(body, headers.toSingleValueMap());
 *     ...
 * }
 * }</pre>
 *
 * <p>Verify the raw bytes. A body that was parsed and re-serialized ({@code @RequestBody Map} or
 * {@code JsonNode}) no longer matches the signature. The {@code String} overloads encode the text as
 * UTF-8, so they work only when the body was decoded as UTF-8: Hookbase sends {@code
 * application/json} without a charset, and a Servlet {@code getReader()} then decodes it as
 * ISO-8859-1, which breaks any non-ASCII body.
 *
 * <p>Instances are immutable and thread-safe.
 */
public final class Webhook {

    /** How far a timestamp may be from now, in either direction, before it is rejected. */
    public static final Duration DEFAULT_TOLERANCE = Duration.ofSeconds(300);

    private static final Pattern HEX_SECRET = Pattern.compile("^[0-9a-fA-F]{64}$");
    private static final Pattern TIMESTAMP = Pattern.compile("[0-9]{1,15}");
    private static final String ID_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** {@link #signatureScheme()} of an HMAC-SHA256 ({@code v1}) verifier. */
    public static final String SCHEME_HMAC_SHA256 = "hmac-sha256";

    /** {@link #signatureScheme()} of an Ed25519 ({@code v1a}) verifier. */
    public static final String SCHEME_ED25519 = "ed25519";

    private static final String PUBLIC_KEY_PREFIX = "whpk_";
    private static final String SECRET_KEY_PREFIX = "whsk_";
    // DER headers that wrap a raw 32-byte Ed25519 key as X.509 SubjectPublicKeyInfo / PKCS#8 (RFC 8410).
    private static final byte[] X509_ED25519_PREFIX = HexFormat.of().parseHex("302a300506032b6570032100");
    private static final byte[] PKCS8_ED25519_PREFIX = HexFormat.of().parseHex("302e020100300506032b657004220420");

    /** HMAC key bytes; {@code null} for an Ed25519 verifier. */
    private final byte[] key;
    /** Ed25519 public key; {@code null} for an HMAC verifier. */
    private final PublicKey publicKey;
    /** Ed25519 private key, set only when constructed from a {@code whsk_} secret key (for tests). */
    private final PrivateKey privateKey;
    private final Clock clock;

    /**
     * @param secret the endpoint's signing secret: {@code whsec_<base64>}, {@code
     *     whsec_op_<base64>} (operational webhooks), a legacy 64-character hex secret, with or
     *     without the {@code whsec_} prefix. For an Ed25519 ({@code v1a}) endpoint, its public key
     *     {@code whpk_<base64 32 bytes>}; a {@code whsk_<base64 seed || public key>} secret key is
     *     also accepted, so {@link #sign} and {@link #generateTestHeaders} can produce {@code v1a}
     *     signatures in tests. The prefix picks the scheme.
     * @throws IllegalArgumentException if the secret is empty or cannot be decoded
     */
    public Webhook(String secret) {
        this(secret, Clock.systemUTC());
    }

    /** As {@link #Webhook(String)}, reading the current time from {@code clock}. For tests. */
    public Webhook(String secret, Clock clock) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Webhook secret is required");
        }
        String s = secret.strip();
        if (s.startsWith(PUBLIC_KEY_PREFIX)) {
            this.key = null;
            this.publicKey = ed25519PublicKey(decodeBase64(s.substring(PUBLIC_KEY_PREFIX.length()), 32, "Ed25519 public key"));
            this.privateKey = null;
        } else if (s.startsWith(SECRET_KEY_PREFIX)) {
            byte[] raw = decodeBase64(s.substring(SECRET_KEY_PREFIX.length()), 64, "Ed25519 secret key");
            this.key = null;
            this.privateKey = ed25519PrivateKey(Arrays.copyOfRange(raw, 0, 32));
            this.publicKey = ed25519PublicKey(Arrays.copyOfRange(raw, 32, 64));
            Arrays.fill(raw, (byte) 0);
        } else {
            this.key = decodeSecret(s);
            this.publicKey = null;
            this.privateKey = null;
        }
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Verify the headers and signature, then parse the body.
     *
     * @param payload the raw request body, exactly as received
     * @param headers the request headers (at least the three {@code webhook-*} ones), names matched
     *     case-insensitively. For a multi-valued map such as Spring's {@code HttpHeaders}, pass
     *     {@code headers.toSingleValueMap()}
     * @return the parsed JSON body
     * @throws WebhookVerificationException if a header is missing, the timestamp is outside the
     *     tolerance, no signature matches, or the body is not JSON
     */
    public JsonNode verify(String payload, Map<String, String> headers) {
        return verify(payload, headers, DEFAULT_TOLERANCE);
    }

    /** As {@link #verify(String, Map)}, with a custom timestamp tolerance. */
    public JsonNode verify(String payload, Map<String, String> headers, Duration tolerance) {
        return verify(utf8(payload), headers, tolerance);
    }

    /** As {@link #verify(String, Map)}, binding the body to {@code type}. */
    public <T> T verify(String payload, Map<String, String> headers, Class<T> type) {
        return verify(utf8(payload), headers, type);
    }

    /**
     * As {@link #verify(String, Map)}, taking the raw body as bytes: the preferred form. The
     * signature is checked over the bytes exactly as received, so no charset decoding can change
     * what was signed.
     */
    public JsonNode verify(byte[] payload, Map<String, String> headers) {
        return verify(payload, headers, DEFAULT_TOLERANCE);
    }

    /** As {@link #verify(byte[], Map)}, with a custom timestamp tolerance. */
    public JsonNode verify(byte[] payload, Map<String, String> headers, Duration tolerance) {
        verifyHeaders(payload, headers, tolerance);
        try {
            return Json.MAPPER.readTree(payload);
        } catch (IOException e) {
            throw new WebhookVerificationException("Invalid JSON payload", e);
        }
    }

    /** As {@link #verify(byte[], Map)}, binding the body to {@code type}. */
    public <T> T verify(byte[] payload, Map<String, String> headers, Class<T> type) {
        Objects.requireNonNull(type, "type");
        JsonNode node = verify(payload, headers);
        try {
            return Json.MAPPER.treeToValue(node, type);
        } catch (IOException e) {
            throw new WebhookVerificationException("Payload does not match " + type.getSimpleName(), e);
        }
    }

    /**
     * Check the headers, timestamp and signature without parsing the body.
     *
     * @throws WebhookVerificationException if any check fails
     */
    public void verifyHeaders(String payload, Map<String, String> headers, Duration tolerance) {
        verifyHeaders(utf8(payload), headers, tolerance);
    }

    /** As {@link #verifyHeaders(String, Map, Duration)}, over the raw body bytes. */
    public void verifyHeaders(byte[] payload, Map<String, String> headers, Duration tolerance) {
        checkTolerance(tolerance);
        if (payload == null) throw new WebhookVerificationException("Missing payload");
        if (headers == null) throw new WebhookVerificationException("Missing webhook headers");
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null) normalized.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }
        String id = normalized.get("webhook-id");
        String timestamp = normalized.get("webhook-timestamp");
        String signature = normalized.get("webhook-signature");
        if (id == null || id.isEmpty()) throw new WebhookVerificationException("Missing webhook-id header");
        if (timestamp == null || timestamp.isEmpty()) throw new WebhookVerificationException("Missing webhook-timestamp header");
        if (signature == null || signature.isEmpty()) throw new WebhookVerificationException("Missing webhook-signature header");

        verifyTimestamp(timestamp, tolerance);
        verifySignature(payload, id, timestamp, signature);
    }

    /**
     * Check a signature header against the payload. Does not check the timestamp's age; use {@link
     * #verify} or {@link #verifyHeaders} for that.
     *
     * <p><b>Without the timestamp check a captured delivery can be replayed indefinitely.</b> Prefer
     * {@link #verify}.
     *
     * @throws WebhookVerificationException if no signature of this verifier's scheme ({@code v1}
     *     for HMAC, {@code v1a} for Ed25519) in the header matches
     */
    public void verifySignature(String payload, String webhookId, String webhookTimestamp, String webhookSignature) {
        verifySignature(utf8(payload), webhookId, webhookTimestamp, webhookSignature);
    }

    /** As {@link #verifySignature(String, String, String, String)}, over the raw body bytes. */
    public void verifySignature(byte[] payload, String webhookId, String webhookTimestamp, String webhookSignature) {
        if (payload == null || webhookId == null || webhookTimestamp == null || webhookSignature == null) {
            throw new WebhookVerificationException("Missing payload or webhook header");
        }
        String prefix = webhookId + "." + webhookTimestamp + ".";
        boolean ed25519 = publicKey != null;
        byte[] expected = ed25519 ? null : hmac(prefix, payload);
        String version = ed25519 ? "v1a" : "v1";
        boolean anyParsed = false;
        for (String part : webhookSignature.trim().split(" +")) {
            int comma = part.indexOf(',');
            if (comma <= 0 || comma == part.length() - 1) continue;
            anyParsed = true;
            if (!part.substring(0, comma).equals(version)) continue;
            byte[] actual;
            try {
                actual = Base64.getDecoder().decode(part.substring(comma + 1));
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (ed25519 ? ed25519Verify(prefix, payload, actual) : MessageDigest.isEqual(expected, actual)) return;
        }
        if (!anyParsed) throw new WebhookVerificationException("No valid signatures found");
        throw new WebhookVerificationException("Webhook signature verification failed");
    }

    /**
     * {@code v1,<base64 HMAC-SHA256>} of {@code "<id>.<timestamp>.<payload>"}, as Hookbase signs; for
     * a verifier built from a {@code whsk_} secret key, {@code v1a,<base64 Ed25519 signature>}.
     *
     * @throws IllegalStateException if this verifier holds only an Ed25519 public key ({@code
     *     whpk_}), which cannot sign
     */
    public String sign(String webhookId, long timestamp, String payload) {
        Objects.requireNonNull(webhookId, "webhookId");
        Objects.requireNonNull(payload, "payload");
        String prefix = webhookId + "." + timestamp + ".";
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        if (publicKey != null) {
            if (privateKey == null) {
                throw new IllegalStateException("An Ed25519 public key (whpk_) can only verify; sign with the whsk_ secret key");
            }
            return "v1a," + Base64.getEncoder().encodeToString(ed25519Sign(prefix, body));
        }
        return "v1," + Base64.getEncoder().encodeToString(hmac(prefix, body));
    }

    /**
     * Which signatures this instance verifies: {@link #SCHEME_HMAC_SHA256} ({@code v1}) or {@link
     * #SCHEME_ED25519} ({@code v1a}), matching the endpoint's {@code signatureScheme}.
     */
    public String signatureScheme() {
        return publicKey != null ? SCHEME_ED25519 : SCHEME_HMAC_SHA256;
    }

    /**
     * A new Ed25519 key pair in the Standard Webhooks serialization Hookbase uses. Hookbase generates
     * an endpoint's key pair itself and never returns the secret key, so this is for testing: sign
     * with {@code new Webhook(pair.secretKey())}, verify with {@code new Webhook(pair.publicKey())}.
     */
    public static Ed25519KeyPair generateEd25519KeyPair() {
        KeyPair pair;
        try {
            pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Ed25519 unavailable", e);
        }
        byte[] pub = stripPrefix(pair.getPublic().getEncoded(), X509_ED25519_PREFIX, "public");
        byte[] seed = stripPrefix(pair.getPrivate().getEncoded(), PKCS8_ED25519_PREFIX, "private");
        byte[] combined = new byte[64];
        System.arraycopy(seed, 0, combined, 0, 32);
        System.arraycopy(pub, 0, combined, 32, 32);
        Base64.Encoder b64 = Base64.getEncoder();
        String secretKey = SECRET_KEY_PREFIX + b64.encodeToString(combined);
        Arrays.fill(seed, (byte) 0);
        Arrays.fill(combined, (byte) 0);
        return new Ed25519KeyPair(secretKey, PUBLIC_KEY_PREFIX + b64.encodeToString(pub));
    }

    /**
     * An Ed25519 key pair from {@link #generateEd25519KeyPair()}.
     *
     * @param secretKey {@code whsk_<base64 of the 32-byte seed || 32-byte public key>}
     * @param publicKey {@code whpk_<base64 of the 32-byte public key>}
     */
    public record Ed25519KeyPair(String secretKey, String publicKey) {
        /** The secret key is printed as {@code <redacted>}. */
        @Override
        public String toString() {
            return "Ed25519KeyPair[secretKey=<redacted>, publicKey=" + publicKey + "]";
        }
    }

    /**
     * Headers that {@link #verify} accepts for {@code payload}, signed now with a random
     * {@code msg_} id. For testing your webhook handler.
     */
    public Map<String, String> generateTestHeaders(String payload) {
        return generateTestHeaders(payload, null, null);
    }

    /**
     * As {@link #generateTestHeaders(String)}, with a fixed id and/or Unix timestamp (seconds).
     * {@code null} for either picks the default.
     */
    public Map<String, String> generateTestHeaders(String payload, String webhookId, Long timestamp) {
        String id = webhookId != null ? webhookId : "msg_" + randomId();
        long ts = timestamp != null ? timestamp : clock.instant().getEpochSecond();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("webhook-id", id);
        headers.put("webhook-timestamp", Long.toString(ts));
        headers.put("webhook-signature", sign(id, ts, payload));
        return headers;
    }

    private void verifyTimestamp(String timestamp, Duration tolerance) {
        // Unix seconds, ASCII digits only (no sign, spaces or other scripts' digits).
        if (!TIMESTAMP.matcher(timestamp).matches()) {
            throw new WebhookVerificationException("Invalid timestamp format");
        }
        long sent = Long.parseLong(timestamp);
        // Both values are at most 15 digits, so the difference cannot overflow.
        long diff = Math.abs(clock.instant().getEpochSecond() - sent);
        if (diff > tolerance.toSeconds()) {
            throw new WebhookVerificationException(
                    "Webhook timestamp is outside tolerance (" + diff + "s > " + tolerance.toSeconds() + "s)");
        }
    }

    private static void checkTolerance(Duration tolerance) {
        if (tolerance == null || tolerance.isNegative()) {
            throw new IllegalArgumentException("tolerance must be zero or positive");
        }
    }

    private static byte[] utf8(String payload) {
        return payload == null ? null : payload.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] hmac(String prefix, byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            mac.update(prefix.getBytes(StandardCharsets.UTF_8));
            return mac.doFinal(payload);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }

    private boolean ed25519Verify(String prefix, byte[] payload, byte[] signature) {
        if (signature.length != 64) return false;
        try {
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(prefix.getBytes(StandardCharsets.UTF_8));
            verifier.update(payload);
            return verifier.verify(signature);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Ed25519 unavailable", e);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    private byte[] ed25519Sign(String prefix, byte[] payload) {
        try {
            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(privateKey);
            signer.update(prefix.getBytes(StandardCharsets.UTF_8));
            signer.update(payload);
            return signer.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Ed25519 signing failed", e);
        }
    }

    private static PublicKey ed25519PublicKey(byte[] raw) {
        try {
            return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(concat(X509_ED25519_PREFIX, raw)));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Ed25519 public key is invalid");
        }
    }

    private static PrivateKey ed25519PrivateKey(byte[] seed) {
        byte[] der = concat(PKCS8_ED25519_PREFIX, seed);
        try {
            return KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Ed25519 secret key is invalid");
        } finally {
            Arrays.fill(der, (byte) 0);
            Arrays.fill(seed, (byte) 0);
        }
    }

    /** Base64 that must decode to exactly {@code length} bytes. Never echoes the input. */
    private static byte[] decodeBase64(String b64, int length, String what) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(b64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(what + " is not valid base64");
        }
        if (decoded.length != length) {
            throw new IllegalArgumentException(what + " must be " + length + " bytes, got " + decoded.length);
        }
        return decoded;
    }

    private static byte[] stripPrefix(byte[] der, byte[] prefix, String what) {
        if (der.length != prefix.length + 32 || !Arrays.equals(der, 0, prefix.length, prefix, 0, prefix.length)) {
            throw new IllegalStateException("Unexpected Ed25519 " + what + " key encoding");
        }
        return Arrays.copyOfRange(der, prefix.length, der.length);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    /**
     * The same decoding the API signs with (api/src/utils/outbound-signature.ts): strip
     * {@code whsec_} and the {@code op_} infix operational-webhook secrets carry; a 64-character hex
     * remainder is a legacy secret and is the hex bytes; anything else is base64.
     */
    static byte[] decodeSecret(String secret) {
        String raw = secret;
        if (raw.startsWith("whsec_")) raw = raw.substring("whsec_".length());
        if (raw.startsWith("op_")) raw = raw.substring("op_".length());
        if (HEX_SECRET.matcher(raw).matches()) {
            return HexFormat.of().parseHex(raw);
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(raw);
        } catch (IllegalArgumentException e) {
            // Never echo the secret in the message.
            throw new IllegalArgumentException("Webhook secret is not valid base64 or 64-character hex");
        }
        if (decoded.length == 0) {
            throw new IllegalArgumentException("Webhook secret is empty after decoding");
        }
        return decoded;
    }

    private static String randomId() {
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++) sb.append(ID_ALPHABET.charAt(RANDOM.nextInt(ID_ALPHABET.length())));
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Webhook[secret=<redacted>]";
    }
}
