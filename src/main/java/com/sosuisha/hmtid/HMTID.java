package com.sosuisha.hmtid;

import java.io.Serializable;
import java.security.SecureRandom;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.DoubleSupplier;

/**
 * A human-readable monotonic time-based ID (HMTID) value type.
 *
 * <p>Instances are immutable and safe for use by multiple concurrent threads.
 *
 * <p><b>Equality and ordering:</b> {@link #equals}, {@link #hashCode}, and
 * {@link #compareTo} are based on a separator-free canonical form
 * ({@code YYYYMMDDHHMMSSXXXXXXX}, 21 characters). Two instances with the same
 * timestamp and random part but different separators — for example
 * {@code "20211015072644_YYYYYYY"} and {@code "2021-10-15-07-26-44-YYYYYYY"} —
 * are considered equal and compare as {@code 0}.
 * {@link #toString()} returns the original presentation form and is unaffected
 * by this equality.
 *
 * <p>Because the canonical form preserves lexicographic = chronological order,
 * {@code HMTID} instances can be placed directly in a {@link java.util.TreeSet}
 * or sorted with {@link java.util.Collections#sort} to obtain time order.
 */
public final class HMTID implements Comparable<HMTID>, Serializable {

    private static final long serialVersionUID = 1L;

    private final String rawString;
    private final String canonicalString;
    private final Instant instant;
    private final String randomPart;

    /**
     * Package-private constructor used by {@link HmtidGenerator} to build an
     * {@code HMTID} directly from already-computed components, avoiding a
     * re-parse of {@code rawString}.
     *
     * @param rawString  the full HMTID string (presentation form)
     * @param instant    UTC timestamp at second precision
     * @param randomPart 7-character Crockford Base32 random component
     */
    HMTID(String rawString, Instant instant, String randomPart) {
        this.rawString = rawString;
        this.instant = instant;
        this.randomPart = randomPart;
        var zdt = instant.atZone(ZoneOffset.UTC);
        this.canonicalString = String.format("%04d%02d%02d%02d%02d%02d%s",
                zdt.getYear(), zdt.getMonthValue(), zdt.getDayOfMonth(),
                zdt.getHour(), zdt.getMinute(), zdt.getSecond(), randomPart);
    }

    // -------------------------------------------------------------------------
    // Static factory methods (migrated from deleted Hmtid class)
    // -------------------------------------------------------------------------

    /**
     * Creates an HMTID generator with default settings (separator {@code '_'},
     * no time separation).
     *
     * @return a new {@link HmtidGenerator}
     */
    public static HmtidGenerator monotonicFactory() {
        return monotonicFactory(new SecureRandom()::nextDouble, '_', false);
    }

    /**
     * Creates an HMTID generator with a custom PRNG and default separator and
     * time settings.
     *
     * @param prng custom PRNG returning values in {@code [0.0, 1.0)}
     * @return a new {@link HmtidGenerator}
     */
    public static HmtidGenerator monotonicFactory(DoubleSupplier prng) {
        return monotonicFactory(prng, '_', false);
    }

    /**
     * Creates an HMTID generator with full control over all parameters.
     *
     * @param prng         custom PRNG returning values in {@code [0.0, 1.0)}
     * @param separator    {@code '_'} or {@code '-'}; inserted between the
     *                     timestamp and random components
     * @param separateTime if {@code true}, the separator is also inserted between
     *                     each time component
     * @return a new {@link HmtidGenerator}
     * @throws IllegalArgumentException if {@code separator} is not {@code '_'} or {@code '-'}
     */
    public static HmtidGenerator monotonicFactory(DoubleSupplier prng, char separator, boolean separateTime) {
        validateSeparator(separator);
        return new HmtidGenerator(prng, separator, separateTime);
    }

    // -------------------------------------------------------------------------
    // Parsing
    // -------------------------------------------------------------------------

    /**
     * Parses an HMTID string into an {@code HMTID} instance.
     *
     * <p>The format is auto-detected by string length:
     * <ul>
     *   <li>Length 22 ({@code separateTime=false}): {@code YYYYMMDDHHMMSS{sep}XXXXXXX}</li>
     *   <li>Length 27 ({@code separateTime=true}):
     *       {@code YYYY{sep}MM{sep}DD{sep}HH{sep}MM{sep}SS{sep}XXXXXXX}</li>
     * </ul>
     *
     * @param s the HMTID string to parse
     * @return parsed {@code HMTID}
     * @throws IllegalArgumentException if {@code s} has invalid length, an invalid or
     *         inconsistent separator, an out-of-range date/time, or random-part characters
     *         outside the Crockford Base32 alphabet
     */
    public static HMTID of(String s) {
        return switch (s.length()) {
            case 22 -> parseCompact(s);
            case 27 -> parseSeparated(s);
            default -> throw new IllegalArgumentException(
                    "[hmtid] invalid HMTID length " + s.length() + ": " + s);
        };
    }

    private static HMTID parseCompact(String s) {
        var sep = s.charAt(14);
        validateSeparator(sep);

        var timeStr = s.substring(0, 14);
        if (!timeStr.matches("[0-9]{14}")) {
            throw new IllegalArgumentException("[hmtid] non-digit in time component: " + s);
        }
        var year  = Integer.parseInt(timeStr.substring(0, 4));
        var month = Integer.parseInt(timeStr.substring(4, 6));
        var day   = Integer.parseInt(timeStr.substring(6, 8));
        var hour  = Integer.parseInt(timeStr.substring(8, 10));
        var min   = Integer.parseInt(timeStr.substring(10, 12));
        var sec   = Integer.parseInt(timeStr.substring(12, 14));
        Instant instant;
        try {
            instant = LocalDateTime.of(year, month, day, hour, min, sec)
                    .toInstant(ZoneOffset.UTC);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("[hmtid] invalid date/time in: " + s, e);
        }

        var random = s.substring(15);
        validateRandomPart(random, s);
        return new HMTID(s, instant, random);
    }

    private static HMTID parseSeparated(String s) {
        // separator appears at indices 4, 7, 10, 13, 16, 19
        var sep = s.charAt(4);
        validateSeparator(sep);
        int[] sepPositions = {4, 7, 10, 13, 16, 19};
        for (int pos : sepPositions) {
            if (s.charAt(pos) != sep) {
                throw new IllegalArgumentException(
                        "[hmtid] inconsistent separators in: " + s);
            }
        }

        var yearStr  = s.substring(0, 4);
        var monthStr = s.substring(5, 7);
        var dayStr   = s.substring(8, 10);
        var hourStr  = s.substring(11, 13);
        var minStr   = s.substring(14, 16);
        var secStr   = s.substring(17, 19);
        for (var part : new String[]{yearStr, monthStr, dayStr, hourStr, minStr, secStr}) {
            if (!part.matches("[0-9]+")) {
                throw new IllegalArgumentException("[hmtid] non-digit in time component: " + s);
            }
        }

        Instant instant;
        try {
            instant = LocalDateTime.of(
                    Integer.parseInt(yearStr),
                    Integer.parseInt(monthStr),
                    Integer.parseInt(dayStr),
                    Integer.parseInt(hourStr),
                    Integer.parseInt(minStr),
                    Integer.parseInt(secStr)
            ).toInstant(ZoneOffset.UTC);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("[hmtid] invalid date/time in: " + s, e);
        }

        var random = s.substring(20);
        validateRandomPart(random, s);
        return new HMTID(s, instant, random);
    }

    // -------------------------------------------------------------------------
    // Formatting
    // -------------------------------------------------------------------------

    /**
     * Returns the string representation of {@code id} reformatted with the
     * given {@code separator} and {@code separateTime}.
     *
     * <p>This affects only the presentation; the value ({@link #equals},
     * {@link #compareTo}) is unchanged.
     *
     * @param id           source HMTID
     * @param separator    {@code '_'} or {@code '-'}
     * @param separateTime if {@code true}, the separator is also inserted between
     *                     each time component
     * @return reformatted HMTID string
     * @throws IllegalArgumentException if {@code separator} is not {@code '_'} or {@code '-'}
     */
    public static String format(HMTID id, char separator, boolean separateTime) {
        validateSeparator(separator);
        return TimeEncoder.encodeTime(id.instant.toEpochMilli(), separator, separateTime)
                + separator + id.randomPart;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the UTC timestamp encoded in this HMTID at second precision.
     *
     * @return UTC {@link Instant} truncated to whole seconds
     */
    public Instant toInstant() {
        return instant;
    }

    /**
     * Returns the 7-character Crockford Base32 random component of this HMTID.
     *
     * @return random part string
     */
    public String getRandomPart() {
        return randomPart;
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    /**
     * Returns the original string representation of this HMTID, preserving the
     * separator and {@code separateTime} format used when this instance was
     * created or parsed.
     *
     * <p>Note: {@link #equals} is <em>not</em> based on this string — two
     * instances with different separators may be equal. See the class Javadoc.
     *
     * @return HMTID string in its original presentation form
     */
    @Override
    public String toString() {
        return rawString;
    }

    /**
     * Returns {@code true} if {@code o} is an {@code HMTID} with the same
     * canonical form (separator-free {@code YYYYMMDDHHMMSSXXXXXXX}).
     *
     * <p>Two instances with the same timestamp and random part but different
     * separators are considered equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HMTID other)) return false;
        return canonicalString.equals(other.canonicalString);
    }

    /**
     * Returns a hash code based on the separator-free canonical form.
     * Consistent with {@link #equals}.
     */
    @Override
    public int hashCode() {
        return canonicalString.hashCode();
    }

    /**
     * Compares this HMTID with {@code other} in chronological order.
     *
     * <p>Comparison is based on the separator-free canonical form, so instances
     * with different separators compare correctly and mixed-separator collections
     * sort in time order.
     *
     * @param other the HMTID to compare to
     * @return negative if this is earlier, zero if equal, positive if later
     */
    @Override
    public int compareTo(HMTID other) {
        return this.canonicalString.compareTo(other.canonicalString);
    }

    // -------------------------------------------------------------------------
    // Validation helpers
    // -------------------------------------------------------------------------

    private static void validateSeparator(char sep) {
        if (sep != '_' && sep != '-') {
            throw new IllegalArgumentException(
                    "[hmtid] separator must be '_' or '-', got: '" + sep + "'");
        }
    }

    private static void validateRandomPart(String random, String source) {
        for (int i = 0; i < random.length(); i++) {
            if (Encoding.CHARS.indexOf(random.charAt(i)) == -1) {
                throw new IllegalArgumentException(
                        "[hmtid] invalid character in random part of: " + source);
            }
        }
    }
}