package io.github.sosuisen.hmtid;

import java.time.Instant;
import java.time.ZoneOffset;

final class TimeEncoder {
    private TimeEncoder() {}

    /**
     * Encodes a Unix timestamp in milliseconds as a UTC date-time string (YYYYMMDDHHMMSS).
     *
     * @param now          Unix timestamp in milliseconds; must be in {@code [0, (1L << 48) - 1]}
     * @param separator    separator character inserted between each time component when
     *                     {@code separateTime} is {@code true}
     * @param separateTime if {@code true}, inserts {@code separator} between each time component
     *                     (e.g. {@code YYYY_MM_DD_HH_MM_SS})
     * @return 14-character date-time string, or 19-character string when {@code separateTime}
     *         is {@code true}
     * @throws IllegalArgumentException if {@code now} is negative or exceeds
     *         {@code (1L << 48) - 1} milliseconds
     */
    static String encodeTime(long now, char separator, boolean separateTime) {
        if (now > Encoding.TIME_MAX) {
            throw new IllegalArgumentException("[hmtid] cannot encode time greater than " + Encoding.TIME_MAX);
        }
        if (now < 0) {
            throw new IllegalArgumentException("[hmtid] time must be positive");
        }
        var dt = Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC);
        if (separateTime) {
            var sep = String.valueOf(separator);
            return String.format("%04d%s%02d%s%02d%s%02d%s%02d%s%02d",
                dt.getYear(), sep, dt.getMonthValue(), sep, dt.getDayOfMonth(), sep,
                dt.getHour(), sep, dt.getMinute(), sep, dt.getSecond());
        }
        return String.format("%04d%02d%02d%02d%02d%02d",
            dt.getYear(), dt.getMonthValue(), dt.getDayOfMonth(),
            dt.getHour(), dt.getMinute(), dt.getSecond());
    }
}
