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

    @Nested
    class IncrementBase32 {
        @Test
        void incrementsCorrectly() {
            assertEquals("A109D", Base32Util.incrementBase32("A109C"));
        }

        @Test
        void carriesCorrectly() {
            assertEquals("A1Z00", Base32Util.incrementBase32("A1YZZ"));
        }

        @Test
        void doubleIncrementsCorrectly() {
            assertEquals("A1Z01", Base32Util.incrementBase32(Base32Util.incrementBase32("A1YZZ")));
        }

        @Test
        void throwsWhenItCannotIncrement() {
            assertThrows(IllegalArgumentException.class,
                () -> Base32Util.incrementBase32("ZZZ"));
        }

        @Test
        void throwsOnInvalidCharacter() {
            assertThrows(IllegalArgumentException.class,
                () -> Base32Util.incrementBase32("A1_Z"));
        }

        // Java-specific: TypeScript's loop reaches str[-1] (undefined) for empty strings;
        // Java's --index loop exits immediately when length is 0, so done stays false.
        @Test
        void throwsOnEmptyString() {
            assertThrows(IllegalArgumentException.class,
                () -> Base32Util.incrementBase32(""));
        }
    }
}
