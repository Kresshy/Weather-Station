# Weather Station for Free Flight Aeromodels

A comprehensive weather monitoring and thermal analysis system specifically designed for **free-flight aeromodelling**. By deploying static weather stations on an airfield, this system monitors critical changes in wind speed and temperature to detect thermal activity in real-time. Launching a model into a thermal detected by this system significantly increases elevation and extends flight times.

## 🚀 Key Features

- **Thermal Detection**: Purpose-built algorithm to identify rising air currents (thermals) suitable for aeromodel launches.
- **Real-time Airfield Monitoring**: Track precise wind speed and temperature readings via Bluetooth at the launch site.
- **Single Heartbeat Architecture**: Guaranteed synchronization between raw sensor readings and analytical trends.
- **Advanced Trend Analysis**: Uses Exponential Moving Averages (EMA) to detect subtle thermal pulses, providing a "Launch Suitability" score (0-100).
- **High-Performance Visualization**: Linear, real-time graphs optimized for legacy hardware (Android 6.0+) with zero-lag $O(1)$ data plotting.
- **Global Compatibility**: Full support for international locales (comma/dot decimal separators) and legacy data formats.

## 🏗️ System Architecture

The application follows **Clean Architecture** principles with a decoupled **Control vs. Data Plane** design:

1.  **UI Layer (MVVM)**: Fragments observe a unified `WeatherUiState` from the `WeatherViewModel`.
2.  **Domain Layer (UseCases)**: The bridge that aggregates data from both planes.
3.  **Data Plane (`WeatherRepository`)**: Exclusively handles sensor data processing, parsing, and analytical trends using the **Single Heartbeat** pattern.
4.  **Control Plane (`WeatherConnectionController`)**: Exclusively handles hardware lifecycles, Bluetooth adapter management, and connection orchestration.
Detailed hardware documentation can be found here:
- **[Standard Wiring Diagram](WIRING_DIAGRAM.md)**: Current Arduino Nano setup.
- **[Future Hardware Roadmap](FUTURE_IMPROVEMENTS.md)**: Potential sensor upgrades.

## 🛡️ Data Integrity & Performance

To ensure stable charts and reliable thermal analysis across various hardware generations (API 23-35), the system implements a multi-layer strategy:

1.  **Atomic Event Pipeline (v3.2.0)**:
    - Bundles raw data and analytical trends into a single `ProcessedWeatherData` heartbeat.
    - Eliminates "UI jitter" and ensures that text displays and charts are always in mathematical sync.
2.  **Performance Optimization (v3.1.5+)**:
    - **Constant-Time Plotting**: Removed $O(N)$ chart shifting loops. Adding data is now a lightweight operation, ensuring smooth performance even on 10-year-old devices.
    - **Initial Full Grid**: Charts initialize with a fixed 300-sample grid for consistent airfield viewing.
    - **Linear Rendering**: Pure linear connections between data points for a technical, accurate visualization with minimal CPU/GPU overhead.
3.  **Universal Legacy Parser**:
    - **Locale Independence**: Automatically handles both dot (`.`) and comma (`,`) decimal separators, preventing crashes on international devices.
    - **Format Agnostic**: Support for both JSON and legacy space/comma-separated PDU formats.
4.  **Bluetooth & Platform Stability**:
    - **Client-Only Mode**: Optimized for stability by removing server-socket overhead.
    - **Android 6.0+ Resiliency**: Specialized `PermissionHelper` and Java 8 compatibility fixes ensure 100% reliability on older airfield tablets.

## 📥 Getting Started

### 1. Arduino Setup
Flash the definitive firmware located at **`arduino/weatherstation.ino`**.
- **Dependencies**: `OneWire`, `ArduinoJson` (v5.x).
- **Baud Rate**: 9600 baud.

### 2. Android App
Open the `/weatherstation` folder in Android Studio.
- **Tech Stack**: Java 17, Dagger Hilt, ViewBinding, MPAndroidChart.
- **Minimum SDK**: 23 (Android 6.0).
- **Target SDK**: 35 (Android 15).

## 🧪 Testing & Quality Control

The project uses a mandatory validation sequence for every change:
```bash
./gradlew spotlessApply test build
```
- **Unit Tests**: 50+ tests verifying thermal analysis, parsing, and UI state synchronization.
- **Static Analysis**: PMD and Android Lint for code quality.

## 🔢 Versioning

This project follows [Semantic Versioning (SemVer)](https://semver.org/):
Detailed version changes can be found in the [changelogs/](changelogs/) directory.

## 📄 License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

Copyright (c) 2014-2026 Szabolcs Varadi
