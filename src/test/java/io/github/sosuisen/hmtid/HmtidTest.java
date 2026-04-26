package io.github.sosuisen.hmtid;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class HmtidTest {

    @Nested
    class ReplaceCharAt {
        @Test
        void replacesCharacterAtGivenIndex() {
            assertEquals("ABXDE", Base32Util.replaceCharAt("ABCDE", 2, 'X'));
        }

        @Test
        void replacesFirstCharacter() {
            assertEquals("XBCDE", Base32Util.replaceCharAt("ABCDE", 0, 'X'));
        }

        @Test
        void replacesLastCharacter() {
            assertEquals("ABCDX", Base32Util.replaceCharAt("ABCDE", 4, 'X'));
        }

        @Test
        void returnsOriginalStringWhenIndexIsOutOfBounds() {
            assertEquals("ABCDE", Base32Util.replaceCharAt("ABCDE", 10, 'X'));
        }
    }
}
