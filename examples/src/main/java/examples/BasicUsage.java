package examples;

import app.hookbase.Hookbase;
import app.hookbase.models.Application;
import app.hookbase.models.Endpoint;
import app.hookbase.models.EventType;
import app.hookbase.models.MessageAttempt;
import app.hookbase.models.OutboundMessage;
import app.hookbase.models.SendMessageResponse;
import app.hookbase.params.CreateApplicationParams;
import app.hookbase.params.CreateEndpointParams;
import app.hookbase.params.CreateEventTypeParams;
import app.hookbase.params.CreateSubscriptionParams;
import app.hookbase.params.SendMessageParams;
import java.util.List;
import java.util.Map;

/**
 * Send webhooks to a customer: create an application, an endpoint and an event type, subscribe the
 * endpoint, send an event, and read the delivery attempts.
 *
 * <pre>
 * HOOKBASE_API_KEY=whr_... ./gradlew runExample -Pexample=BasicUsage
 * </pre>
 *
 * Set HOOKBASE_BASE_URL to point at another API (e.g. http://localhost:8787).
 */
public final class BasicUsage {

    public static void main(String[] args) throws InterruptedException {
        Hookbase hookbase = Hookbase.builder()
                .apiKey(System.getenv("HOOKBASE_API_KEY"))
                .baseUrl(System.getenv().getOrDefault("HOOKBASE_BASE_URL", "https://api.hookbase.app"))
                .build();

        String suffix = Long.toString(System.currentTimeMillis(), 36);

        // One application per customer, keyed by your own id.
        Application app = hookbase.applications().getOrCreate("customer-" + suffix,
                new CreateApplicationParams().name("Acme Corp"));

        // Where the customer receives webhooks. The secret is shown only on create.
        Endpoint endpoint = hookbase.endpoints().create(app.id(), new CreateEndpointParams()
                .url("https://example.com/webhooks")
                .description("Production")
                .headers(Map.of("X-Customer", "acme")));
        System.out.println("Endpoint " + endpoint.id() + ": give the customer its secret, " + endpoint.secret().substring(0, 10) + "...");

        EventType orderCreated = hookbase.eventTypes().getOrCreate("order.created." + suffix,
                new CreateEventTypeParams().description("An order was placed"));

        hookbase.subscriptions().create(app.id(), new CreateSubscriptionParams()
                .endpointId(endpoint.id())
                .eventTypeId(orderCreated.id()));

        // Every send carries an idempotency key (yours, or a generated one), so a retried send
        // can't deliver twice.
        SendMessageResponse sent = hookbase.messages().send(app.id(), new SendMessageParams()
                .eventType(orderCreated.name())
                .payload(Map.of("orderId", "ord_123", "amount", 99.99))
                .idempotencyKey("order-ord_123-created"));
        System.out.println("Queued to " + sent.messagesQueued() + " endpoint(s), event " + sent.eventId());

        Thread.sleep(2000);
        for (OutboundMessage message : hookbase.messages().listAll(app.id())) {
            List<MessageAttempt> attempts = hookbase.messages().listAttempts(app.id(), message.id());
            System.out.println(message.id() + " " + message.status() + ", " + attempts.size() + " attempt(s)");
        }

        hookbase.applications().delete(app.id());
        hookbase.eventTypes().delete(orderCreated.id());
    }
}
