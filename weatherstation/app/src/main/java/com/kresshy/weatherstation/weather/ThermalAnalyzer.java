package com.kresshy.weatherstation.weather;

import com.kresshy.weatherstation.repository.WeatherRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Analyzes weather data to determine if conditions are suitable for thermal launching. Uses
 * Exponential Moving Averages (EMA) to detect trends in temperature and wind speed.
 */
@Singleton
public class ThermalAnalyzer {

    /** Container for the results of a weather data analysis. */
    public static class AnalysisResult {
        public final WeatherRepository.LaunchDecision decision;
        public final double tempTrend;
        public final double windTrend;
        public final int score;

        /**
         * @param decision The calculated launch decision.
         * @param tempTrend The temperature trend (delta between fast and slow EMA).
         * @param windTrend The wind speed trend (delta between fast and slow EMA).
         * @param score The 0-100 thermal suitability score.
         */
        public AnalysisResult(
                WeatherRepository.LaunchDecision decision,
                double tempTrend,
                double windTrend,
                int score) {
            this.decision = decision;
            this.tempTrend = tempTrend;
            this.windTrend = windTrend;
            this.score = score;
        }
    }

    private static final int HISTORY_SIZE = 60;
    private static final double FAST_ALPHA = 0.5;
    private static final double SLOW_ALPHA = 0.1;

    private final List<WeatherData> recentHistory = new ArrayList<>();
    private double fastEmaTemp = -1;
    private double slowEmaTemp = -1;
    private double fastEmaWind = -1;
    private double slowEmaWind = -1;

    private boolean isEnabled = true;
    private double sensitivityFactor = 1.0;

    @Inject
    public ThermalAnalyzer() {}

    /**
     * @param enabled Set to false to always return WAITING/0 score.
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    /**
     * @param sensitivity Factor to multiply the final score by (e.g., 0.7 to 1.3).
     */
    public void setSensitivity(double sensitivity) {
        this.sensitivityFactor = sensitivity;
    }

    /**
     * Analyzes the current weather data point against historical trends.
     *
     * @param current The latest weather reading.
     * @return An AnalysisResult containing trends, score, and launch decision.
     */
    public AnalysisResult analyze(WeatherData current) {
        updateHistory(current);

        if (!isEnabled) {
            return new AnalysisResult(WeatherRepository.LaunchDecision.WAITING, 0, 0, 0);
        }

        if (fastEmaTemp == -1) {
            initializeEma(current);
            return new AnalysisResult(WeatherRepository.LaunchDecision.WAITING, 0, 0, 0);
        }

        updateEma(current);

        double tempDelta = fastEmaTemp - slowEmaTemp;
        double windDelta = fastEmaWind - slowEmaWind;
        double stdDevWind = calculateWindStdDev();

        int score = calculateScore(tempDelta, windDelta, stdDevWind, current.getWindSpeed());
        WeatherRepository.LaunchDecision decision = determineDecision(score, tempDelta);

        return new AnalysisResult(decision, tempDelta, windDelta, score);
    }

    private void updateHistory(WeatherData current) {
        recentHistory.add(current);
        if (recentHistory.size() > HISTORY_SIZE) {
            recentHistory.remove(0);
        }
    }

    private void initializeEma(WeatherData current) {
        fastEmaTemp = current.getTemperature();
        slowEmaTemp = current.getTemperature();
        fastEmaWind = current.getWindSpeed();
        slowEmaWind = current.getWindSpeed();
    }

    private void updateEma(WeatherData current) {
        fastEmaTemp = (current.getTemperature() * FAST_ALPHA) + (fastEmaTemp * (1 - FAST_ALPHA));
        slowEmaTemp = (current.getTemperature() * SLOW_ALPHA) + (slowEmaTemp * (1 - SLOW_ALPHA));
        fastEmaWind = (current.getWindSpeed() * FAST_ALPHA) + (fastEmaWind * (1 - FAST_ALPHA));
        slowEmaWind = (current.getWindSpeed() * SLOW_ALPHA) + (slowEmaWind * (1 - SLOW_ALPHA));
    }

    private double calculateWindStdDev() {
        if (recentHistory.isEmpty()) return 0;

        double sumWind = 0;
        for (WeatherData d : recentHistory) {
            sumWind += d.getWindSpeed();
        }
        double avgWind = sumWind / recentHistory.size();

        double sumSqDiff = 0;
        for (WeatherData d : recentHistory) {
            sumSqDiff += Math.pow(d.getWindSpeed() - avgWind, 2);
        }

        return Math.sqrt(sumSqDiff / recentHistory.size());
    }

    private int calculateScore(
            double tempDelta, double windDelta, double stdDevWind, double currentWind) {
        double score = 0;
        if (tempDelta > 0) score += Math.min(50, (tempDelta * 200));
        if (windDelta < 0) score += Math.min(30, (Math.abs(windDelta) * 60));
        if (stdDevWind < 1.0) score += ((1.0 - Math.min(1.0, stdDevWind)) * 20);
        if (tempDelta < -0.1) score -= 30;
        if (currentWind > 5.0) score -= 40;

        // Apply user-defined sensitivity
        score *= sensitivityFactor;

        return (int) Math.max(0, Math.min(100, score));
    }

    private WeatherRepository.LaunchDecision determineDecision(int score, double tempDelta) {
        if (score >= 70) return WeatherRepository.LaunchDecision.LAUNCH;
        if (score >= 40) return WeatherRepository.LaunchDecision.POTENTIAL;
        if (score < 20 || tempDelta < -0.05) return WeatherRepository.LaunchDecision.POOR;
        return WeatherRepository.LaunchDecision.WAITING;
    }

    /** Resets the analyzer state, clearing history and EMA values. */
    public void reset() {
        recentHistory.clear();
        fastEmaTemp = -1;
        slowEmaTemp = -1;
        fastEmaWind = -1;
        slowEmaWind = -1;
    }
}
