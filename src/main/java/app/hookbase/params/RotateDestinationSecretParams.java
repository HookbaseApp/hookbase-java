package app.hookbase.params;

/** Body for {@code destinations().rotateSecret()}. Every field is optional. */
public final class RotateDestinationSecretParams extends Params<RotateDestinationSecretParams> {
    /**
     * How long, 0-86400 seconds, deliveries keep being signed with the previous secret as well
     * (the API defaults to 3600). 0 retires the old secret at once.
     */
    public RotateDestinationSecretParams gracePeriodSeconds(int gracePeriodSeconds) {
        return set("gracePeriodSeconds", gracePeriodSeconds);
    }
}
