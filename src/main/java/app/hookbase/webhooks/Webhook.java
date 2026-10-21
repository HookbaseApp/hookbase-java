package app.hookbase.webhooks;

import app.hookbase.errors.WebhookVerificationException;
import app.hookbase.internal.Json;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
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
 * <pre>{@code
 * Webhook webhook = new Webhook(System.getenv("HOOKBASE_WEBHOOK_SECRET")); // whsec_...
 *
 * // rawBody must be the exact bytes received, before any JSON parsing.
 * JsonNode event = webhook.verify(rawBody, headers);
 * }</pre>
 *
 * <p>Instances are immutable and thread-safe.
 */
public final class Webhook {

    /** How far a timestamp may be from now, in either direction, before it is rejected. */
    public static final Duration DEFAULT_TOLERANCE = Duration.ofSeconds(300);

    private static final Pattern HEX_SECRET = Pattern.compile("^[0-9a-fA-F]{64}$");
    private static final String ID_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] key;
    private final Clock clock;

    /**
     * @param secret the endpoint's signing secret: {@code whsec_<base64>}, {@code
     *     whsec_op_<base64>} (operational webhooks), a legacy 64-character hex secret, with or
     *     without the {@code whsec_} prefix
     * @throws IllegalArgumentException if the secret is empty or cannot be decoded
     */
    public Webhook(String secret) {
        this(secret, Clock.systemUTC());
    }

    /** As {@link #Webhook(String)}, reading the current time from {@code clock}. For tests. */
    public Webhook(String secret, Clock clock) {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("Webhook secret is required");
        }
        this.key = decodeSecret(secret);
        this.clock = clock;
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
        verifyHeaders(payload, headers, tolerance);
        try {
            return Json.read(payload);
        } catch (IOException e) {
            throw new WebhookVerificationException("Invalid JSON payload", e);
        }
    }

    /** As {@link #verify(String, Map)}, binding the body to {@code type}. */
    public <T> T verify(String payload, Map<String, String> headers, Class<T> type) {
        JsonNode node = verify(payload, headers);
        try {
            return Json.MAPPER.treeToValue(node, type);
        } catch (IOException e) {
            throw new WebhookVerificationException("Payload does not match " + type.getSimpleName(), e);
        }
    }

    /**
     * As {@link #verify(String, Map)}, taking the raw body as bytes. The signature is checked over
     * the bytes exactly as received, so no charset decoding can change what was signed.
     */
    public JsonNode verify(byte[] payload, Map<String, String> headers) {
        verifyHeaders(payload, headers, DEFAULT_TOLERANCE);
        try {
            return Json.MAPPER.readTree(payload);
        } catch (IOException e) {
            throw new WebhookVerificationException("Invalid JSON payload", e);
        }
    }

    /**
     * Check the headers, timestamp and signature without parsing the body.
     *
     * @throws WebhookVerificationException if any check fails
     */
    public void verifyHeaders(String payload, Map<String, String> headers, Duration tolerance) {
        verifyHeaders(payload.getBytes(StandardCharsets.UTF_8), headers, tolerance);
    }

    /** As {@link #verifyHeaders(String, Map, Duration)}, over the raw body bytes. */
    public void verifyHeaders(byte[] payload, Map<String, String> headers, Duration tolerance) {
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
     * @throws WebhookVerificationException if no {@code v1} signature in the header matches
     */
    public void verifySignature(String payload, String webhookId, String webhookTimestamp, String webhookSignature) {
        verifySignature(payload.getBytes(StandardCharsets.UTF_8), webhookId, webhookTimestamp, webhookSignature);
    }

    /** As {@link #verifySignature(String, String, String, String)}, over the raw body bytes. */
    public void verifySignature(byte[] payload, String webhookId, String webhookTimestamp, String webhookSignature) {
        byte[] expected = hmac(webhookId + "." + webhookTimestamp + ".", payload);
        boolean anyParsed = false;
        for (String part : webhookSignature.trim().split(" +")) {
            int comma = part.indexOf(',');
            if (comma <= 0 || comma == part.length() - 1) continue;
            anyParsed = true;
            if (!part.substring(0, comma).equals("v1")) continue;
            byte[] actual;
            try {
                actual = Base64.getDecoder().decode(part.substring(comma + 1));
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (MessageDigest.isEqual(expected, actual)) return;
        }
        if (!anyParsed) throw new WebhookVerificationException("No valid signatures found");
        throw new WebhookVerificationException("Webhook signature verification failed");
    }

    /** {@code v1,<base64 HMAC-SHA256>} of {@code "<id>.<timestamp>.<payload>"}, as Hookbase signs. */
    public String sign(String webhookId, long timestamp, String payload) {
        return "v1," + Base64.getEncoder().encodeToString(
                hmac(webhookId + "." + timestamp + ".", payload.getBytes(StandardCharsets.UTF_8)));
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
        long sent;
        try {
            sent = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException e) {
            throw new WebhookVerificationException("Invalid timestamp format");
        }
        long diff = Math.abs(clock.instant().getEpochSecond() - sent);
        if (diff > tolerance.toSeconds()) {
            throw new WebhookVerificationException(
                    "Webhook timestamp is outside tolerance (" + diff + "s > " + tolerance.toSeconds() + "s)");
        }
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
