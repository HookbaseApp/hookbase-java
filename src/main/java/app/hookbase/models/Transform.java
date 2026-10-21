package app.hookbase.models;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * A payload transform.
 *
 * @param transformType {@code jsonata}, {@code liquid}, {@code xslt} or {@code javascript}
 * @param inputFormat {@code json}, {@code xml} or {@code text}
 * @param outputFormat {@code json}, {@code xml} or {@code text}
 */
public record Transform(
        String id,
        String organizationId,
        String name,
        String slug,
        String description,
        @JsonAlias("transform_type") String transformType,
        String code,
        @JsonAlias("input_format") String inputFormat,
        @JsonAlias("output_format") String outputFormat,
        boolean isActive,
        String createdAt,
        String updatedAt) {}
