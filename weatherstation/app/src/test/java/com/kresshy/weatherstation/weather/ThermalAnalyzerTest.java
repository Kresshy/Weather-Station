package com.kresshy.weatherstation.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.kresshy.weatherstation.repository.WeatherRepository;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the {@link ThermalAnalyzer}. Verifies EMA accuracy, trend detection (rising temp,
 * falling wind), scoring logic, and launch decision thresholds.
 */
public class ThermalAnalyzerTest {

    private ThermalAnalyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new ThermalAnalyzer();
    }

    /** Verifies that the first data point correctly initializes EMAs. */
    @Test
    public void analyze_InitializesOnFirstData() {
        WeatherData data = new WeatherData(2.0, 25.0);
        ThermalAnalyzer.AnalysisResult result = analyzer.analyze(data);

        assertEquals(WeatherRepository.LaunchDecision.WAITING, result.decision);
        assertEquals(0.0, result.tempTrend, 0.001);
        assertEquals(0.0, result.windTrend, 0.001);
        assertEquals(0, result.score);
    }

    /** Verifies that consistently rising temperature is detected as a positive trend. */
    @Test
    public void analyze_DetectsRisingTemperature() {
        // Initialize
        analyzer.analyze(new WeatherData(2.0, 25.0));

        // Simulate rising temperature over multiple readings
        ThermalAnalyzer.AnalysisResult result = null;
        for (int i = 1; i <= 10; i++) {
            result = analyzer.analyze(new WeatherData(2.0, 25.0 + (i * 0.1)));
        }

        assertTrue("Temperature trend should be positive", result.tempTrend > 0);
        assertTrue("Score should increase with rising temp", result.score > 0);
    }

    /** Verifies that consistently falling wind speed is detected as a negative trend. */
    @Test
    public void analyze_DetectsFallingWind() {
        // Initialize
        analyzer.analyze(new WeatherData(5.0, 25.0));

        // Simulate falling wind
        ThermalAnalyzer.AnalysisResult result = null;
        for (int i = 1; i <= 10; i++) {
            result = analyzer.analyze(new WeatherData(5.0 - (i * 0.2), 25.0));
        }

        assertTrue("Wind trend should be negative (falling)", result.windTrend < 0);
        assertTrue("Score should increase with falling wind", result.score > 0);
    }

    /** Verifies that ideal conditions (rising temp + falling wind) trigger a LAUNCH decision. */
    @Test
    public void analyze_TriggersLaunchDecision() {
        // Initialize
        analyzer.analyze(new WeatherData(5.0, 25.0));

        // Simulate ideal conditions: Rising temp AND falling wind
        ThermalAnalyzer.AnalysisResult result = null;
        for (int i = 1; i <= 20; i++) {
            result = analyzer.analyze(new WeatherData(5.0 - (i * 0.2), 25.0 + (i * 0.2)));
        }

        assertEquals(WeatherRepository.LaunchDecision.LAUNCH, result.decision);
        assertTrue("Score should be high for ideal conditions", result.score >= 70);
    }

    /** Verifies that high wind speed correctly triggers a POOR decision. */
    @Test
    public void analyze_TriggersPoorDecisionOnHighWind() {
        // Initialize
        analyzer.analyze(new WeatherData(2.0, 25.0));

        // High wind (over 5.0)
        ThermalAnalyzer.AnalysisResult result = analyzer.analyze(new WeatherData(6.0, 25.0));

        assertEquals(WeatherRepository.LaunchDecision.POOR, result.decision);
    }

    /** Verifies that reset() correctly clears all internal state. */
    @Test
    public void reset_ClearsHistoryAndEma() {
        // Fill history
        analyzer.analyze(new WeatherData(2.0, 25.0));
        analyzer.analyze(new WeatherData(3.0, 26.0));

        analyzer.reset();

        // After reset, it should behave like the first initialization again
        ThermalAnalyzer.AnalysisResult result = analyzer.analyze(new WeatherData(2.0, 25.0));
        assertEquals(WeatherRepository.LaunchDecision.WAITING, result.decision);
        assertEquals(0.0, result.tempTrend, 0.001);
    }
}
