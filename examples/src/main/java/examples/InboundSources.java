package examples;

import app.hookbase.Hookbase;
import app.hookbase.errors.NotFoundException;
import app.hookbase.models.CatalogProviderDetail;
import app.hookbase.models.Destination;
import app.hookbase.models.Event;
import app.hookbase.models.KnownSourceProviders;
import app.hookbase.models.SourceWithSecret;
import app.hookbase.params.CreateDestinationParams;
import app.hookbase.params.CreateRouteParams;
import app.hookbase.params.CreateSourceParams;
import app.hookbase.params.ListEventsParams;

/**
 * Receive webhooks from a provider: create a source, a destination and a route between them, then
 * read the events that arrive.
 *
 * <pre>
 * HOOKBASE_API_KEY=whr_... ./gradlew runExample -Pexample=InboundSources
 * </pre>
 */
public final class InboundSources {

    public static void main(String[] args) {
        Hookbase hookbase = Hookbase.builder()
                .apiKey(System.getenv("HOOKBASE_API_KEY"))
                .baseUrl(System.getenv().getOrDefault("HOOKBASE_BASE_URL", "https://api.hookbase.app"))
                .build();

        // Which secret does GitHub need, and where does it come from?
        CatalogProviderDetail github = hookbase.providers().get(KnownSourceProviders.GITHUB);
        if (github.signatureVerification() != null) {
            System.out.println("GitHub secret source: " + github.signatureVerification().secretSource());
        }

        // GitHub lets you choose the secret; leave it out and Hookbase generates one.
        SourceWithSecret source = hookbase.sources().create(new CreateSourceParams()
                .name("GitHub")
                .slug("github-" + Long.toString(System.currentTimeMillis(), 36))
                .provider(KnownSourceProviders.GITHUB));
        System.out.println("Point GitHub at /ingest/<org>/" + source.slug() + " with the secret from revealSecret()");

        Destination destination = hookbase.destinations().create(new CreateDestinationParams()
                .name("Build service")
                .url("https://example.com/builds"));
        hookbase.routes().create(new CreateRouteParams()
                .name("GitHub to builds")
                .sourceId(source.id())
                .destinationId(destination.id()));

        // sourceId takes an id or a slug.
        for (Event event : hookbase.events().list(new ListEventsParams().sourceId(source.slug()).limit(10)).data()) {
            System.out.println(event.id() + " " + event.eventType() + " " + event.status());
        }

        try {
            hookbase.sources().get("no-such-source");
        } catch (NotFoundException e) {
            System.out.println("Not found: " + e.getMessage());
        }

        hookbase.sources().delete(source.id());
        hookbase.destinations().delete(destination.id());
    }
}
