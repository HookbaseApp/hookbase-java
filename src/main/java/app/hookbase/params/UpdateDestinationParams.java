package app.hookbase.params;

import app.hookbase.models.DestinationFieldMapping;
import app.hookbase.models.DestinationThrottle;
import java.util.List;
import java.util.Map;

/**
 * Body for {@code destinations().update()}. Only the fields you set are changed; set
 * {@code headers}, {@code authType}, {@code authConfig}, {@code throttle} (resets to {@code off}),
 * {@code config} or {@code fieldMapping} to {@code null} to clear it. A destination's slug and type
 * cannot be changed.
 */
public final class UpdateDestinationParams extends Params<UpdateDestinationParams> {
    public UpdateDestinationParams name(String name) {
        return set("name", name);
    }

    public UpdateDestinationParams url(String url) {
        return set("url", url);
    }

    public UpdateDestinationParams method(String method) {
        return set("method", method);
    }

    public UpdateDestinationParams headers(Map<String, String> headers) {
        return set("headers", headers);
    }

    public UpdateDestinationParams authType(String authType) {
        return set("authType", authType);
    }

    public UpdateDestinationParams authConfig(Map<String, String> authConfig) {
        return set("authConfig", authConfig);
    }

    /** Request timeout in milliseconds, 1000-60000. */
    public UpdateDestinationParams timeoutMs(int timeoutMs) {
        return set("timeoutMs", timeoutMs);
    }

    public UpdateDestinationParams throttle(DestinationThrottle throttle) {
        return set("throttle", throttle);
    }

    public UpdateDestinationParams isActive(boolean isActive) {
        return set("isActive", isActive);
    }

    /** See {@link CreateDestinationParams#config}. */
    public UpdateDestinationParams config(Map<String, ?> config) {
        return set("config", config);
    }

    public UpdateDestinationParams fieldMapping(List<DestinationFieldMapping> fieldMapping) {
        return set("fieldMapping", fieldMapping);
    }

    public UpdateDestinationParams useStaticIp(boolean useStaticIp) {
        return set("useStaticIp", useStaticIp);
    }

    public UpdateDestinationParams batchSize(int batchSize) {
        return set("batchSize", batchSize);
    }

    public UpdateDestinationParams batchWindowSeconds(int batchWindowSeconds) {
        return set("batchWindowSeconds", batchWindowSeconds);
    }
}
