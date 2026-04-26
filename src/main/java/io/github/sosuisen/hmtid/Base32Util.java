package io.github.sosuisen.hmtid;

final class Base32Util {
    private Base32Util() {}

    static String replaceCharAt(String str, int index, char ch) {
        if (index > str.length() - 1) {
            return str;
        }
        return str.substring(0, index) + ch + str.substring(index + 1);
    }
}
