# hookbase-java

Official Java SDK for [Hookbase](https://hookbase.app) - the webhook management platform.

## Installation

Requires Java 25 or newer.

Gradle (Kotlin DSL):

```kotlin
dependencies {
    implementation("app.hookbase:hookbase-java:1.1.0")
}
```

Maven:

```xml
<dependency>
  <groupId>app.hookbase</groupId>
  <artifactId>hookbase-java</artifactId>
  <version>1.1.0</version>
</dependency>
```

The only dependency is Jackson (`jackson-databind`). HTTP uses the JDK's `java.net.http.HttpClient`.

## Quick Start

```java
import app.hookbase.Hookbase;
import app.hookbase.params.SendMessageParams;
import java.util.Map;

Hookbase hookbase = new Hookbase(System.getenv("HOOKBASE_API_KEY"));

// Send a webhook
hookbase.messages().send("app_123", new SendMessageParams()
        .eventType("order.created")
        .payload(Map.of("orderId", "ord_123", "amount", 99.99)));
```

## Features

- **Full API Coverage**: outbound (applications, endpoints, event types, subscriptions, messages, portal tokens, DLQ), inbound (sources, destinations, routes, events, deliveries, transforms, filters, schemas, provider catalog) and admin (analytics, API keys, tunnels, cron jobs)
- **Webhook Verification**: verify incoming webhook signatures (Standard Webhooks)
- **Automatic Retries**: exponential backoff, without ever repeating a write that may have gone through
- **Pagination Helpers**: every paginated list has a lazy `listAll` you can iterate or `stream()`
- **Error Handling**: a sealed exception hierarchy, one class per failure mode
- **Small**: immutable, thread-safe client; blocking calls that work well on virtual threads

## API Reference

### Client Initialization

```java
import app.hookbase.Hookbase;
import java.time.Duration;

// Option 1: defaults (https://api.hookbase.app, 30 s timeout, 3 retries)
Hookbase hookbase = new Hookbase(System.getenv("HOOKBASE_API_KEY"));

// Option 2: builder
Hookbase hookbase = Hookbase.builder()
        .apiKey(System.getenv("HOOKBASE_API_KEY"))
        .baseUrl("https://api.hookbase.app") // optional; must be https (http only for localhost)
        .timeout(Duration.ofSeconds(30))       // optional
        .maxRetries(3)                         // optional
        .debug(false)                          // optional: log method + URL to System.Logger "app.hookbase"
        .build();
```

Create one client per API key and share it. Every call is blocking, so run calls on virtual threads
(`Executors.newVirtualThreadPerTaskExecutor()`) to make many at once.

Request bodies are built with fluent params classes. A setter you don't call is left out of the
request. On update params, setting a field to `null` sends JSON `null`, which clears the field
where the API allows it (each class's javadoc lists which). `extra(key, value)` sends a field this
SDK version doesn't model yet.

### Applications

```java
// Create
Application app = hookbase.applications().create(new CreateApplicationParams()
        .name("Acme Corp")
        .externalId("customer_123")
        .metadata(Map.of("plan", "enterprise")));

// Get or create by your own id (PUT /upsert)
Application app = hookbase.applications().getOrCreate("customer_123",
        new CreateApplicationParams().name("Acme Corp"));

// Get by id, or by your own id
Application app = hookbase.applications().get("app_123");
Application app = hookbase.applications().getByUid("customer_123");

// One page
Page<Application> page = hookbase.applications().list(new ListApplicationsParams().limit(50));

// Every application, fetched page by page as you iterate
for (Application a : hookbase.applications().listAll()) {
    System.out.println(a.name());
}

// Update and delete
hookbase.applications().update("app_123", new UpdateApplicationParams().name("Acme Inc"));
hookbase.applications().delete("app_123");
```

### Endpoints

```java
// Create. The signing secret is returned only here; give it to your customer.
Endpoint endpoint = hookbase.endpoints().create("app_123", new CreateEndpointParams()
        .url("https://example.com/webhooks")
        .description("Production")
        .headers(Map.of("X-Customer", "acme"))
        .timeoutSeconds(30));
String secret = endpoint.secret();

// List, get, update
Page<Endpoint> page = hookbase.endpoints().list("app_123");
Endpoint ep = hookbase.endpoints().get("app_123", "ep_123");
hookbase.endpoints().update("app_123", "ep_123", new UpdateEndpointParams()
        .url("https://example.com/webhooks/v2")
        .description(null)); // clear it

// Rotate the secret; the old one keeps signing for the grace period
EndpointRotateSecretResult rotated = hookbase.endpoints().rotateSecret("app_123", "ep_123",
        new RotateEndpointSecretParams().gracePeriodSeconds(3600));

// Enable, disable, stats, circuit breaker
hookbase.endpoints().disable("app_123", "ep_123");
hookbase.endpoints().enable("app_123", "ep_123");
EndpointStats stats = hookbase.endpoints().getStats("app_123", "ep_123");
hookbase.endpoints().recoverCircuit("app_123", "ep_123");

// Send a test event
hookbase.endpoints().test("app_123", "ep_123");

hookbase.endpoints().delete("app_123", "ep_123");
```

### Event Types

```java
EventType type = hookbase.eventTypes().create(new CreateEventTypeParams()
        .name("order.created")
        .displayName("Order Created")
        .category("orders")
        .schema(Map.of("type", "object", "required", List.of("orderId")))
        .examplePayload(Map.of("orderId", "ord_123")));

EventType same = hookbase.eventTypes().getByName("order.created");
EventType ensured = hookbase.eventTypes().getOrCreate("order.created", new CreateEventTypeParams());

hookbase.eventTypes().archive(type.id());
hookbase.eventTypes().unarchive(type.id());

for (EventType t : hookbase.eventTypes().listAll(new ListEventTypesParams().category("orders"))) {
    System.out.println(t.name());
}
```

### Subscriptions

```java
// Subscribe an endpoint to an event type
Subscription sub = hookbase.subscriptions().create("app_123", new CreateSubscriptionParams()
        .endpointId("ep_123")
        .eventTypeId("evt_type_123"));

// Several at once
List<Subscription> subs = hookbase.subscriptions().subscribeToMany("app_123", "ep_123",
        List.of("evt_type_1", "evt_type_2"));

hookbase.subscriptions().disable("app_123", sub.id());
hookbase.subscriptions().enable("app_123", sub.id());

// Remove every subscription of an endpoint
hookbase.subscriptions().unsubscribeAll("app_123", "ep_123");
```

### Messages

```java
// Send an event to every subscribed endpoint of the application
SendMessageResponse sent = hookbase.messages().send("app_123", new SendMessageParams()
        .eventType("order.created")
        .payload(Map.of("orderId", "ord_123"))
        .labels(Map.of("region", "eu"))
        .idempotencyKey("order-ord_123-created")); // optional

System.out.println(sent.eventId() + " queued to " + sent.messagesQueued() + " endpoint(s)");

// Or to particular endpoints only
hookbase.messages().send("app_123", new SendMessageParams()
        .eventType("order.created")
        .payload(Map.of("orderId", "ord_123"))
        .endpointIds(List.of("ep_123")));

// Outbound messages and their delivery attempts
for (OutboundMessage m : hookbase.messages().listAll("app_123", new ListMessagesParams().status("failed"))) {
    List<MessageAttempt> attempts = hookbase.messages().listAttempts("app_123", m.id());
}

// Replay a failed message (creates a new message)
MessageReplayResult replay = hookbase.messages().retry("app_123", "msg_123");

// Counts by status, and exports
MessageStatsSummary summary = hookbase.messages().getStatsSummary();
String csv = hookbase.messages().exportCsv(new ExportMessagesParams().status("failed"));
```

Every `send` carries an idempotency key: the one you set, else one from `RequestOptions`, else a
generated UUID. The API deduplicates on it, so a send retried after a timeout is delivered once.

### Portal Tokens

```java
PortalToken token = hookbase.portalTokens().create("app_123", new CreatePortalTokenParams()
        .name("Customer dashboard")
        .scopes(List.of("read", "write"))
        .expiresInDays(30));
String plaintext = token.token(); // only returned on create

List<PortalToken> tokens = hookbase.portalTokens().list("app_123");
hookbase.portalTokens().revoke("app_123", token.id());
```

### Sources (inbound)

`provider` is any string: `KnownSourceProviders` lists the values the API accepts today, and an
unknown one is a 400 from the API. Which secret a provider needs is in the provider catalog.

```java
// Find out which secret a provider needs before creating the source
CatalogProviderDetail stripeInfo = hookbase.providers().get(KnownSourceProviders.STRIPE);
System.out.println(stripeInfo.signatureVerification().secretSource()); // "provider": paste the whsec_ secret

SourceWithSecret stripe = hookbase.sources().create(new CreateSourceParams()
        .name("Stripe")
        .slug("stripe")
        .provider(KnownSourceProviders.STRIPE)
        .signingSecret("whsec_...")); // vendor-issued: required, format-checked

// Providers that verify with no secret (wise, aws-sns) return a null signingSecret
SourceWithSecret wise = hookbase.sources().create(new CreateSourceParams()
        .name("Wise").slug("wise").provider(KnownSourceProviders.WISE));
wise.verifiesWithoutSecret(); // true

// WhatsApp (Meta) sources also carry a verify token
SourceWithSecret wa = hookbase.sources().create(new CreateSourceParams()
        .name("WhatsApp").slug("wa").provider(KnownSourceProviders.WHATSAPP));
System.out.println(wa.verifyToken());
String newToken = hookbase.sources().rotateVerifyToken(wa.id());

// Notion parks its verification token until you adopt it
PendingVerification pending = hookbase.sources().getPendingVerification("notion");
hookbase.sources().adoptPendingVerification("notion");

// Rotate a Hookbase-generated secret (400 for vendor-issued, public-key and built-in ones)
String newSecret = hookbase.sources().rotateSecret("src_123");

// sourceId filters take an id or a slug; an unknown one is a 404, not an empty list
Page<Event> events = hookbase.events().list(new ListEventsParams().sourceId("stripe"));
```

Destinations, routes, events, deliveries, transforms, filters, schemas, analytics, API keys,
tunnels and cron jobs follow the same pattern; see the javadoc of each resource class, and
[examples/InboundSources.java](./examples/src/main/java/examples/InboundSources.java).

The sources, destinations and routes lists are paged (`page`, `pageSize`) but not filtered by the
API yet, so their `search`, `provider`, `isActive`, `sourceId` and `destinationId` setters are
deprecated: the list comes back unfiltered, and you filter the items yourself.

## Webhook Verification

Verify incoming webhooks in your application. Always verify the **raw bytes** of the body: JSON
that was parsed and re-serialized no longer matches the signature.

```java
import app.hookbase.errors.WebhookVerificationException;
import app.hookbase.webhooks.Webhook;

Webhook webhook = new Webhook(System.getenv("HOOKBASE_WEBHOOK_SECRET")); // whsec_...

// Spring Boot example
@PostMapping("/webhooks")
ResponseEntity<String> receive(@RequestBody byte[] body, @RequestHeader HttpHeaders headers) {
    try {
        JsonNode event = webhook.verify(body, headers.toSingleValueMap());

        // Process the verified payload
        System.out.println("Event: " + event.path("type").asText());

        return ResponseEntity.ok("OK");
    } catch (WebhookVerificationException e) {
        return ResponseEntity.status(401).body("Invalid signature");
    }
}
```

`verify` checks the `webhook-id`, `webhook-timestamp` and `webhook-signature` headers (names are
case-insensitive), rejects timestamps more than 5 minutes from now, and accepts any of the
space-separated signatures sent during a secret rotation. Pass a `Duration` to change the
tolerance, or a class to bind the payload: `webhook.verify(body, headers, OrderEvent.class)`.

Take the body as `byte[]`, not `String`: Hookbase sends `application/json` without a charset, and
servlet containers then decode text as ISO-8859-1, which breaks any non-ASCII payload.

### Ed25519 (v1a) endpoints

An endpoint created with `signatureScheme("ed25519")` is signed asymmetrically (Standard Webhooks
`v1a`). Hookbase keeps the private key; you verify with the endpoint's public key, `whpk_...`, from
`endpoint.publicKey()` (or `publicKey()` on the rotate response). Pass it to the same constructor;
the prefix picks the scheme, and tolerance and rotation handling are the same as for `v1`:

```java
Endpoint endpoint = hookbase.endpoints().create(appId,
        new CreateEndpointParams().url("https://example.com/webhooks").signatureScheme("ed25519"));

Webhook webhook = new Webhook(endpoint.publicKey()); // whpk_...
JsonNode event = webhook.verify(body, headers.toSingleValueMap());
```

During a key rotation deliveries carry `v1a,<old> v1a,<new>`; keep verifying with the old public
key (`previousPublicKey()`) until you switch, as either entry matching is enough.

### Testing Webhooks

Generate test headers for local development:

```java
Webhook webhook = new Webhook("whsec_dGVzdF9zZWNyZXRfa2V5X2Zvcl90ZXN0aW5n");

String payload = "{\"orderId\":\"test_123\"}";
Map<String, String> headers = webhook.generateTestHeaders(payload);

// Use these headers to test your webhook handler
```

For an Ed25519 handler, generate a key pair and sign with its secret key:

```java
Webhook.Ed25519KeyPair pair = Webhook.generateEd25519KeyPair();
Map<String, String> headers = new Webhook(pair.secretKey()).generateTestHeaders(payload); // v1a,...
new Webhook(pair.publicKey()).verify(payload, headers);
```

## Error Handling

Every failure is a `HookbaseException` (unchecked). The hierarchy is sealed, so a `switch` over it
can be exhaustive:

```java
import app.hookbase.errors.*;

// HookbaseException                  base class
//   HookbaseApiException             any error response: status(), code(), details()
//     AuthenticationException        401
//     ForbiddenException             403
//     NotFoundException              404
//     ConflictException              409
//     ValidationException            400 / 422, with fieldErrors()
//     RateLimitException             429, with retryAfter()
//   HookbaseTimeoutException         the request timed out
//   HookbaseNetworkException         could not connect, or the connection dropped
//   WebhookVerificationException     signature verification failed

try {
    hookbase.endpoints().get("app_123", "invalid_id");
} catch (NotFoundException e) {
    System.out.println("Endpoint not found");
} catch (RateLimitException e) {
    System.out.println("Rate limited, retry after " + e.retryAfter());
} catch (ValidationException e) {
    System.out.println("Invalid: " + e.fieldErrors());
} catch (HookbaseApiException e) {
    System.out.println("API error: " + e.getMessage() + " (" + e.code() + ")");
}
```

### Retries

Failed requests are retried up to `maxRetries` times (default 3):

- **429** is retried for every method, POST included (the API refused it, so nothing ran), after
  the `Retry-After` the API sends, when it is 60 seconds or less. A 429 without one (a plan or
  monthly quota) is thrown at once, with `retryAfter()` returning `null`.
- **500, 502, 503, 504, timeouts and dropped connections** are retried with exponential backoff
  (1 s, 2 s, 4 s ... up to 10 s, plus jitter), for every method except POST. A POST may already
  have been carried out, so it is retried only when sending an event (the API deduplicates on its
  idempotency key). Setting `RequestOptions.retries` does not change this.
- **A connection that was never made** (refused, DNS failure) is always retried.
- Every other error is thrown straight away.

## Request Options

Override defaults for specific requests. Every method has an overload that takes `RequestOptions`
as its last argument:

```java
import app.hookbase.RequestOptions;

SendMessageResponse sent = hookbase.messages().send("app_123",
        new SendMessageParams().eventType("order.created").payload(Map.of()),
        RequestOptions.builder()
                .timeout(Duration.ofSeconds(60)) // custom timeout
                .retries(5)                      // custom retry count
                .idempotencyKey("unique_key")    // for safe retries
                .build());
```

For an endpoint this SDK version doesn't wrap yet, `hookbase.request(method, path, query, body,
options)` sends a raw request with the same auth, retries and error mapping, and returns a Jackson
`JsonNode`.

## Models

Responses are immutable Java records in `app.hookbase.models`, named after the API's resources
(`Application`, `Endpoint`, `EventType`, `Subscription`, `OutboundMessage`, `Source`, `Event`,
`Delivery` ...). Field names match the API's JSON. Fields the API adds later are ignored until the
SDK models them. Timestamps are ISO-8601 strings (`Instant.parse(app.createdAt())`), and
free-form JSON such as payloads and metadata is a Jackson `JsonNode`. Records that carry
credentials (`ApiKeyWithSecret`, `SourceWithSecret`, `Endpoint`, `PortalToken` ...) print them as
`<redacted>` in `toString()`, so logging one is safe.

## Examples

See the [examples](./examples/src/main/java/examples) directory for complete examples:

- [Basic Usage](./examples/src/main/java/examples/BasicUsage.java)
- [Verifying Webhooks](./examples/src/main/java/examples/VerifyWebhook.java)
- [Inbound Sources](./examples/src/main/java/examples/InboundSources.java)
- [Error Handling](./examples/src/main/java/examples/ErrorHandling.java)

Run one with `HOOKBASE_BASE_URL=http://localhost:8787 HOOKBASE_API_KEY=whr_... ./gradlew runExample -Pexample=BasicUsage`.
`HOOKBASE_BASE_URL` is required: the examples create and delete resources, so they never default to
the production API. Point it at a local or staging API.

## License

MIT
