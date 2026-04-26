package io.github.sosuisen.hmtid;

import java.util.function.DoubleSupplier;

final class Base32Util {
    private Base32Util() {}

    /**
     * Replaces the character at {@code index} in {@code str} with {@code ch}.
     *
     * @param str   source string
     * @param index zero-based index of the character to replace
     * @param ch    replacement character
     * @return new string with the character replaced
     * @throws IllegalArgumentException if {@code index} is out of bounds
     */
    static String replaceCharAt(String str, int index, char ch) {
        if (index < 0 || index > str.length() - 1) {
            throw new IllegalArgumentException("[hmtid] index out of bounds: " + index);
        }
        return str.substring(0, index) + ch + str.substring(index + 1);
    }

    /**
     * Increments a Crockford Base32 string by 1 at the least significant position,
     * propagating carry leftward as needed.
     *
     * @param str Crockford Base32 string to increment
     * @return incremented string
     * @throws IllegalArgumentException if {@code str} contains a character outside the
     *         Crockford Base32 alphabet, or if all characters are already {@code 'Z'}
     *         and the increment overflows
     */
    static String incrementBase32(String str) {
        var result = str;
        var index = str.length();
        var done = false;
        while (!done && --index >= 0) {
            var ch = result.charAt(index);
            var charIndex = Encoding.CHARS.indexOf(ch);
            if (charIndex == -1) {
                throw new IllegalArgumentException("[hmtid] incorrectly encoded string");
            }
            if (charIndex == Encoding.LEN - 1) {
                result = replaceCharAt(result, index, Encoding.CHARS.charAt(0));
                continue;
            }
            result = replaceCharAt(result, index, Encoding.CHARS.charAt(charIndex + 1));
            done = true;
        }
        if (done) {
            return result;
        }
        throw new IllegalArgumentException("[hmtid] cannot increment this string");
    }

    /**
     * Returns a single random Crockford Base32 character.
     *
     * @param prng PRNG returning values in {@code [0.0, 1.0)}
     * @return a random character from the Crockford Base32 alphabet
     * @throws IllegalArgumentException if {@code prng} returns {@code 1.0}, violating its contract
     */
    static char randomChar(DoubleSupplier prng) {
        var rand = (int) Math.floor(prng.getAsDouble() * Encoding.LEN);
        // prng must return [0.0, 1.0); 1.0 would produce rand == LEN and overflow CHARS
        if (rand == Encoding.LEN) {
            throw new IllegalArgumentException("[hmtid] prng must return a value in [0.0, 1.0)");
        }
        return Encoding.CHARS.charAt(rand);
    }

    /**
     * Generates a random Crockford Base32 string of the given length.
     *
     * @param len  number of characters to generate
     * @param prng PRNG returning values in {@code [0.0, 1.0)}
     * @return random Crockford Base32 string of length {@code len}
     */
    static String encodeRandom(int len, DoubleSupplier prng) {
        var sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(randomChar(prng));
        }
        return sb.reverse().toString();
    }
}
