package app.hookbase.params;

import app.hookbase.models.DestinationFieldMapping;
import app.hookbase.models.DestinationThrottle;
import java.util.List;
import java.util.Map;

/**
 * Body for {@code destinations().create()}. {@code name} is required, and {@code url} for
 * {@code http} destinations.
 */
public final class CreateDestinationParams extends Params<CreateDestinationParams> {
    public CreateDestinationParams name(String name) {
        return set("name", name);
    }

    /**
     * URL-safe identifier, {@code ^[a-z0-9-]+$}, max 50 characters. Required by the API; leave it
     * unset (or blank) and one is derived from {@code name}, the same way every Hookbase SDK
     * derives it ({@code "Café EU"} gives {@code "cafe-eu"}). Set it explicitly whenever the slug
     * matters: it cannot be changed afterwards. A slug you set is sent unchanged.
     */
    public CreateDestinationParams slug(String slug) {
        return set("slug", slug);
    }

    /**
     * {@code http} (the default), a queue ({@code sqs}, {@code eventbridge}, {@code servicebus},
     * {@code pubsub}, {@code oci_queue}) or a warehouse ({@code s3}, {@code r2}, {@code gcs},
     * {@code azure_blob}).
     */
    public CreateDestinationParams type(String type) {
        return set("type", type);
    }

    /** Required for {@code http}; queue and warehouse destinations leave it unset. */
    public CreateDestinationParams url(String url) {
        return set("url", url);
    }

    /** {@code GET}, {@code POST} (the default), {@code PUT}, {@code PATCH} or {@code DELETE}. */
    public CreateDestinationParams method(String method) {
        return set("method", method);
    }

    /** Sent as a name-to-value record, the shape the destinations API takes. */
    public CreateDestinationParams headers(Map<String, String> headers) {
        return set("headers", headers);
    }

    /** {@code none}, {@code basic}, {@code bearer}, {@code api_key} or {@code custom_header}. */
    public CreateDestinationParams authType(String authType) {
        return set("authType", authType);
    }

    /** Credentials for {@code authType}, e.g. {@code {"token": "..."}}. String values only. */
    public CreateDestinationParams authConfig(Map<String, String> authConfig) {
        return set("authConfig", authConfig);
    }

    /** Request timeout in milliseconds, 1000-60000. The API defaults to 30000. */
    public CreateDestinationParams timeoutMs(int timeoutMs) {
        return set("timeoutMs", timeoutMs);
    }

    public CreateDestinationParams throttle(DestinationThrottle throttle) {
        return set("throttle", throttle);
    }

    /**
     * Queue or warehouse configuration: for {@code s3} {@code bucket}, {@code region},
     * {@code accessKeyId}, {@code secretAccessKey}; for {@code r2} {@code bucket}; for {@code gcs}
     * {@code bucket}, {@code projectId}, {@code serviceAccountKey}; for {@code azure_blob}
     * {@code accountName}, {@code accountKey}, {@code containerName}; warehouses also take
     * {@code prefix}, {@code fileFormat} ({@code json}/{@code jsonl}) and {@code partitionBy}
     * ({@code date}/{@code hour}/{@code source}).
     */
    public CreateDestinationParams config(Map<String, ?> config) {
        return set("config", config);
    }

    /** Warehouse column mapping, at most 50 entries. */
    public CreateDestinationParams fieldMapping(List<DestinationFieldMapping> fieldMapping) {
        return set("fieldMapping", fieldMapping);
    }

    public CreateDestinationParams useStaticIp(boolean useStaticIp) {
        return set("useStaticIp", useStaticIp);
    }

    /** Warehouse batch size, 1-1000. */
    public CreateDestinationParams batchSize(int batchSize) {
        return set("batchSize", batchSize);
    }

    /** Warehouse batch window in seconds, 0-300. */
    public CreateDestinationParams batchWindowSeconds(int batchWindowSeconds) {
        return set("batchWindowSeconds", batchWindowSeconds);
    }
}
