package io.github.sosuisen.hmtid;

import java.util.function.DoubleSupplier;

/**
 * Generates monotonically increasing HMTID strings.
 *
 * <p>Not thread-safe; use one instance per thread or apply external synchronization.
 */
public final class HmtidGenerator {
    private final DoubleSupplier prng;
    private final char separator;
    private final boolean separateTime;
    private long lastTime = 0;
    private String lastRandom = null;
    private long overflowedTime = 0;

    HmtidGenerator(DoubleSupplier prng, char separator, boolean separateTime) {
        this.prng = prng;
        this.separator = separator;
        this.separateTime = separateTime;
    }

    /**
     * Generates a new HMTID using the current system time.
     *
     * @return a monotonically increasing HMTID string
     */
    public String generate() {
        return generate(System.currentTimeMillis());
    }

    /**
     * Generates a new HMTID using the given seed time.
     *
     * @param seedTime Unix timestamp in milliseconds
     * @return a monotonically increasing HMTID string
     * @throws IllegalArgumentException if {@code seedTime} is negative or exceeds the maximum encodable time
     */
    public String generate(long seedTime) {
        if (seedTime < overflowedTime) {
            seedTime = overflowedTime;
        }
        if (lastRandom != null && Math.floorDiv(seedTime, 1000) <= Math.floorDiv(lastTime, 1000)) {
            if (Encoding.MAX_RANDOM.equals(lastRandom)) {
                lastTime = (seedTime / 1000 + 1) * 1000;
                overflowedTime = lastTime;
                lastRandom = Encoding.MIN_RANDOM;
                return TimeEncoder.encodeTime(lastTime, separator, separateTime) + separator + lastRandom;
            }
            lastRandom = Base32Util.incrementBase32(lastRandom);
            return TimeEncoder.encodeTime(lastTime, separator, separateTime) + separator + lastRandom;
        }
        lastTime = seedTime;
        lastRandom = Base32Util.encodeRandom(Encoding.RANDOM_LEN, prng);
        return TimeEncoder.encodeTime(seedTime, separator, separateTime) + separator + lastRandom;
    }
}
