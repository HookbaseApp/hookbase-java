package app.hookbase.models;

/**
 * DLQ messages for one endpoint, in {@link DlqStats#byEndpoint()}.
 *
 * @param endpointUrl {@code null} when the endpoint has since been deleted
 */
public record DlqEndpointCount(String endpointId, String endpointUrl, long count) {}
