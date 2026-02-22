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

    /** Torture test for very messy legacy strings. */
    @Test
    public void parse_LegacyFormatTortureTest() {
        // Mixed delimiters and erratic framing
        String[] tortureStrings = {
            "5.5;22.2", // Semicolon delimiter, no framing
            "start_5.5, 22.2", // Missing suffix, comma+space
            "5.5 22.2_end", // Missing prefix
            "  5.5   22.2  ", // Purely raw whitespace
            "start_5.5;22.2;0_end" // Node ID with semicolons
        };

        for (String input : tortureStrings) {
            WeatherData result = parser.parse(input);
            String msg = "Failed to parse: " + input;
            assertEquals(msg, 5.5, result.getWindSpeed(), 0.001);
            assertEquals(msg, 22.2, result.getTemperature(), 0.001);
        }
    }

    /** Verifies that legacy frames preceded by junk data are parsed correctly. */
    @Test
    public void parse_LegacyWithLeadingJunk_ReturnsCorrectData() {
        String rawData = "31\n30\nstart_5.5 22.2_end";
        WeatherData result = parser.parse(rawData);
        assertEquals(5.5, result.getWindSpeed(), 0.001);
        assertEquals(22.2, result.getTemperature(), 0.001);
    }

    /** Verifies that JSON frames preceded by junk data are parsed correctly. */
    @Test
    public void parse_JsonWithLeadingJunk_ReturnsCorrectData() {
        String json =
                "{\"version\":2,\"numberOfNodes\":1,\"measurements\":[{\"windSpeed\":5.5,\"temperature\":22.2,\"nodeId\":0}]}";
        String rawData = "31\n30\nWS_" + json + "_end";
        WeatherData result = parser.parse(rawData);
        assertEquals(5.5, result.getWindSpeed(), 0.001);
        assertEquals(22.2, result.getTemperature(), 0.001);
    }
}
