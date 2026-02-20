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
2.  **Bluetooth & Platform Stability**:
    - **Android 6.0+ Compatibility**: Implemented a specialized `PermissionHelper` and removed Java 8 `Stream` APIs to ensure 100% compatibility with legacy devices (API 23+).
    - **Emulator & No-BT Support**: Gracefully handles missing Bluetooth hardware, allowing the app to run in **Simulator Mode** on standard Android emulators.
    - **Proactive Reconnection**: Automatically prompts to reconnect to the last known station immediately after Bluetooth is enabled on startup.
    - **Race Condition Protection**: Resolved socket-management bugs that caused disconnections during state transitions.
3.  **UI/UX & Visualization**:
    - **High-Visibility Charts**: Enhanced aesthetics with bold **5.0f** lines and prominent **3.5f** solid data points for clear airfield viewing.
    - **Dynamic Status**: Replaces cryptic "RSSI: N/A" with a clear "Status: Connected" message when signal strength is unavailable.
    - **User-Friendly Signal**: Maps raw dBm values to intuitive labels (Excellent, Good, Fair, Poor).

## 📱 Legacy Device Compatibility (Android 6.0 - 11.0)

While the app is optimized for modern Android versions, it fully supports legacy devices with the following limitations and requirements:

- **Location Services Mandatory**: On Android 6.0 through 9.0, Bluetooth scanning **will not find any devices** unless the system **Location (GPS)** toggle is turned **ON** in the quick settings. This is a platform-level requirement for Bluetooth discovery on these versions.
- **Hardware Limitations**: Emulators and devices without Bluetooth hardware can still run the app using **Simulator Mode** (enabled in Settings).
- **UI Performance**: Complex charts are optimized with Cubic Bézier smoothing, but very old devices may experience slight frame drops during high-frequency data updates.
- **Backgrounding**: Android 6 is aggressive with power management. Ensure "Battery Optimization" is disabled for the app to keep the weather service alive during long field sessions.

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
