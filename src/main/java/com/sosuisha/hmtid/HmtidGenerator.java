package com.sosuisha.hmtid;

import java.time.Instant;
import java.util.function.DoubleSupplier;

/**
 * Generates monotonically increasing HMTIDs.
 *
 * <p>Thread-safe: {@link #generate(long)} is {@code synchronized}. The returned
 * {@link HMTID} instances are immutable and safe for use by multiple threads.
 */
public final class HmtidGenerator {
    private final DoubleSupplier prng;
    private final char separator;
    private final boolean separateTime;
    private long lastTime = 0;
    private String lastRandom = null;
    // The artificially-advanced timestamp set when the random component overflows
    // (ZZZZZZZ -> +1 s).
    // seedTime is clamped to this value until the real clock catches up, preserving
    // monotonicity.
    private long overflowedTime = 0;

    HmtidGenerator(DoubleSupplier prng, char separator, boolean separateTime) {
        this.prng = prng;
        this.separator = separator;
        this.separateTime = separateTime;
    }

    /**
     * Generates a new HMTID using the current system time.
     *
     * @return a monotonically increasing {@link HMTID}
     */
    public HMTID generate() {
        return generate(System.currentTimeMillis());
    }

    /**
     * Generates a new HMTID using the given seed time.
     *
     * @param seedTime Unix timestamp in milliseconds
     * @return a monotonically increasing {@link HMTID}
     * @throws IllegalArgumentException if {@code seedTime} exceeds the maximum
     *                                  encodable time
     *                                  ({@code (1L << 48) - 1} milliseconds);
     *                                  negative values are silently clamped to
     *                                  {@code overflowedTime} (which is always &ge; 0)
     */
    public synchronized HMTID generate(long seedTime) {
        if (seedTime < overflowedTime) {
            seedTime = overflowedTime;
        }
        if (lastRandom != null && Math.floorDiv(seedTime, 1000) <= Math.floorDiv(lastTime, 1000)) {
            if (Encoding.MAX_RANDOM.equals(lastRandom)) {
                // Integer division truncates to a whole-second boundary, matching the
                // TypeScript Math.floor semantics for the second granularity used by the
                // monotonicity comparison.
                lastTime = (seedTime / 1000 + 1) * 1000;
                overflowedTime = lastTime;
                lastRandom = Encoding.MIN_RANDOM;
                // HMTID is constructed inside the synchronized block; all fields are
                // final, so the JMM guarantees safe publication to any receiving thread.
                return new HMTID(
                        TimeEncoder.encodeTime(lastTime, separator, separateTime) + separator + lastRandom,
                        Instant.ofEpochSecond(lastTime / 1000),
                        lastRandom);
            }
            lastRandom = Base32Util.incrementBase32(lastRandom);
            return new HMTID(
                    TimeEncoder.encodeTime(lastTime, separator, separateTime) + separator + lastRandom,
                    Instant.ofEpochSecond(lastTime / 1000),
                    lastRandom);
        }
        lastTime = seedTime;
        lastRandom = Base32Util.encodeRandom(Encoding.RANDOM_LEN, prng);
        return new HMTID(
                TimeEncoder.encodeTime(seedTime, separator, separateTime) + separator + lastRandom,
                Instant.ofEpochSecond(lastTime / 1000),
                lastRandom);
    }
}