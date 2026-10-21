package app.hookbase.models;

import app.hookbase.internal.JsonTextDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

/**
 * A routing filter.
 *
 * @param logic how conditions combine: {@code AND} or {@code OR}
 */
public record Filter(
        String id,
        String organizationId,
        String name,
        String slug,
        String description,
        @JsonDeserialize(using = JsonTextDeserializer.class) List<FilterCondition> conditions,
        String logic,
        boolean isActive,
        String createdAt,
        String updatedAt) {}
