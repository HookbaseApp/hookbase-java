package examples;

import app.hookbase.Hookbase;
import app.hookbase.RequestOptions;
import app.hookbase.errors.HookbaseApiException;
import app.hookbase.errors.HookbaseException;
import app.hookbase.errors.HookbaseNetworkException;
import app.hookbase.errors.HookbaseTimeoutException;
import app.hookbase.errors.NotFoundException;
import app.hookbase.errors.RateLimitException;
import app.hookbase.errors.ValidationException;
import app.hookbase.params.CreateEndpointParams;
import java.time.Duration;

/**
 * Every failure is a {@link HookbaseException}; its subclasses say what went wrong.
 *
 * <pre>
 * HOOKBASE_BASE_URL=http://localhost:8787 HOOKBASE_API_KEY=whr_... ./gradlew runExample -Pexample=ErrorHandling
 * </pre>
 *
 * HOOKBASE_BASE_URL is required: the examples never default to the production API.
 */
public final class ErrorHandling {

    public static void main(String[] args) {
        String baseUrl = System.getenv("HOOKBASE_BASE_URL");
        if (baseUrl == null || baseUrl.isBlank()) {
            System.err.println("Set HOOKBASE_BASE_URL (e.g. http://localhost:8787 or your staging API); the examples never default to production.");
            System.exit(1);
        }
        Hookbase hookbase = Hookbase.builder()
                .apiKey(System.getenv("HOOKBASE_API_KEY"))
                .baseUrl(baseUrl)
                .timeout(Duration.ofSeconds(10))
                .maxRetries(2)
                .build();

        try {
            hookbase.endpoints().create("app_does_not_exist", new CreateEndpointParams().url("not a url"),
                    RequestOptions.builder().timeout(Duration.ofSeconds(5)).build());
        } catch (ValidationException e) {
            System.out.println("Invalid: " + e.getMessage() + " " + e.fieldErrors());
        } catch (NotFoundException e) {
            System.out.println("Not found: " + e.getMessage());
        } catch (RateLimitException e) {
            System.out.println("Rate limited; retry after " + e.retryAfter());
        } catch (HookbaseApiException e) {
            System.out.println("API error " + e.status() + " " + e.code() + " (request " + e.requestId() + ")");
        } catch (HookbaseTimeoutException | HookbaseNetworkException e) {
            System.out.println("Could not reach Hookbase: " + e.getMessage());
        }
    }
}
