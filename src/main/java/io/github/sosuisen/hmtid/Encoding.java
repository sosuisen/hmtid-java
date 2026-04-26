package io.github.sosuisen.hmtid;

final class Encoding {
    static final String CHARS = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    static final int LEN = CHARS.length();
    static final long TIME_MAX = (1L << 48) - 1;
    static final int RANDOM_LEN = 7;
    static final String MAX_RANDOM = "ZZZZZZZ";
    static final String MIN_RANDOM = "0000000";

    private Encoding() {}
}
