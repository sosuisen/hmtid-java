package com.sosuisha.hmtid;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.DoubleSupplier;

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
        void throwsWhenIndexIsOutOfBounds() {
            assertThrows(IllegalArgumentException.class,
                    () -> Base32Util.replaceCharAt("ABCDE", 10, 'X'));
        }

        @Test
        void throwsWhenIndexIsNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> Base32Util.replaceCharAt("ABCDE", -1, 'X'));
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

        // Java-specific: TypeScript's loop reaches str[-1] (undefined) for empty
        // strings;
        // Java's --index loop exits immediately when length is 0, so done stays false.
        @Test
        void throwsOnEmptyString() {
            assertThrows(IllegalArgumentException.class,
                    () -> Base32Util.incrementBase32(""));
        }
    }

    @Nested
    class EncodeTime {
        @Test
        void returnsExpectedEncodedResult() {
            assertEquals("20211015064449", TimeEncoder.encodeTime(1634280289042L, '_', false));
        }

        @Test
        void separatesNumbersByHyphen() {
            assertEquals("2021-10-15-06-44-49", TimeEncoder.encodeTime(1634280289042L, '-', true));
        }

        @Test
        void separatesNumbersByUnderbar() {
            assertEquals("2021_10_15_06_44_49", TimeEncoder.encodeTime(1634280289042L, '_', true));
        }

        @Test
        void throwsWhenTimeIsNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> TimeEncoder.encodeTime(-1L, '_', false));
        }

        @Test
        void throwsWhenTimeExceedsMaximum() {
            assertThrows(IllegalArgumentException.class,
                    () -> TimeEncoder.encodeTime(1L << 48, '_', false));
        }

        // Java-specific: verify UTC formatting from the Unix epoch base.
        // TypeScript's number type doesn't have a meaningful "0" boundary test.
        @Test
        void epochProducesExpectedResult() {
            assertEquals("19700101000000", TimeEncoder.encodeTime(0L, '_', false));
        }
    }

    @Nested
    class RandomChar {
        @Test
        void shouldAlwaysReturnValidBase32Char() {
            DoubleSupplier prng = new SecureRandom()::nextDouble;
            for (int i = 0; i < 320_000; i++) {
                var ch = Base32Util.randomChar(prng);
                assertTrue(Encoding.CHARS.indexOf(ch) >= 0,
                        "char '" + ch + "' not in Crockford Base32 encoding");
            }
        }

        @Test
        void throwsWhenPrngReturnsOne() {
            assertThrows(IllegalArgumentException.class,
                    () -> Base32Util.randomChar(() -> 1.0));
        }
    }

    @Nested
    class EncodeRandom {
        @Test
        void shouldReturnCorrectLength() {
            DoubleSupplier prng = new SecureRandom()::nextDouble;
            assertEquals(12, Base32Util.encodeRandom(12, prng).length());
        }

        @Test
        void shouldOnlyContainValidBase32Characters() {
            DoubleSupplier prng = new SecureRandom()::nextDouble;
            var result = Base32Util.encodeRandom(100, prng);
            assertTrue(result.matches("[0-9ABCDEFGHJKMNPQRSTVWXYZ]+"));
        }
    }

    @Nested
    class Generator {
        @Test
        void shouldReturnCorrectLength() {
            var gen = Hmtid.monotonicFactory();
            assertEquals(22, gen.generate().length());
        }

        @Test
        void shouldReturnCorrectLengthWhenSeparatorIsHyphen() {
            var gen = Hmtid.monotonicFactory(new SecureRandom()::nextDouble, '-', false);
            assertEquals(22, gen.generate().length());
        }

        @Test
        void shouldUseHyphenAsSeparatorBetweenTimestampAndRandomComponent() {
            var gen = Hmtid.monotonicFactory(new SecureRandom()::nextDouble, '-', false);
            assertEquals("20211015070216-", gen.generate(1634281336026L).substring(0, 15));
        }

        @Test
        void shouldReturnCorrectLengthWhenSeparatedByHyphenWithTimeSeparation() {
            var gen = Hmtid.monotonicFactory(new SecureRandom()::nextDouble, '-', true);
            assertEquals(27, gen.generate().length());
        }

        @Test
        void shouldReturnExpectedTimeComponentResult() {
            var gen = Hmtid.monotonicFactory();
            assertEquals("20211015070216_", gen.generate(1634281336026L).substring(0, 15));
        }

        @Test
        void shouldReturnExpectedTimeComponentSeparatedByHyphen() {
            var gen = Hmtid.monotonicFactory(new SecureRandom()::nextDouble, '-', true);
            assertEquals("2021-10-15-07-02-16-", gen.generate(1634281336026L).substring(0, 20));
        }

        @Test
        void shouldReturnExpectedTimeComponentSeparatedByUnderbar() {
            var gen = Hmtid.monotonicFactory(new SecureRandom()::nextDouble, '_', true);
            assertEquals("2021_10_15_07_02_16_", gen.generate(1634281336026L).substring(0, 20));
        }

        @Test
        void shouldGenerateIdsInMonotonicallyIncreasingOrder() {
            var gen = Hmtid.monotonicFactory();
            var ids = new ArrayList<String>();
            for (int i = 0; i < 100; i++) {
                ids.add(gen.generate());
            }
            var sorted = new ArrayList<>(ids);
            Collections.sort(sorted);
            assertEquals(ids, sorted);
        }
    }

    @Nested
    class MonotonicityWithoutSeedTime {
        @Test
        void callSequenceWithFrozenTime() {
            var gen = Hmtid.monotonicFactory(() -> 0.96);
            var time = 1634282804081L; // 2021-10-15 07:26:44 UTC
            assertEquals("20211015072644_YYYYYYY", gen.generate(time)); // first
            assertEquals("20211015072644_YYYYYYZ", gen.generate(time)); // second
            assertEquals("20211015072644_YYYYYZ0", gen.generate(time)); // third
            assertEquals("20211015072644_YYYYYZ1", gen.generate(time)); // fourth
        }
    }

    @Nested
    class MonotonicityWithSeedTime {
        @Test
        void callSequenceWithVariousSeedTimes() {
            var gen = Hmtid.monotonicFactory(() -> 0.96);
            // first call — new second
            assertEquals("20160730223616_YYYYYYY", gen.generate(1469918176385L));
            // second call with the same time — increment
            assertEquals("20160730223616_YYYYYYZ", gen.generate(1469918176385L));
            // third call with time less than lastTime — still same second
            assertEquals("20160730223616_YYYYYZ0", gen.generate(100000000L));
            // fourth call with even smaller time — still same second
            assertEquals("20160730223616_YYYYYZ1", gen.generate(10000L));
            // fifth call with 1ms greater, still same second
            assertEquals("20160730223616_YYYYYZ2", gen.generate(1469918176386L));
            // sixth call with 1000ms greater — new second, fresh random
            assertEquals("20160730223617_YYYYYYY", gen.generate(1469918177385L));
        }
    }

    @Nested
    class ForceIncrementSeedTime {
        @Test
        void overflowAdvancesTimestampByOneSecond() {
            var gen = Hmtid.monotonicFactory(() -> 0.99);
            // 1st call — new second, random fills with Z
            assertEquals("20160730223616_ZZZZZZZ", gen.generate(1469918176385L));
            // 2nd call same time — MAX_RANDOM overflow, advance 1 sec
            assertEquals("20160730223617_0000000", gen.generate(1469918176385L));
            // 3rd call with very old time — overflowedTime clamps it
            assertEquals("20160730223617_0000001", gen.generate(100000000L));
            // 4th call with tiny time — still clamped
            assertEquals("20160730223617_0000002", gen.generate(10000L));
            // 5th call 1ms after original — still in overflowed second
            assertEquals("20160730223617_0000003", gen.generate(1469918176386L));
            // 6th call 1000ms after original — still in overflowed second
            assertEquals("20160730223617_0000004", gen.generate(1469918177385L));
            // 7th call 1001ms after original — still in overflowed second
            assertEquals("20160730223617_0000005", gen.generate(1469918177386L));
            // 8th call 2000ms after original — new second, fresh Z random
            assertEquals("20160730223618_ZZZZZZZ", gen.generate(1469918178385L));
        }
    }
}
