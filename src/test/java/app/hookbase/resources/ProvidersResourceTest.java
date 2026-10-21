package app.hookbase.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.hookbase.errors.NotFoundException;
import app.hookbase.models.CatalogProviderDetail;
import app.hookbase.models.ProviderCatalogList;
import app.hookbase.models.ProviderEvents;
import app.hookbase.params.ListProvidersParams;
import app.hookbase.testing.MockServer;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProvidersResourceTest {

    static final String CATALOG = """
            {"providers":[
              {"id":"stripe","name":"Stripe","category":"payments","description":"Payments","icon":"x","eventTypeCount":12,
               "hookbaseProvider":"stripe","hasSignatureVerification":true,"hasIpRanges":true,
               "signatureVerification":{"supported":true,"kind":"hmac","secretSource":"provider",
                 "secretLabel":"Signing secret","secretPlaceholder":"whsec_..."}},
              {"id":"mollie-nextgen","name":"Mollie","category":"payments","description":"","icon":"m","eventTypeCount":3,
               "hookbaseProvider":"mollie","hasSignatureVerification":true,"hasIpRanges":false}],
             "categories":["payments"],"total":2}""";

    private MockServer server;
    private ProvidersResource providers;

    @BeforeEach
    void start() {
        server = new MockServer();
        providers = server.client().providers();
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void listSendsCategoryAndSearch() {
        server.enqueue(200, CATALOG);
        ProviderCatalogList list = providers.list(new ListProvidersParams().category("payments").search("str"));
        assertThat(server.last().method()).isEqualTo("GET");
        assertThat(server.last().path()).isEqualTo("/api/catalog/providers");
        assertThat(server.last().query()).containsExactlyInAnyOrderEntriesOf(Map.of("category", "payments", "search", "str"));
        assertThat(list.total()).isEqualTo(2);
        assertThat(list.categories()).containsExactly("payments");
        assertThat(list.providers().get(0).signatureVerification().secretSource()).isEqualTo("provider");
        assertThat(list.providers().get(1).hookbaseProvider()).isEqualTo("mollie");
        assertThat(list.providers().get(1).signatureVerification()).isNull();
    }

    @Test
    void listWithoutFiltersSendsNoQuery() {
        server.enqueue(200, "{\"providers\":[],\"categories\":[],\"total\":0}");
        providers.list();
        assertThat(server.last().query()).isEmpty();
    }

    @Test
    void getUnwrapsTheProvider() {
        server.enqueue(200, """
                {"provider":{"id":"whatsapp","name":"WhatsApp","hookbaseProvider":"whatsapp",
                 "signatureVerification":{"supported":true,"algorithm":"HMAC-SHA256","headerName":"X-Hub-Signature-256",
                   "secretLocationHint":"App settings","kind":"hmac","secretLabel":"App secret","secretSource":"provider",
                   "secretInstruction":"Copy the App secret.","verifyToken":{"label":"Verify token","instruction":"Paste it into Meta."}},
                 "eventTypes":[{"type":"messages","description":"Inbound messages"}],
                 "samplePayloads":{},"ipRanges":null,"setupGuide":[]}}""");
        CatalogProviderDetail detail = providers.get("whatsapp");
        assertThat(server.last().path()).isEqualTo("/api/catalog/providers/whatsapp");
        assertThat(detail.signatureVerification().verifyToken().label()).isEqualTo("Verify token");
        assertThat(detail.signatureVerification().headerName()).isEqualTo("X-Hub-Signature-256");
        assertThat(detail.eventTypes().getFirst().type()).isEqualTo("messages");
        assertThat(detail.ipRanges()).isNull();
    }

    @Test
    void anUnknownProviderIsA404() {
        server.enqueue(404, "{\"error\":\"Provider not found\"}");
        assertThatThrownBy(() -> providers.get("nope")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void eventsListsEventTypesAndSamples() {
        server.enqueue(200, """
                {"providerId":"stripe","providerName":"Stripe",
                 "eventTypes":[{"type":"charge.succeeded","description":"A charge succeeded"}],
                 "samplePayloads":{"charge.succeeded":{"id":"evt_1"}}}""");
        ProviderEvents events = providers.events("stripe");
        assertThat(server.last().path()).isEqualTo("/api/catalog/providers/stripe/events");
        assertThat(events.eventTypes().getFirst().type()).isEqualTo("charge.succeeded");
        assertThat(events.samplePayloads().get("charge.succeeded").get("id").asText()).isEqualTo("evt_1");
    }
}
