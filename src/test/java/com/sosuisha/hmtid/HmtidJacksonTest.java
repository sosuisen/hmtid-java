package com.sosuisha.hmtid;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HmtidJacksonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Nested
    class Serialization {
        @Test
        void serializesCompactFormAsPlainString() throws Exception {
            var id = HMTID.of("20211015072644_YYYYYYY");
            assertEquals("\"20211015072644_YYYYYYY\"", mapper.writeValueAsString(id));
        }

        @Test
        void serializesSeparatedFormAsPlainString() throws Exception {
            var id = HMTID.of("2021-10-15-07-26-44-YYYYYYY");
            assertEquals("\"2021-10-15-07-26-44-YYYYYYY\"", mapper.writeValueAsString(id));
        }

        @Test
        void serializesGeneratedIdAsPlainString() throws Exception {
            var id = HMTID.monotonicFactory().generate();
            assertEquals("\"" + id + "\"", mapper.writeValueAsString(id));
        }

        record Holder(HMTID id, String name) {
        }

        @Test
        void serializesAsStringInsideContainerObject() throws Exception {
            var holder = new Holder(HMTID.of("2021-10-15-07-26-44-YYYYYYY"), "page");
            assertEquals("{\"id\":\"2021-10-15-07-26-44-YYYYYYY\",\"name\":\"page\"}",
                    mapper.writeValueAsString(holder));
        }
    }

    @Nested
    class Deserialization {
        @Test
        void deserializesFromPlainString() throws Exception {
            var id = mapper.readValue("\"2021-10-15-07-26-44-YYYYYYY\"", HMTID.class);
            assertEquals(HMTID.of("2021-10-15-07-26-44-YYYYYYY"), id);
        }

        @Test
        void roundTripPreservesValueAndPresentation() throws Exception {
            var original = HMTID.monotonicFactory().generate();
            var json = mapper.writeValueAsString(original);
            var restored = mapper.readValue(json, HMTID.class);
            assertEquals(original, restored);
            assertEquals(original.toString(), restored.toString());
        }

        @Test
        void throwsOnInvalidString() {
            assertThrows(Exception.class,
                    () -> mapper.readValue("\"YYYYYYY\"", HMTID.class));
        }
    }
}
