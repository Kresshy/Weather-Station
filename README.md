# Weather Station for Free Flight Aeromodels

A comprehensive weather monitoring and thermal analysis system specifically designed for **free-flight aeromodelling**. By deploying static weather stations on an airfield, this system monitors critical changes in wind speed and temperature to detect thermal activity in real-time. Launching a model into a thermal detected by this system significantly increases elevation and extends flight times.

## 🚀 Key Features

- **Thermal Detection**: Purpose-built algorithm to identify rising air currents (thermals) suitable for aeromodel launches.
- **Real-time Airfield Monitoring**: Track precise wind speed and temperature readings via Bluetooth at the launch site.
- **Advanced Trend Analysis**: Uses Exponential Moving Averages (EMA) to detect subtle thermal pulses, providing a "Launch Suitability" score (0-100).
- **Data Visualization**: Interactive real-time graphs for monitoring temperature and wind speed trends during a flight session.
- **Mobile Access**: Modern Android application (Java) optimized for field use.

## 🏗️ System Architecture

The repository is organized into two main components:

1.  **/weatherstation**: The primary Android application (Java/Dagger Hilt).
2.  **/arduino**: Unified firmware implementation (`weatherstation.ino`) for Arduino Nano/Micro/Uno.

Detailed hardware documentation can be found here:
- **[Standard Wiring Diagram](WIRING_DIAGRAM.md)**: Current Arduino Nano setup.
- **[Future Hardware Roadmap](FUTURE_IMPROVEMENTS.md)**: Potential sensor upgrades (Ultrasonic, Barometer).
- **[Future Wiring Diagram](FUTURE_WIRING_DIAGRAM.md)**: Technical wiring for upgraded sensors.

## 🛠️ Hardware Requirements

To build a physical station, you will typically need:

- **Microcontroller**: Arduino Nano, Micro, or Uno.
- **Sensors**: 
    - DS18B20 or DS18S20 high-resolution temperature sensors (OneWire).
    - Precision Anemometer (Hall effect or magnetic switch).
- **Connectivity**: HC-05 or HC-06 Bluetooth module for mobile connection.
- **Power**: 9V battery or a similar portable power supply for field use.

## 📥 Getting Started

### 1. Arduino Setup
Flash the firmware located at `arduino/weatherstation.ino`.
- **Dependencies**: `OneWire`, `ArduinoJson` (v5.x).
- **Wiring**: See [WIRING_DIAGRAM.md](WIRING_DIAGRAM.md) for details.

### 2. Android App
Open the `/weatherstation` folder in Android Studio.
- **Tech Stack**: Java 17, Dagger Hilt for DI, ViewBinding, MPAndroidChart.
- **Minimum SDK**: 23 (Android 6.0).
- **Target SDK**: 35 (Android 15).

## 🛡️ Data Integrity & Protocol Compatibility

To ensure stable charts and reliable thermal analysis across various hardware generations, the system implements a multi-layer compatibility and filtering strategy:

1.  **Universal Legacy Parser (Application Layer)**:
    - **Backwards Compatibility**: Automatically handles both modern `WS_` and legacy `start_` PDU prefixes.
    - **Format Agnostic**: Support for both JSON and space/comma-separated raw data formats, allowing the app to work with every version of the station firmware without updates.
    - **Whitespace Immunity**: Resilient to erratic formatting, extra newlines, and trailing spaces from older serial implementations.
2.  **Bluetooth Stability (Connection Layer)**:
    - **Race Condition Protection**: Resolved a critical socket-management bug that caused instant disconnections after successful handshakes.
    - **Fallback Support**: Implements automated RFCOMM fallback for improved pairing reliability on older Android devices.
3.  **Sensor Filtering (Firmware & App Layers)**:
    - **Outlier Rejection**: Discards temperature jumps > 10°C/sec (physically impossible air shifts).
    - **OneWire Guard**: Firmware rejects `85.0°C` power-on defaults and `-127.0°C` bus errors.
    - **Smoothing**: Uses Exponential Moving Averages (EMA) for trend detection and stable "Launch Suitability" scoring.

## 🧪 Testing & Quality Control

The project includes a comprehensive suite of tools to ensure reliability:

- **Unit Tests**: Verifies thermal analysis logic and data flow.
  ```bash
  ./gradlew test
  ```
- **Static Analysis (PMD)**: Checks for dead code and unused variables.
  ```bash
  ./gradlew pmd
  ```
- **Android Lint**: Verifies resource integrity and Android-specific best practices.
  ```bash
  ./gradlew lint
  ```

## 📡 Protocol Specification

The system uses a custom PDU (Protocol Data Unit) format for Bluetooth communication:
- **Format**: `WS_{JSON_PAYLOAD}_end`
- **Payload Example**:
  ```json
  {
    "version": 2,
    "numberOfNodes": 1,
    "measurements": [
      {
        "windSpeed": 3.5,
        "temperature": 22.4,
        "nodeId": 0
      }
    ]
  }
  ```

## 📄 License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

Copyright (c) 2014-2026 Szabolcs Varadi
