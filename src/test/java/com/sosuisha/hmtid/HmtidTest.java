package com.sosuisha.hmtid;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Instant;
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
            var gen = HMTID.monotonicFactory();
            assertEquals(22, gen.generate().toString().length());
        }

        @Test
        void shouldReturnCorrectLengthWhenSeparatorIsHyphen() {
            var gen = HMTID.monotonicFactory(new SecureRandom()::nextDouble, '-', false);
            assertEquals(22, gen.generate().toString().length());
        }

        @Test
        void shouldUseHyphenAsSeparatorBetweenTimestampAndRandomComponent() {
            var gen = HMTID.monotonicFactory(new SecureRandom()::nextDouble, '-', false);
            assertEquals("20211015070216-", gen.generate(1634281336026L).toString().substring(0, 15));
        }

        @Test
        void shouldReturnCorrectLengthWhenSeparatedByHyphenWithTimeSeparation() {
            var gen = HMTID.monotonicFactory(new SecureRandom()::nextDouble, '-', true);
            assertEquals(27, gen.generate().toString().length());
        }

        @Test
        void shouldReturnExpectedTimeComponentResult() {
            var gen = HMTID.monotonicFactory();
            assertEquals("20211015070216_", gen.generate(1634281336026L).toString().substring(0, 15));
        }

        @Test
        void shouldReturnExpectedTimeComponentSeparatedByHyphen() {
            var gen = HMTID.monotonicFactory(new SecureRandom()::nextDouble, '-', true);
            assertEquals("2021-10-15-07-02-16-", gen.generate(1634281336026L).toString().substring(0, 20));
        }

        @Test
        void shouldReturnExpectedTimeComponentSeparatedByUnderbar() {
            var gen = HMTID.monotonicFactory(new SecureRandom()::nextDouble, '_', true);
            assertEquals("2021_10_15_07_02_16_", gen.generate(1634281336026L).toString().substring(0, 20));
        }

        @Test
        void shouldGenerateIdsInMonotonicallyIncreasingOrder() {
            var gen = HMTID.monotonicFactory();
            var ids = new ArrayList<HMTID>();
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
            var gen = HMTID.monotonicFactory(() -> 0.96);
            var time = 1634282804081L; // 2021-10-15 07:26:44 UTC
            assertEquals("20211015072644_YYYYYYY", gen.generate(time).toString()); // first
            assertEquals("20211015072644_YYYYYYZ", gen.generate(time).toString()); // second
            assertEquals("20211015072644_YYYYYZ0", gen.generate(time).toString()); // third
            assertEquals("20211015072644_YYYYYZ1", gen.generate(time).toString()); // fourth
        }
    }

    @Nested
    class MonotonicityWithSeedTime {
        @Test
        void callSequenceWithVariousSeedTimes() {
            var gen = HMTID.monotonicFactory(() -> 0.96);
            // first call — new second
            assertEquals("20160730223616_YYYYYYY", gen.generate(1469918176385L).toString());
            // second call with the same time — increment
            assertEquals("20160730223616_YYYYYYZ", gen.generate(1469918176385L).toString());
            // third call with time less than lastTime — still same second
            assertEquals("20160730223616_YYYYYZ0", gen.generate(100000000L).toString());
            // fourth call with even smaller time — still same second
            assertEquals("20160730223616_YYYYYZ1", gen.generate(10000L).toString());
            // fifth call with 1ms greater, still same second
            assertEquals("20160730223616_YYYYYZ2", gen.generate(1469918176386L).toString());
            // sixth call with 1000ms greater — new second, fresh random
            assertEquals("20160730223617_YYYYYYY", gen.generate(1469918177385L).toString());
        }
    }

    @Nested
    class ForceIncrementSeedTime {
        @Test
        void overflowAdvancesTimestampByOneSecond() {
            var gen = HMTID.monotonicFactory(() -> 0.99);
            // 1st call — new second, random fills with Z
            assertEquals("20160730223616_ZZZZZZZ", gen.generate(1469918176385L).toString());
            // 2nd call same time — MAX_RANDOM overflow, advance 1 sec
            assertEquals("20160730223617_0000000", gen.generate(1469918176385L).toString());
            // 3rd call with very old time — overflowedTime clamps it
            assertEquals("20160730223617_0000001", gen.generate(100000000L).toString());
            // 4th call with tiny time — still clamped
            assertEquals("20160730223617_0000002", gen.generate(10000L).toString());
            // 5th call 1ms after original — still in overflowed second
            assertEquals("20160730223617_0000003", gen.generate(1469918176386L).toString());
            // 6th call 1000ms after original — still in overflowed second
            assertEquals("20160730223617_0000004", gen.generate(1469918177385L).toString());
            // 7th call 1001ms after original — still in overflowed second
            assertEquals("20160730223617_0000005", gen.generate(1469918177386L).toString());
            // 8th call 2000ms after original — new second, fresh Z random
            assertEquals("20160730223618_ZZZZZZZ", gen.generate(1469918178385L).toString());
        }
    }

    @Nested
    class HmtidType {
        @Test
        void constructorSetsFields() {
            var instant = Instant.parse("2021-10-15T07:26:44Z");
            var id = new HMTID("20211015072644_YYYYYYY", instant, "YYYYYYY");
            assertEquals("20211015072644_YYYYYYY", id.toString());
            assertEquals(instant, id.toInstant());
            assertEquals("YYYYYYY", id.getRandomPart());
        }

        @Test
        void ofParsesCompactFormatWithUnderscore() {
            var id = HMTID.of("20211015072644_YYYYYYY");
            assertEquals(Instant.parse("2021-10-15T07:26:44Z"), id.toInstant());
            assertEquals("YYYYYYY", id.getRandomPart());
            assertEquals("20211015072644_YYYYYYY", id.toString());
        }

        @Test
        void ofParsesCompactFormatWithHyphen() {
            var id = HMTID.of("20211015072644-YYYYYYY");
            assertEquals(Instant.parse("2021-10-15T07:26:44Z"), id.toInstant());
            assertEquals("YYYYYYY", id.getRandomPart());
            assertEquals("20211015072644-YYYYYYY", id.toString());
        }

        @Test
        void ofParsesSeparatedFormatWithUnderscore() {
            var id = HMTID.of("2021_10_15_07_26_44_YYYYYYY");
            assertEquals(Instant.parse("2021-10-15T07:26:44Z"), id.toInstant());
            assertEquals("YYYYYYY", id.getRandomPart());
            assertEquals("2021_10_15_07_26_44_YYYYYYY", id.toString());
        }

        @Test
        void ofParsesSeparatedFormatWithHyphen() {
            var id = HMTID.of("2021-10-15-07-26-44-YYYYYYY");
            assertEquals(Instant.parse("2021-10-15T07:26:44Z"), id.toInstant());
            assertEquals("YYYYYYY", id.getRandomPart());
            assertEquals("2021-10-15-07-26-44-YYYYYYY", id.toString());
        }

        @Test
        void ofThrowsOnWrongLength() {
            assertThrows(IllegalArgumentException.class, () -> HMTID.of("20211015072644_YYYYYYY_extra"));
            assertThrows(IllegalArgumentException.class, () -> HMTID.of("short"));
        }

        @Test
        void ofThrowsOnInvalidSeparatorInCompactFormat() {
            assertThrows(IllegalArgumentException.class, () -> HMTID.of("20211015072644@YYYYYYY"));
        }

        @Test
        void ofThrowsOnInvalidSeparatorInSeparatedFormat() {
            assertThrows(IllegalArgumentException.class, () -> HMTID.of("2021@10@15@07@26@44@YYYYYYY"));
        }

        @Test
        void ofThrowsOnInconsistentSeparators() {
            assertThrows(IllegalArgumentException.class, () -> HMTID.of("2021_10-15_07_26_44_YYYYYYY"));
        }

        @Test
        void ofThrowsOnInvalidDate() {
            assertThrows(IllegalArgumentException.class, () -> HMTID.of("20211315072644_YYYYYYY")); // month=13
            assertThrows(IllegalArgumentException.class, () -> HMTID.of("2021_13_15_07_26_44_YYYYYYY")); // month=13
        }

        @Test
        void ofThrowsOnInvalidRandomPart() {
            assertThrows(IllegalArgumentException.class, () -> HMTID.of("20211015072644_IOUYYYY")); // I, O, U invalid
        }

        @Test
        void equalityIgnoresSeparatorFormat() {
            var compact   = HMTID.of("20211015072644_YYYYYYY");
            var separated = HMTID.of("2021_10_15_07_26_44_YYYYYYY");
            var hyphen    = HMTID.of("20211015072644-YYYYYYY");
            var hyphenSep = HMTID.of("2021-10-15-07-26-44-YYYYYYY");
            assertEquals(compact, separated);
            assertEquals(compact, hyphen);
            assertEquals(compact, hyphenSep);
        }

        @Test
        void hashCodeConsistentWithEquals() {
            var a = HMTID.of("20211015072644_YYYYYYY");
            var b = HMTID.of("2021-10-15-07-26-44-YYYYYYY");
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        void compareToPreservesChronologicalOrder() {
            var earlier = HMTID.of("20211015072644_YYYYYYY");
            var later   = HMTID.of("20211015072645_YYYYYYY"); // 1 second later
            assertTrue(earlier.compareTo(later) < 0);
            assertTrue(later.compareTo(earlier) > 0);
            assertEquals(0, earlier.compareTo(HMTID.of("2021_10_15_07_26_44_YYYYYYY")));
        }

        @Test
        void compareToWorksAcrossDifferentSeparators() {
            var underscore = HMTID.of("20211015072644_YYYYYYY");
            var hyphen     = HMTID.of("20211015072644-YYYYYYY");
            assertEquals(0, underscore.compareTo(hyphen));
        }

        @Test
        void formatConvertsToAllCombinations() {
            var id = HMTID.of("20211015072644_YYYYYYY");
            assertEquals("20211015072644_YYYYYYY", HMTID.format(id, '_', false));
            assertEquals("20211015072644-YYYYYYY", HMTID.format(id, '-', false));
            assertEquals("2021_10_15_07_26_44_YYYYYYY", HMTID.format(id, '_', true));
            assertEquals("2021-10-15-07-26-44-YYYYYYY", HMTID.format(id, '-', true));
        }

        @Test
        void formatThrowsOnInvalidSeparator() {
            var id = HMTID.of("20211015072644_YYYYYYY");
            assertThrows(IllegalArgumentException.class, () -> HMTID.format(id, '@', false));
        }

        @Test
        void monotonicFactoryDefaultReturnsGenerator() {
            assertNotNull(HMTID.monotonicFactory());
        }

        @Test
        void monotonicFactoryWithInvalidSeparatorThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> HMTID.monotonicFactory(new SecureRandom()::nextDouble, '@', false));
        }

        @Test
        void generateReturnsHmtidInstance() {
            var gen = HMTID.monotonicFactory();
            var id = gen.generate();
            assertInstanceOf(HMTID.class, id);
            assertEquals(22, id.toString().length());
        }

        @Test
        void generateWithSeedTimeReturnsHmtidInstance() {
            var gen = HMTID.monotonicFactory();
            var id = gen.generate(1634280289042L);
            assertInstanceOf(HMTID.class, id);
            assertEquals(Instant.parse("2021-10-15T06:44:49Z"), id.toInstant());
        }
    }
}
