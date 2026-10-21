package app.hookbase.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** The SDK's version, written into {@code version.properties} from gradle.properties at build time. */
public final class SdkVersion {

    public static final String VERSION = load();

    private SdkVersion() {}

    private static String load() {
        try (InputStream in = SdkVersion.class.getResourceAsStream("/app/hookbase/version.properties")) {
            if (in == null) return "unknown";
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("version", "unknown");
        } catch (IOException e) {
            return "unknown";
        }
    }
}
