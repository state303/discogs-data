package io.dsub.discogsdata.batch.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class MalformedDateParserTest {

    private List<String> malformedList;

    @BeforeEach
    void setUp() {
        malformedList = Arrays.asList(
                "199X-04",
                "1928-XX",
                "1920-0X",
                "1999-2-31",
                "19XX",
                "192X-");
    }

    @Test
    void parse() {
        assertDoesNotThrow(() -> malformedList.forEach(MalformedDateParser::parse));
        LocalDate dateTime = MalformedDateParser.parse("199X-04");
        assertEquals(1990, dateTime.getYear());
        assertEquals(4, dateTime.getMonthValue());
        assertEquals(1, dateTime.getDayOfMonth());

        dateTime = MalformedDateParser.parse("1928-XX");
        assertEquals(1928, dateTime.getYear());
        assertEquals(1, dateTime.getMonthValue());
        assertEquals(1, dateTime.getDayOfMonth());

        dateTime = MalformedDateParser.parse("1999-2-31");
        assertEquals(1999, dateTime.getYear());
        assertEquals(2, dateTime.getMonthValue());
        assertEquals(1, dateTime.getDayOfMonth());

        dateTime = MalformedDateParser.parse("19XX");
        assertEquals(1900, dateTime.getYear());

        dateTime = MalformedDateParser.parse("192X");
        assertEquals(1920, dateTime.getYear());

        dateTime = MalformedDateParser.parse("192X-");
        assertEquals(1920, dateTime.getYear());

        dateTime = MalformedDateParser.parse("192X-0X-");
        assertEquals(1920, dateTime.getYear());
        assertEquals(1, dateTime.getMonthValue());

        dateTime = MalformedDateParser.parse("192X-0X-X");
        assertEquals(1920, dateTime.getYear());
        assertEquals(1, dateTime.getMonthValue());
        assertEquals(1, dateTime.getDayOfMonth());

        dateTime = MalformedDateParser.parse("192X-0X-X1");
        assertEquals(1920, dateTime.getYear());
        assertEquals(1, dateTime.getMonthValue());
        assertEquals(1, dateTime.getDayOfMonth());
    }
}