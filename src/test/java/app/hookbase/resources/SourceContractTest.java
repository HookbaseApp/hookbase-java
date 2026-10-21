package app.hookbase.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.hookbase.errors.HookbaseException;
import app.hookbase.errors.ValidationException;
import app.hookbase.internal.Json;
import app.hookbase.models.Destination;
import app.hookbase.models.DestinationFieldMapping;
import app.hookbase.models.DestinationThrottle;
import app.hookbase.models.KnownSourceProviders;
import app.hookbase.models.Source;
import app.hookbase.models.SourceWithSecret;
import app.hookbase.params.CreateDestinationParams;
import app.hookbase.params.CreateSourceParams;
import app.hookbase.params.Params;
import app.hookbase.params.UpdateDestinationParams;
import app.hookbase.params.UpdateSourceParams;
import app.hookbase.testing.MockServer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The 2026-10 source contract (open provider strings, null secrets, verify tokens), and the field
 * names this SDK puts on the wire for sources and destinations. Ported from node
 * source-contract/source-fields, go sources_contract/sources_fields/destinations_fields, and python
 * test_source_contract/test_source_fields.
 */
class SourceContractTest {

    // ---- the field sets ----------------------------------------------------------------------

    /**
     * The fields POST /api/sources accepts: api/src/__tests__/unit/source-schema-strict.test.ts,
     * SDK_CREATE_FIELDS plus API_ONLY_FIELDS (verifyToken, which node, python and go also send).
     * The schema is strict, so a key outside this set is a 400.
     */
    static final Set<String> API_SOURCE_CREATE_FIELDS = Set.of(
            "name", "slug", "provider", "description", "signingSecret", "rejectInvalidSignatures",
            "rateLimitPerMinute", "ipFilterMode", "ipAllowlist", "ipDenylist", "encryptFields",
            "maskFields", "dedupEnabled", "dedupStrategy", "dedupWindowHours", "dedupCustomHeader",
            "transientMode", "allowedMethods", "verifyToken");

    static final Set<String> API_SOURCE_UPDATE_FIELDS;

    static {
        Set<String> update = new HashSet<>(API_SOURCE_CREATE_FIELDS);
        update.remove("slug");
        update.add("isActive");
        API_SOURCE_UPDATE_FIELDS = Set.copyOf(update);
    }

    /** The names node, python and go sent before the API schema went strict, each silently dropped. */
    static final List<String> STALE_SOURCE_FIELDS =
            List.of("verifySignature", "dedupWindow", "dedupHeaderName", "rateLimit", "rateLimitWindow", "enabled", "verificationConfig");

    static final Set<String> DESTINATION_CREATE_FIELDS = Set.of(
            "name", "slug", "type", "url", "method", "headers", "authType", "authConfig",
            "timeoutMs", "throttle", "config", "fieldMapping", "useStaticIp", "batchSize", "batchWindowSeconds");

    static final Set<String> DESTINATION_UPDATE_FIELDS = Set.of(
            "name", "url", "method", "headers", "authType", "authConfig", "timeoutMs",
            "throttle", "isActive", "config", "fieldMapping", "useStaticIp", "batchSize", "batchWindowSeconds");

    /** Columns the destinations table never had, and the pre-timeoutMs spelling. */
    static final List<String> STALE_DESTINATION_FIELDS = List.of("description", "retryCount", "retryInterval", "timeout");

    /** Calls every public setter the class declares, so the result is every key it can send. */
    static Set<String> everyKeySent(Params<?> params) {
        for (Method m : params.getClass().getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers()) || m.getParameterCount() != 1 || m.getReturnType() != params.getClass()) continue;
            try {
                m.invoke(params, sample(m.getParameterTypes()[0]));
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(m.getName(), e);
            }
        }
        return new TreeSet<>(params.toMap().keySet());
    }

    private static Object sample(Class<?> type) {
        if (type == String.class) return "x";
        if (type == int.class || type == Integer.class) return 1;
        if (type == boolean.class || type == Boolean.class) return true;
        if (type == List.class) return List.of();
        if (type == Map.class) return Map.of();
        if (type == DestinationThrottle.class) return DestinationThrottle.off();
        throw new AssertionError("no sample for " + type);
    }

    @Test
    void createSourceParamsSendExactlyTheFieldsTheApiAccepts() {
        assertThat(everyKeySent(new CreateSourceParams())).isEqualTo(new TreeSet<>(API_SOURCE_CREATE_FIELDS));
    }

    @Test
    void updateSourceParamsSendExactlyTheFieldsTheApiAccepts() {
        assertThat(everyKeySent(new UpdateSourceParams())).isEqualTo(new TreeSet<>(API_SOURCE_UPDATE_FIELDS));
    }

    @Test
    void createAndUpdateDifferOnlyBySlugAndIsActive() {
        Set<String> create = everyKeySent(new CreateSourceParams());
        Set<String> update = everyKeySent(new UpdateSourceParams());
        assertThat(create.stream().filter(k -> !update.contains(k)).toList()).containsExactly("slug");
        assertThat(update.stream().filter(k -> !create.contains(k)).toList()).containsExactly("isActive");
    }

    @Test
    void noStaleSourceFieldHasASetter() {
        for (Class<?> c : List.of(CreateSourceParams.class, UpdateSourceParams.class)) {
            Set<String> methods = new HashSet<>();
            for (Method m : c.getDeclaredMethods()) methods.add(m.getName());
            assertThat(methods).doesNotContainAnyElementsOf(STALE_SOURCE_FIELDS);
        }
    }

    @Test
    void aFullCreateSerializesEveryFieldUnderItsWireName() {
        CreateSourceParams full = new CreateSourceParams().name("Stripe Production").slug("stripe").provider("stripe")
                .description("Live charge events").signingSecret("whsec_abc").verifyToken("vt_abc")
                .rejectInvalidSignatures(true).rateLimitPerMinute(600).ipFilterMode("allowlist")
                .ipAllowlist(List.of("203.0.113.0/24")).ipDenylist(List.of()).encryptFields(List.of("$.data.object.customer_email"))
                .maskFields(List.of("$.data.object.last4")).dedupEnabled(true).dedupStrategy("provider_id")
                .dedupWindowHours(24).dedupCustomHeader("X-Idempotency-Key").transientMode(false).allowedMethods(List.of("POST"));
        assertThat(MockServer.json(Json.write(full.toMap()))).isEqualTo(MockServer.json("""
                {"name":"Stripe Production","slug":"stripe","provider":"stripe","description":"Live charge events",
                 "signingSecret":"whsec_abc","verifyToken":"vt_abc","rejectInvalidSignatures":true,"rateLimitPerMinute":600,
                 "ipFilterMode":"allowlist","ipAllowlist":["203.0.113.0/24"],"ipDenylist":[],
                 "encryptFields":["$.data.object.customer_email"],"maskFields":["$.data.object.last4"],"dedupEnabled":true,
                 "dedupStrategy":"provider_id","dedupWindowHours":24,"dedupCustomHeader":"X-Idempotency-Key",
                 "transientMode":false,"allowedMethods":["POST"]}"""));
    }

    @Test
    void createDestinationParamsSendExactlyTheAcceptedFields() {
        assertThat(everyKeySent(new CreateDestinationParams())).isEqualTo(new TreeSet<>(DESTINATION_CREATE_FIELDS));
        assertThat(everyKeySent(new UpdateDestinationParams())).isEqualTo(new TreeSet<>(DESTINATION_UPDATE_FIELDS));
        assertThat(DESTINATION_CREATE_FIELDS).doesNotContainAnyElementsOf(STALE_DESTINATION_FIELDS);
        assertThat(DESTINATION_UPDATE_FIELDS).doesNotContain("slug", "type");
    }

    // ---- response models ---------------------------------------------------------------------

    private static Set<String> components(Class<? extends Record> type) {
        Set<String> names = new HashSet<>();
        for (RecordComponent c : type.getRecordComponents()) names.add(c.getName());
        return names;
    }

    @Test
    void aListedSourceCarriesTheMaskedPairNotTheSecret() {
        assertThat(components(Source.class)).contains("hasSigningSecret", "signingSecretLast4").doesNotContain("signingSecret", "verifyToken");
        assertThat(components(SourceWithSecret.class)).contains("signingSecret", "verifyToken", "ingestUrl");
        Source s = Json.MAPPER.convertValue(MockServer.json("{\"hasSigningSecret\":true,\"signingSecretLast4\":\"...c123\"}"), Source.class);
        assertThat(s.hasSigningSecret()).isTrue();
        assertThat(s.signingSecretLast4()).isEqualTo("...c123");
    }

    @Test
    void staleSourceAndDestinationKeysNeitherDecodeNorExist() {
        Source s = Json.MAPPER.convertValue(MockServer.json("""
                {"id":"src_1","verifySignature":true,"dedupWindow":24,"dedupHeaderName":"X-Dedup","rateLimit":100,
                 "rateLimitWindow":60,"lastEventAt":"2026-01-01T00:00:00Z","rejectInvalidSignatures":true,
                 "rateLimitPerMinute":100,"dedupWindowHours":24,"dedupCustomHeader":"X-Dedup"}"""), Source.class);
        assertThat(s.rejectInvalidSignatures()).isTrue();
        assertThat(s.rateLimitPerMinute()).isEqualTo(100);
        assertThat(s.dedupWindowHours()).isEqualTo(24);
        assertThat(s.dedupCustomHeader()).isEqualTo("X-Dedup");
        assertThat(components(Source.class)).doesNotContainAnyElementsOf(STALE_SOURCE_FIELDS).doesNotContain("lastEventAt");

        Destination d = Json.MAPPER.convertValue(MockServer.json("""
                {"id":"dst_1","description":"not a column","timeout":30,"retryCount":3,"retryInterval":60,
                 "lastDeliveryAt":"2026-01-01T00:00:00Z","timeoutMs":30000,"deliveryCount":7}"""), Destination.class);
        assertThat(d.timeoutMs()).isEqualTo(30000);
        assertThat(d.deliveryCount()).isEqualTo(7);
        assertThat(components(Destination.class)).doesNotContainAnyElementsOf(STALE_DESTINATION_FIELDS).doesNotContain("lastDeliveryAt");
    }

    // ---- providers ---------------------------------------------------------------------------

    @Test
    void knownProvidersAreThe78TheApiAccepts() {
        assertThat(KnownSourceProviders.ALL).hasSize(78).doesNotHaveDuplicates()
                .contains("wise", "whatsapp", "sendgrid", "mailgun", "linear", "stripe", "aws-sns", "standard-webhooks");
        assertThat(KnownSourceProviders.ALL).allMatch(p -> p.matches("[a-z0-9-]+"));
        assertThat(KnownSourceProviders.ALL).contains(KnownSourceProviders.WISE, KnownSourceProviders.WHATSAPP);
        assertThatThrownBy(() -> KnownSourceProviders.ALL.add("x")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void providerIsAnOpenStringOnRequestsToo() {
        assertThat(new CreateSourceParams().name("X").slug("x").provider("some-provider-added-next-month").toMap())
                .containsEntry("provider", "some-provider-added-next-month");
        assertThat(new UpdateSourceParams().provider("stripee").toMap()).containsEntry("provider", "stripee");
    }

    // ---- the wire, against a server ----------------------------------------------------------

    private static final String WISE_SOURCE = """
            "id":"src_wise","name":"Wise","slug":"wise","provider":"wise","hasSigningSecret":false,
            "signingSecretLast4":null,"verifiesWithoutSecret":true,"awaitingSigningSecret":false""";

    private MockServer server;
    private SourcesResource sources;
    private DestinationsResource destinations;

    @BeforeEach
    void start() {
        server = new MockServer();
        var client = server.client();
        sources = client.sources();
        destinations = client.destinations();
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void listsASourceWithAProviderNewerThanThisPackage() {
        server.enqueue(200, "{\"sources\":[{" + WISE_SOURCE + "},{" + WISE_SOURCE.replace("\"provider\":\"wise\"", "\"provider\":\"not-yet-known\"")
                + "}],\"pagination\":{\"total\":2,\"page\":1,\"pageSize\":20}}");
        var page = sources.list();
        assertThat(page.data().stream().map(Source::provider).toList()).containsExactly("wise", "not-yet-known");
        assertThat(page.data().getFirst().verifiesWithoutSecret()).isTrue();
    }

    @Test
    void createsASourceWhoseSigningSecretIsNull() {
        server.enqueue(201, "{\"source\":{" + WISE_SOURCE + ",\"signingSecret\":null}}");
        SourceWithSecret created = sources.create(new CreateSourceParams().name("Wise").slug("wise").provider("wise"));
        assertThat(created.signingSecret()).isNull();
        assertThat(created.verifiesWithoutSecret()).isTrue();
        assertThat(server.last().body()).isEqualTo(MockServer.json("{\"name\":\"Wise\",\"slug\":\"wise\",\"provider\":\"wise\"}"));
    }

    @Test
    void reportsASecretAwaitingRegistration() {
        server.enqueue(201, "{\"source\":{" + WISE_SOURCE.replace("\"verifiesWithoutSecret\":true", "\"verifiesWithoutSecret\":false")
                .replace("\"awaitingSigningSecret\":false", "\"awaitingSigningSecret\":true") + ",\"provider\":\"notion\",\"signingSecret\":null}}");
        SourceWithSecret created = sources.create(new CreateSourceParams().name("Notion").slug("notion").provider("notion"));
        assertThat(created.awaitingSigningSecret()).isTrue();
        assertThat(created.verifiesWithoutSecret()).isFalse();
    }

    @Test
    void sendsAndReturnsAWhatsAppVerifyToken() {
        server.enqueue(201, "{\"source\":{\"id\":\"src_wa\",\"provider\":\"whatsapp\",\"signingSecret\":\"app-secret\",\"verifyToken\":\"vt_123\"}}");
        SourceWithSecret created = sources.create(new CreateSourceParams().name("WhatsApp").slug("wa").provider("whatsapp").verifyToken("vt_123"));
        assertThat(created.verifyToken()).isEqualTo("vt_123");
        assertThat(created.signingSecret()).isEqualTo("app-secret");
        assertThat(server.last().body().get("verifyToken").asText()).isEqualTo("vt_123");
    }

    @Test
    void updateSendsExplicitNullsToClearTheSecretAndRegenerateTheVerifyToken() {
        server.enqueue(200, "{\"success\":true}");
        sources.update("src_wa", new UpdateSourceParams().verifyToken(null).signingSecret(null));
        assertThat(server.last().rawBody()).isEqualTo("{\"verifyToken\":null,\"signingSecret\":null}");

        server.enqueue(200, "{\"success\":true}");
        sources.update("src_wa", new UpdateSourceParams().verifyToken(""));
        assertThat(server.last().rawBody()).isEqualTo("{\"verifyToken\":\"\"}");

        server.enqueue(200, "{\"success\":true}");
        sources.update("src_1", new UpdateSourceParams().rateLimitPerMinute(null).ipAllowlist(null).dedupWindowHours(null).allowedMethods(List.of()));
        assertThat(server.last().rawBody()).isEqualTo("{\"rateLimitPerMinute\":null,\"ipAllowlist\":null,\"dedupWindowHours\":null,\"allowedMethods\":[]}");
    }

    @Test
    void aVerifyTokenOnANonMetaSourceIsA400() {
        server.enqueue(400, "{\"error\":\"stripe has no verify token\"}");
        assertThatThrownBy(() -> sources.verifyToken("src_1")).isInstanceOf(ValidationException.class);
    }

    @Test
    void rotatingABuiltInProviderSecretIsA400() {
        server.enqueue(400, "{\"error\":\"wise has no secret to rotate\"}");
        assertThatThrownBy(() -> sources.rotateSecret("src_wise")).isInstanceOf(ValidationException.class);
    }

    // ---- destination slug on create ----------------------------------------------------------

    private String sentSlug(CreateDestinationParams params) {
        server.enqueue(201, "{\"destination\":{\"id\":\"dst_1\"}}");
        destinations.create(params);
        var slug = server.last().body().get("slug");
        return slug == null ? null : slug.isNull() ? "<null>" : slug.asText();
    }

    @Test
    void aMissingSlugIsDerivedFromTheName() {
        assertThat(sentSlug(new CreateDestinationParams().name("S3 Archive"))).isEqualTo("s3-archive");
        assertThat(sentSlug(new CreateDestinationParams().name("Café EU").slug(null))).isEqualTo("cafe-eu");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n", " ", "﻿", " ", "　"})
    void aBlankSlugIsTreatedAsMissing(String blank) {
        assertThat(sentSlug(new CreateDestinationParams().name("S3 Archive").slug(blank))).isEqualTo("s3-archive");
    }

    @Test
    void aSlugTheCallerSetIsSentUnchanged() {
        // This derives a missing slug; it does not launder a bad one into a good one.
        assertThat(sentSlug(new CreateDestinationParams().name("S3 Archive").slug("legacy_Slug"))).isEqualTo("legacy_Slug");
        assertThat(sentSlug(new CreateDestinationParams().name("S3 Archive").slug("Not_Valid"))).isEqualTo("Not_Valid");
        // Not blank to JavaScript's trim (U+200B is not whitespace), so sent as given.
        assertThat(sentSlug(new CreateDestinationParams().name("S3 Archive").slug("​"))).isEqualTo("​");
    }

    @Test
    void updateNeverSendsASlug() {
        server.enqueue(200, "{\"success\":true}");
        destinations.update("dst_1", new UpdateDestinationParams().name("Renamed").timeoutMs(45000).isActive(false));
        assertThat(server.last().rawBody()).isEqualTo("{\"name\":\"Renamed\",\"timeoutMs\":45000,\"isActive\":false}");
        assertThat(Arrays.stream(UpdateDestinationParams.class.getDeclaredMethods()).map(Method::getName)).doesNotContain("slug");
    }

    @Test
    void anUnderivableNameFailsBeforeAnyRequestAndNamesTheField() {
        List<String> names = new ArrayList<>(List.of("☃☃☃", "→→→", "!!!"));
        for (String name : names) {
            assertThatThrownBy(() -> destinations.create(new CreateDestinationParams().name(name).url("https://example.com/webhook")))
                    .isExactlyInstanceOf(HookbaseException.class)
                    .hasMessageContaining("slug");
        }
        // No name at all is the same: nothing to derive from.
        assertThatThrownBy(() -> destinations.create(new CreateDestinationParams().url("https://example.com/webhook")))
                .isInstanceOf(HookbaseException.class);
        assertThat(server.requests()).isEmpty();
    }

    @Test
    void fieldMappingSerializesDefaultUnderItsWireName() {
        server.enqueue(201, "{\"destination\":{\"id\":\"dst_1\"}}");
        destinations.create(new CreateDestinationParams().name("W").type("r2")
                .fieldMapping(List.of(new DestinationFieldMapping("$.ts", "received_at", "timestamp", "now()"))));
        assertThat(server.last().body().get("fieldMapping")).isEqualTo(MockServer.json(
                "[{\"source\":\"$.ts\",\"target\":\"received_at\",\"type\":\"timestamp\",\"default\":\"now()\"}]"));
    }
}
