package app.hookbase.models;

import java.util.Map;

/** The inbound request behind an event: its headers, payload size in bytes and content type. */
public record EventDebugRequest(Map<String, String> headers, Long payloadSize, String contentType) {}
