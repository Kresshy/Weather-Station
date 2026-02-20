package com.kresshy.weatherstation.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link WeatherMessageParser}. Verifies parsing logic for JSON and legacy PDU
 * formats.
 */
public class WeatherMessageParserTest {

    private WeatherMessageParser parser;

    @Before
    public void setUp() {
        parser = new WeatherMessageParser(new Gson());
    }

    /** Verifies parsing of the modern JSON-based protocol. */
    @Test
    public void parse_ModernJsonFormat() {
        String json =
                """
{"version":2,"numberOfNodes":1,"measurements":[{"windSpeed":5.5,"temperature":22.2,"nodeId":0}]}
""";
        String rawData = "WS_" + json.trim() + "_end";

        WeatherData result = parser.parse(rawData);

        assertEquals(5.5, result.getWindSpeed(), 0.001);
        assertEquals(22.2, result.getTemperature(), 0.001);
    }

    /** Verifies parsing of the legacy space-separated string protocol. */
    @Test
    public void parse_LegacyFormat() {
        String legacy = "5.5 22.2";
        String rawData = "WS_" + legacy + "_end";

        WeatherData result = parser.parse(rawData);

        assertEquals(5.5, result.getWindSpeed(), 0.001);
        assertEquals(22.2, result.getTemperature(), 0.001);
    }

    @Test
    public void parse_LegacyFormatWithStartPrefix() {
        String rawData = "start_5.5 22.2_end";
        WeatherData result = parser.parse(rawData);
        assertEquals(5.5, result.getWindSpeed(), 0.001);
        assertEquals(22.2, result.getTemperature(), 0.001);
    }

    @Test
    public void parse_LegacyFormatWithExtraWhitespace() {
        String rawData = "  start_  5.5   22.2  _end  \n";
        WeatherData result = parser.parse(rawData);
        assertEquals(5.5, result.getWindSpeed(), 0.001);
        assertEquals(22.2, result.getTemperature(), 0.001);
    }

    @Test
    public void parse_LegacyFormatWithCommas() {
        String rawData = "start_5.5,22.2_end";
        WeatherData result = parser.parse(rawData);
        assertEquals(5.5, result.getWindSpeed(), 0.001);
        assertEquals(22.2, result.getTemperature(), 0.001);
    }

    @Test
    public void parse_LegacyFormatWithNodeId() {
        String rawData = "start_5.5 22.2 1_end";
        WeatherData result = parser.parse(rawData);
        assertEquals(5.5, result.getWindSpeed(), 0.001);
        assertEquals(22.2, result.getTemperature(), 0.001);
        assertEquals(1, result.getNodeId());
    }

    /** Verifies that malformed prefixes or null inputs return null. */
    @Test
    public void parse_MalformedInput_ReturnsNull() {
        assertNull(parser.parse(null));
        assertNull(parser.parse(""));
        assertNull(parser.parse("   "));
    }

    @Test
    public void parse_EmptyJson_ReturnsNull() {
        String json = "{}";
        String rawData = "WS_" + json + "_end";
        assertNull(parser.parse(rawData));
    }
}
