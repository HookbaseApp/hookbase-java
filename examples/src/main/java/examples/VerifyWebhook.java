package examples;

import app.hookbase.errors.WebhookVerificationException;
import app.hookbase.webhooks.Webhook;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Receive webhooks from Hookbase and verify their signatures, with the JDK's built-in HTTP server.
 *
 * <pre>
 * HOOKBASE_WEBHOOK_SECRET=whsec_... ./gradlew runExample -Pexample=VerifyWebhook
 * </pre>
 *
 * The same three lines work in any framework: read the body as bytes, collect the headers into a
 * map, call {@code verify}.
 */
public final class VerifyWebhook {

    public static void main(String[] args) throws IOException {
        Webhook webhook = new Webhook(System.getenv("HOOKBASE_WEBHOOK_SECRET"));

        HttpServer server = HttpServer.create(new InetSocketAddress(3000), 0);
        server.createContext("/webhooks", exchange -> {
            byte[] body;
            try (InputStream in = exchange.getRequestBody()) {
                body = in.readAllBytes(); // the exact bytes that were signed
            }
            Map<String, String> headers = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> h : exchange.getRequestHeaders().entrySet()) {
                headers.put(h.getKey(), h.getValue().getFirst());
            }

            int status;
            try {
                JsonNode event = webhook.verify(body, headers);
                System.out.println("Verified " + headers.get("Webhook-id") + ": " + event);
                status = 204;
            } catch (WebhookVerificationException e) {
                System.out.println("Rejected: " + e.getMessage());
                status = 401;
            }
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        System.out.println("Listening on http://localhost:3000/webhooks");

        // Try it: headers a real delivery would carry, for a payload you choose.
        String payload = "{\"type\":\"order.created\",\"data\":{\"orderId\":\"ord_123\"}}";
        System.out.println("Test headers: " + webhook.generateTestHeaders(payload));
    }
}
