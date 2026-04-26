package io.github.sosuisen.hmtid;

import java.security.SecureRandom;
import java.util.function.DoubleSupplier;

/**
 * Factory for creating HMTID generators.
 */
public final class Hmtid {
    private Hmtid() {}

    /**
     * Creates an HMTID generator with default settings (separator {@code '_'}, no time separation).
     *
     * @return a new {@link HmtidGenerator}
     */
    public static HmtidGenerator monotonicFactory() {
        return monotonicFactory(new SecureRandom()::nextDouble, '_', false);
    }

    /**
     * Creates an HMTID generator with a custom PRNG and default separator and time settings.
     *
     * @param prng custom pseudo-random number generator returning values in {@code [0.0, 1.0)}
     * @return a new {@link HmtidGenerator}
     */
    public static HmtidGenerator monotonicFactory(DoubleSupplier prng) {
        return monotonicFactory(prng, '_', false);
    }

    /**
     * Creates an HMTID generator with full control over all parameters.
     *
     * @param prng         custom PRNG returning values in {@code [0.0, 1.0)}; pass a lambda such as
     *                     {@code new SecureRandom()::nextDouble} or a fixed value for testing
     * @param separator    character inserted between the timestamp and random components
     * @param separateTime if {@code true}, the separator is also inserted between each time component
     * @return a new {@link HmtidGenerator}
     */
    public static HmtidGenerator monotonicFactory(DoubleSupplier prng, char separator, boolean separateTime) {
        return new HmtidGenerator(prng, separator, separateTime);
    }
}
