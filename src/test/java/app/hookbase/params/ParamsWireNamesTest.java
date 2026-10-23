package app.hookbase.params;

import static org.assertj.core.api.Assertions.assertThat;

import app.hookbase.models.DestinationFieldMapping;
import app.hookbase.models.DestinationThrottle;
import app.hookbase.models.EndpointHeader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every setter of every params class, called once, must produce exactly the wire names the API
 * reads. The expected sets were taken from the API's validators and handlers (api/src/routes: zod
 * schemas, destructured bodies and {@code c.req.query} reads), so a typo in a key, or a setter for a
 * field the API never reads, fails here even when no resource test happens to call that setter.
 */
class ParamsWireNamesTest {

    static Stream<Arguments> params() {
        return Stream.of(
                // inbound
                of(CreateSourceParams.class, "allowedMethods dedupCustomHeader dedupEnabled dedupStrategy dedupWindowHours description encryptFields ipAllowlist ipDenylist ipFilterMode maskFields name provider rateLimitPerMinute rejectInvalidSignatures signingSecret slug transientMode verifyToken"),
                of(UpdateSourceParams.class, "allowedMethods dedupCustomHeader dedupEnabled dedupStrategy dedupWindowHours description encryptFields ipAllowlist ipDenylist ipFilterMode isActive maskFields name provider rateLimitPerMinute rejectInvalidSignatures signingSecret transientMode verifyToken"),
                // search/provider/isActive are deprecated: sent, but the API's list reads only page and pageSize.
                of(ListSourcesParams.class, "isActive page pageSize provider search"),
                of(CreateDestinationParams.class, "authConfig authType batchSize batchWindowSeconds config fieldMapping headers method name slug throttle timeoutMs type url useStaticIp"),
                of(UpdateDestinationParams.class, "authConfig authType batchSize batchWindowSeconds config fieldMapping headers isActive method name throttle timeoutMs url useStaticIp"),
                of(ListDestinationsParams.class, "isActive page pageSize search"),
                of(CreateRouteParams.class, "destinationId expectedResponse failoverAfterAttempts failoverDestinationIds failureThreshold filterConditions filterId filterLogic isActive name notifyEmails notifyOnFailure notifyOnRecovery notifyOnSuccess priority schemaId sourceId transformId"),
                of(UpdateRouteParams.class, "destinationId expectedResponse failoverAfterAttempts failoverDestinationIds failureThreshold filterConditions filterId filterLogic isActive name notifyEmails notifyOnFailure notifyOnRecovery notifyOnSuccess priority schemaId sourceId transformId"),
                of(ListRoutesParams.class, "destinationId isActive page pageSize sourceId"),
                of(CircuitBreakerConfigParams.class, "circuitCooldownSeconds circuitFailureThreshold circuitProbeSuccessThreshold"),
                of(ImportParams.class, "conflictStrategy validateOnly"),
                of(CreateTransformParams.class, "code description inputFormat name outputFormat slug transformType"),
                of(UpdateTransformParams.class, "code description inputFormat isActive name outputFormat transformType"),
                of(TestTransformParams.class, "code inputFormat outputFormat payload transformType"),
                of(ListTransformsParams.class, "page pageSize"),
                of(CreateFilterParams.class, "conditions description logic name slug"),
                of(UpdateFilterParams.class, "conditions description logic name"),
                of(TestFilterParams.class, "conditions logic payload"),
                of(ListFiltersParams.class, "page pageSize"),
                // slug is deprecated: the API derives it from the name.
                of(CreateSchemaParams.class, "description jsonSchema name slug"),
                of(UpdateSchemaParams.class, "description jsonSchema name"),
                of(ListEventsParams.class, "cursor eventType fromDate includeTotal limit search signatureValid sourceId status toDate"),
                of(ExportEventsParams.class, "eventType format fromDate search signatureValid sourceId status toDate"),
                of(ListDeliveriesParams.class, "destinationId eventId limit offset routeId status"),
                of(ListProvidersParams.class, "category search"),
                // outbound
                of(CreateApplicationParams.class, "externalId metadata name"),
                of(UpdateApplicationParams.class, "metadata name"),
                of(ListApplicationsParams.class, "cursor isDisabled limit order search"),
                of(CreateEndpointParams.class, "backoffType circuitCooldownSeconds circuitFailureThreshold circuitSuccessThreshold description headers ipAllowlistNotes rateLimitPerSecond retryDelays signatureScheme successStatusCodes timeoutSeconds url useStaticIp"),
                of(UpdateEndpointParams.class, "backoffType circuitCooldownSeconds circuitFailureThreshold circuitSuccessThreshold description disabledReason headers ipAllowlistNotes isDisabled rateLimitPerSecond retryDelays signatureScheme successStatusCodes timeoutSeconds url useStaticIp"),
                of(ListEndpointsParams.class, "circuitState cursor isDisabled limit"),
                of(RotateEndpointSecretParams.class, "gracePeriodSeconds"),
                of(TestEndpointParams.class, "eventType payload"),
                of(CreateEventTypeParams.class, "category defaultPriority description displayName documentationUrl examplePayload isEnabled name schema validatePayloads"),
                of(UpdateEventTypeParams.class, "category defaultPriority deprecatedMessage description displayName documentationUrl examplePayload isDeprecated isEnabled isPublic schema validatePayloads"),
                of(ListEventTypesParams.class, "category cursor isEnabled limit search"),
                of(CreateSubscriptionParams.class, "endpointId eventTypeId eventVersion filterExpression isEnabled labelFilterMode labelFilters priorityOverride transformId"),
                of(UpdateSubscriptionParams.class, "eventVersion filterExpression isEnabled labelFilterMode labelFilters priorityOverride transformId"),
                of(ListSubscriptionsParams.class, "cursor endpointId eventTypeId isEnabled limit"),
                of(SendMessageParams.class, "endpointId endpointIds eventType eventVersion idempotencyKey labels metadata payload priority"),
                of(ListMessagesParams.class, "cursor endpointId eventType limit status"),
                of(ExportMessagesParams.class, "applicationId endDate eventType limit startDate status type"),
                of(ListDlqParams.class, "applicationId cursor endDate endpointId eventType limit reason startDate"),
                of(CreatePortalTokenParams.class, "allowedIps expiresInDays name scopes"),
                // admin
                of(AnalyticsDashboardParams.class, "destinationId endDate range sourceId startDate tz"),
                of(CreateApiKeyParams.class, "expiresIn name scopes"),
                of(CreateTunnelParams.class, "allowedHosts direction name subdomain"),
                of(CreateCronJobParams.class, "cronExpression description groupId headers method name notifyEmails notifyOnFailure notifyOnSuccess payload timeoutMs timezone url useStaticIp"),
                of(UpdateCronJobParams.class, "cronExpression description groupId headers isActive method name notifyEmails notifyOnFailure notifyOnSuccess payload timeoutMs timezone url useStaticIp"),
                of(CreateCronGroupParams.class, "description name"));
    }

    private static Arguments of(Class<?> type, String keys) {
        return Arguments.of(type.getSimpleName(), type, new TreeSet<>(List.of(keys.split(" "))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("params")
    void everySetterSendsAnApiFieldName(String name, Class<?> type, Set<String> expected) throws Exception {
        Params<?> params = (Params<?>) type.getDeclaredConstructor().newInstance();
        int setters = 0;
        for (Method m : type.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers()) || Modifier.isStatic(m.getModifiers())) continue;
            if (m.getReturnType() != type || m.getParameterCount() != 1) continue;
            Object arg = sample(m.getGenericParameterTypes()[0]);
            try {
                m.invoke(params, arg);
            } catch (InvocationTargetException e) {
                throw new AssertionError(name + "." + m.getName() + " rejected a sample value", e.getCause());
            }
            setters++;
        }
        assertThat(setters).as("%s has setters", name).isPositive();
        assertThat(new TreeSet<>(params.toMap().keySet())).as("%s wire names", name).isEqualTo(expected);
    }

    /** Guards the list above against a params class added later without an entry. */
    @Test
    void theListNamesEveryParamsClass() throws Exception {
        Set<String> listed = new TreeSet<>();
        params().forEach(a -> listed.add((String) a.get()[0]));
        Set<String> all = new TreeSet<>();
        // Gradle runs tests from the project directory.
        try (Stream<Path> files = Files.list(Path.of("src/main/java/app/hookbase/params"))) {
            files.map(p -> p.getFileName().toString())
                    .filter(f -> f.endsWith("Params.java") && !f.equals("Params.java"))
                    .forEach(f -> all.add(f.substring(0, f.length() - ".java".length())));
        }
        assertThat(listed).isEqualTo(all);
    }

    private static Object sample(Type type) {
        Class<?> raw = type instanceof ParameterizedType p ? (Class<?>) p.getRawType() : (Class<?>) type;
        if (raw == String.class || raw == Object.class) return "x";
        if (raw == int.class || raw == Integer.class) return 1;
        if (raw == long.class || raw == Long.class) return 1L;
        if (raw == boolean.class || raw == Boolean.class) return true;
        if (raw == double.class || raw == Double.class) return 1.0;
        if (raw == DestinationThrottle.class) return DestinationThrottle.off();
        if (raw == Map.class) return Map.of("k", "v");
        if (List.class.isAssignableFrom(raw) || raw == Iterable.class || raw == java.util.Collection.class) {
            Type element = type instanceof ParameterizedType p ? p.getActualTypeArguments()[0] : Object.class;
            if (element == EndpointHeader.class) return List.of(new EndpointHeader("X-A", "1"));
            if (element == DestinationFieldMapping.class) return List.of(new DestinationFieldMapping("a", "b", "string"));
            if (element == Integer.class) return List.of(1);
            if (element instanceof ParameterizedType) return List.of(Map.of("k", "v"));
            return List.of("a");
        }
        throw new AssertionError("no sample value for " + type);
    }
}
