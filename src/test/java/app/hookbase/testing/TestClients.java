package app.hookbase.testing;

import app.hookbase.Hookbase;
import java.lang.reflect.Method;
import java.time.Duration;

/** Builds clients for tests. */
public final class TestClients {
    private TestClients() {}

    /** A client whose retries record nothing and don't sleep. */
    public static Hookbase noSleep(String baseUrl, int retries) {
        Hookbase.Builder builder = Hookbase.builder().apiKey("whr_test").baseUrl(baseUrl).maxRetries(retries).timeout(Duration.ofSeconds(5));
        try {
            // Builder.sleeper is package-private to keep it out of the public API.
            Method m = Hookbase.Builder.class.getDeclaredMethod("sleeper", app.hookbase.internal.HttpTransport.Sleeper.class);
            m.setAccessible(true);
            m.invoke(builder, (app.hookbase.internal.HttpTransport.Sleeper) d -> {});
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return builder.build();
    }
}
