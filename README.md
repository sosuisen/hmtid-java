# Human-readable Monotonic Timestamp Identifier (HMTID) for Java

> This is a Java port of [hmtid](https://github.com/sosuisen/hmtid).
> hmtid is a reduced and modified fork of [ulid](https://github.com/ulid/javascript)

- HMTID begins with a 14-digit human-readable timestamp (YYYYMMDDHHMMSS).
  - The timestamps in most IDs, including ulid, are encoded in a shorter form. HMTID does not encode timestamp digits.
- HMTID ends with 7 random characters (Crockford's Base32) with monotonic sort order. It correctly detects and handles the same second.

- e.g.) `20260426035700_QZ998PX`

- HMTID is not suitable for universal use. It is suitable for naming local files with human-readable, monotonically and infrequently generated IDs, avoiding collisions.

## Spec

- 14-digit current UTC timestamp (YYYYMMDDHHMMSS).
  - e.g.) 20211015134707 (shows 2021-10-15 13:47:07 UTC)
- 7 random characters. Crockford's Base32 is used as shown. This alphabet excludes the letters I, L, O, and U to avoid confusion and abuse.

```
0123456789ABCDEFGHJKMNPQRSTVWXYZ
```

- A separator (underbar `_` or hyphen `-`) that separates the timestamp and random characters. Default is an underbar.
- 22 characters in total.

(optional)
- 14-digit timestamp can be separated by underbar `_` or hyphen `-`. e.g.) YYYY_MM_DD_HH_MM_SS, YYYY-MM-DD-HH-MM-SS
- In that case, 27 characters in total.

## Monotonicity

When generating an HMTID within the same second, we can provide some guarantees regarding sort order. Namely, if the same second is detected, the random component is incremented by 1 in the least significant position (with carrying).

If the increment of random characters fails, the timestamp will be forced to advance ahead of time. Random characters start from `0000000`. This reduces the accuracy of the timestamp, but gives priority to monotonicity.

```java
HmtidGenerator generator = HMTID.monotonicFactory();

generator.generate() // 20211013090000_GEMMVRX
generator.generate() // 20211013090000_GEMMVRY  <- Monotonic increment in the same second
...
generator.generate() // 20211013090000_ZZZZZZZ
generator.generate() // 20211013090001_0000000  <- Does not throw an exception!
generator.generate() // 20211013090001_0000001
generator.generate() // 20211013090001_0000002
generator.generate() // 20211013090002_E3ACF82
generator.generate() // 20211013090003_XER13D3
```

## Install with Maven

```xml
<dependency>
    <groupId>com.sosuisha</groupId>
    <artifactId>hmtid</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Import

```java
import com.sosuisha.hmtid.HMTID;
import com.sosuisha.hmtid.HmtidGenerator;
```

## Usage

`generate()` returns an `HMTID` value type (immutable, like `java.util.UUID`). Call `toString()` to get the string form.

```java
HmtidGenerator generator = HMTID.monotonicFactory();

HMTID id = generator.generate();
id.toString()    // "20211013090001_GEMMVRX"
id.toInstant()   // 2021-10-13T09:00:01Z (java.time.Instant, second precision)
id.getRandomPart() // "GEMMVRX"
```

> **Note:** `HmtidGenerator` is thread-safe. `HMTID` instances are immutable and safe for use by multiple threads.

### Seed Time

You can also input a seed time (Unix timestamp in milliseconds) which will consistently give you the same string for the time component. The default seed time is `System.currentTimeMillis()`.

```java
generator.generate(1469918176385L).toString() // "20160730223616_1M0MRXV"
```

### Use separators

Only `'_'` (default) and `'-'` are valid separators.

```java
import java.security.SecureRandom;

HmtidGenerator generator = HMTID.monotonicFactory(new SecureRandom()::nextDouble, '-', false);
generator.generate().toString() // "20211013090001-A5M3MXZ"
```

```java
HmtidGenerator generator = HMTID.monotonicFactory(new SecureRandom()::nextDouble, '_', true);
generator.generate().toString() // "2021_10_13_09_00_01_GAS28DA"
```

```java
HmtidGenerator generator = HMTID.monotonicFactory(new SecureRandom()::nextDouble, '-', true);
generator.generate().toString() // "2021-10-13-09-00-01-GAS28DA"
```

### Parse from string

`HMTID.of()` auto-detects the format from the string length.

```java
HMTID id = HMTID.of("20211013090001_GEMMVRX");   // compact (length 22)
HMTID id = HMTID.of("2021_10_13_09_00_01_GEMMVRX"); // separated (length 27)
```

### Reformat

`HMTID.format()` returns a string with a different separator or `separateTime` setting without changing the value.

```java
HMTID id = HMTID.of("20211013090001_GEMMVRX");
HMTID.format(id, '-', false) // "20211013090001-GEMMVRX"
HMTID.format(id, '_', true)  // "2021_10_13_09_00_01_GEMMVRX"
HMTID.format(id, '-', true)  // "2021-10-13-09-00-01-GEMMVRX"
```

### Sorting and equality

`HMTID` implements `Comparable<HMTID>`. Comparison is based on a separator-free canonical form, so instances with different separators but the same timestamp and random part compare as equal and sort correctly together.

```java
TreeSet<HMTID> set = new TreeSet<>(); // automatically sorted in chronological order
Collections.sort(list);               // sorts a List<HMTID> in generation order

HMTID.of("20211013090001_GEMMVRX").equals(
    HMTID.of("2021-10-13-09-00-01-GEMMVRX")) // true — same value, different presentation
```

## Test Suite

```
mvn test
```
