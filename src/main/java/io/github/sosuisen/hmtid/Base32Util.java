package io.github.sosuisen.hmtid;

final class Base32Util {
    private Base32Util() {}

    static String replaceCharAt(String str, int index, char ch) {
        if (index > str.length() - 1) {
            return str;
        }
        return str.substring(0, index) + ch + str.substring(index + 1);
    }

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
}
