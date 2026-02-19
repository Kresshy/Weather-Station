# Future Improvements: Thermal Hunting Station

This document outlines potential hardware and software enhancements to evolve the current Weather Station into a high-precision Thermal Hunting system for free-flight aeromodelling.

## 📡 Hardware Sensor Enhancements

### 1. High-Precision Barometric Pressure (BMP280 / BME280)
*   **The Upgrade:** Integrate a sensor capable of measuring micro-changes in atmospheric pressure.
*   **The Benefit:** Thermals are preceded by a slight localized pressure drop before the temperature rises. This provides a "Pre-Thermal" warning, giving the pilot extra seconds to prepare for launch.

### 2. Ultrasonic Anemometer (Solid State)
*   **The Upgrade:** Replace the mechanical spinning cups with an ultrasonic sensor (no moving parts).
*   **The Benefit:** Mechanical anemometers have physical inertia and can't detect instantaneous wind shifts or very light "dead air" accurately. Ultrasonic sensors provide zero-latency wind speed data, critical for timed free-flight launches.

### 3. Wind Direction (Vane or 2D Ultrasonic)
*   **The Upgrade:** Add a wind vane or a multi-axis ultrasonic array.
*   **The Benefit:** Thermals "inhale" air from their surroundings. A sudden shift in wind direction often indicates a thermal is passing nearby, even if the station isn't directly in its path.

---

## 📱 Android App Enhancements

### 1. Thermal "Launch Window" Timer
*   **Feature:** Predictive countdown based on the rate of change in pressure and temperature.
*   **Impact:** Instead of just a suitability score, the app provides an estimated "Peak Thermal" time, helping the pilot time their release for maximum lift.

### 2. Audio/Haptic "Variometer" Alerts
*   **Feature:** Variable-pitch audio tones that rise or fall based on thermal strength.
*   **Impact:** Allows the pilot to keep their eyes on the model and the sky while receiving real-time "acoustic" feedback on air quality.

### 3. Wind Vector Visualization (Compass UI)
*   **Feature:** A 2D compass overlay showing the current wind direction and intensity.
*   **Impact:** Helps the pilot visualize which direction the thermal is "pulling" from, aiding in the decision of which direction to throw the model.

### 4. Session Logging & Cycle Analysis
*   **Feature:** Local database (Room/SQLite) to record every thermal cycle during a session.
*   **Impact:** Automatically calculates the average time between thermals (e.g., "thermals are triggering every 12 minutes today"), allowing the pilot to predict the next big lift cycle.

---

## 🛠️ Required Technical Updates
1.  **Protocol:** Update `WeatherMessageParser` to handle new JSON fields (`pressure`, `direction`).
2.  **Analysis Logic:** Update `ThermalAnalyzer` to incorporate pressure-drop rates into the `LaunchDecision` algorithm.
3.  **UI:** Add a Compass View and Variometer Audio Service to the Android application.
