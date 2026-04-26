# hmtid-java Design Spec

**Date:** 2026-04-26  
**Source project:** https://github.com/sosuisen/hmtid (TypeScript)  
**Target project:** https://github.com/sosuisen/hmtid-java (Java 25 Maven)

---

## Overview

Port of the `hmtid` TypeScript library to Java. **hmtid** is a Human-readable Monotonic Timestamp ID generator that produces unique, lexicographically sortable IDs combining a UTC timestamp and a Crockford Base32 random component.

**ID format (default):**
```
20211015064449_YYYYYYY
└─── 14 chars ──┘ └─7 chars─┘
  UTC timestamp    Base32 random
  (YYYYMMDDHHMMSS)
```

Total length: 22 chars (default) or 27 chars with `separateTime=true`.

---

## Project Structure

```
F:\TypeScriptProject\hmtid-java\
├── pom.xml
└── src/
    ├── main/java/io/github/sosuisen/hmtid/
    │   ├── Hmtid.java              # public factory class
    │   ├── HmtidGenerator.java     # public generator class
    │   ├── Encoding.java           # package-private constants
    │   ├── Base32Util.java         # package-private Base32 utilities
    │   └── TimeEncoder.java        # package-private time encoder
    └── test/java/io/github/sosuisen/hmtid/
        └── HmtidTest.java          # JUnit 5 tests
```

**Maven dependencies:** JUnit 5 only. No external runtime dependencies.  
**Java version:** Java 25  
**Package:** `io.github.sosuisen.hmtid`

---

## Public API

### `Hmtid` (static factory class)

```java
public final class Hmtid {
    public static HmtidGenerator monotonicFactory();
    public static HmtidGenerator monotonicFactory(DoubleSupplier prng);
    public static HmtidGenerator monotonicFactory(DoubleSupplier prng, char separator, boolean separateTime);
}
```

- `detectPrng()` is **not** exposed publicly — Java's `SecureRandom` is always available with no environment detection needed, so it is used internally.
- `monotonicFactory()` creates a `SecureRandom`-backed PRNG internally, with `separator='_'` and `separateTime=false`.

### `HmtidGenerator` (public concrete class)

```java
public final class HmtidGenerator {
    public String generate();              // uses System.currentTimeMillis()
    public String generate(long seedTime); // seedTime in milliseconds
}
```

### Error handling

`IllegalArgumentException` is thrown for invalid inputs. Messages are prefixed with `[hmtid]`, e.g.: `"[hmtid] time must be positive"`.

---

## Internal Implementation

### `Encoding` (package-private)

Constants:
- `CHARS = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"` — Crockford's Base32 (32 chars)
- `LEN = 32`
- `TIME_MAX = (1L << 48) - 1`
- `RANDOM_LEN = 7`
- `MAX_RANDOM = "ZZZZZZZ"` — all chars at maximum
- `MIN_RANDOM = "0000000"` — all chars at minimum

### `Base32Util` (package-private)

| Method | Description |
|--------|-------------|
| `replaceCharAt(String str, int index, char ch)` | Replace char at index; returns original if out of bounds |
| `incrementBase32(String str)` | Increment Crockford Base32 string; throws if overflow or invalid char |
| `randomChar(DoubleSupplier prng)` | Returns single random Base32 char |
| `encodeRandom(int len, DoubleSupplier prng)` | Returns random Base32 string of given length |

### `TimeEncoder` (package-private)

```java
static String encodeTime(long now, char separator, boolean separateTime)
```

Uses `Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC)` for formatting.
Validates: not NaN (always long, so not applicable), not negative, not > TIME_MAX, must be integer (always true for long).

Format without separator: `DateTimeFormatter.ofPattern("yyyyMMddHHmmss")`  
Format with separator: dynamically built, e.g. `yyyy-MM-dd-HH-mm-ss` for `'-'`.

### `HmtidGenerator` state (monotonicity)

```java
private long lastTime = 0;
private String lastRandom = null;
private long overflowedTime = 0;
private final DoubleSupplier prng;
private final char separator;
private final boolean separateTime;
```

### Monotonicity algorithm

Mirrors the TypeScript implementation exactly:

1. If `seedTime` is not provided, use `System.currentTimeMillis()`.
2. If `seedTime < overflowedTime`, set `seedTime = overflowedTime`.
3. If `Math.floorDiv(seedTime, 1000) <= Math.floorDiv(lastTime, 1000)` (same second or earlier):
   - If `lastRandom.equals(MAX_RANDOM)`: advance `lastTime` by 1 second, set `overflowedTime = lastTime`, set `lastRandom = MIN_RANDOM`, return new ID with `lastTime`.
   - Otherwise: increment `lastRandom` via `Base32Util.incrementBase32`, return ID with `lastTime`.
4. Otherwise (new second): set `lastTime = seedTime`, generate fresh `lastRandom`, return new ID.

**ID format:** `encodeTime(lastTime) + separator + randomPart`

---

## Testing

JUnit 5 tests in `HmtidTest.java`, 1-to-1 mapping with TypeScript `test/test.cjs`.

| TypeScript construct | Java equivalent |
|----------------------|-----------------|
| `stubbedPrng = () => 0.96` | `() -> 0.96` lambda |
| `FakeTimers` (frozen clock) | `generate(long seedTime)` with fixed values |
| `assert.strictEqual` | `assertEquals` |
| `assert.throws` | `assertThrows` |

Test groups (30 tests total):
- `replaceCharAt` — boundary cases (middle, first, last, out-of-bounds)
- `incrementBase32` — increment, carry, double-increment, overflow throws, invalid char throws, **empty string throws** *(Java-specific: TypeScript's `str[-1]` path differs from Java's loop-exit path)*
- `randomChar` — distribution over 320,000 samples (all chars valid Base32) *(replaces TS undefined/empty-string checks)*
- `encodeTime` — expected output, separator variants, error cases, **epoch=19700101000000** *(Java-specific: verifies UTC base from epoch)*
- `encodeRandom` — correct length, valid Base32 chars
- `hmtid` (generator) — length=22/27, separator in output, monotonically increasing order
- `monotonicity without seedTime` — first/second/third/fourth call with same second
- `monotonicity with seedTime` — same/less/greater time handling
- `force increment seedTime` — overflow to next second behavior

**Excluded TypeScript-only tests:**
- `detectPrng` (browser/Node.js environment detection not needed in Java)
- `encodeTime` → NaN and non-integer checks (`long` is always a valid integer)
- `randomChar` → `undefined`/empty-string checks (`char` primitive is never null or empty)

---

## Key Design Decisions

1. **No HMTID interface** — `HmtidGenerator` is a concrete class; a separate interface adds no value since there is only one implementation.
2. **`detectPrng()` not exposed publicly** — TypeScript needed environment detection (browser vs Node.js); Java always has `SecureRandom`, so detection is unnecessary. `monotonicFactory()` creates the PRNG internally.
3. **Custom PRNG via `DoubleSupplier`** — `monotonicFactory(DoubleSupplier)` enables deterministic testing (e.g., `() -> 0.96`) without mocking frameworks.
4. **Internal utilities are package-private** — only `Hmtid` and `HmtidGenerator` are public; internal math is an implementation detail.
5. **`Math.floorDiv` for second comparison** — correctly handles negative timestamps (unlike `/ 1000` in Java which truncates toward zero), matching TypeScript's `Math.floor`.
