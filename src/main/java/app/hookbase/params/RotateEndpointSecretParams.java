package app.hookbase.params;

/** Body for {@code endpoints().rotateSecret()}. Every field is optional. */
public final class RotateEndpointSecretParams extends Params<RotateEndpointSecretParams> {
    /**
     * How long, 0-86400 seconds, deliveries keep being signed with the previous secret as well
     * (the API defaults to 3600). 0 retires the old secret at once.
     */
    public RotateEndpointSecretParams gracePeriodSeconds(int gracePeriodSeconds) {
        return set("gracePeriodSeconds", gracePeriodSeconds);
    }
}
