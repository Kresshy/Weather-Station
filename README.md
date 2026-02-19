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
2.  **/arduino**: Firmware implementations for Arduino Nano/Micro/Uno.

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
Flash the firmware located in the `/arduino` directory.
- **Dependencies**: `OneWire`, `ArduinoJson`, `SoftwareSerial`.
- **Wiring**: Temperature sensor on pin 5, Anemometer interrupt on pin 2 (for newer versions).

### 2. Android App
Open the `/weatherstation` folder in Android Studio.
- **Tech Stack**: Java 17, Dagger Hilt for DI, ViewBinding, MPAndroidChart.
- **Minimum SDK**: 23 (Android 6.0).
- **Target SDK**: 35 (Android 15).

## 🛡️ Data Integrity & Spike Protection

To ensure stable charts and reliable thermal analysis, the system implements a **Double-Layer Filter** to eliminate false sensor readings:

1.  **Firmware Layer (Arduino)**:
    - Rejects the `85.0°C` power-on default value of the DS18B20 sensor.
    - Filters out `-127.0°C` error values caused by OneWire bus disconnections.
    - Limits readings to a sane environmental range (-30°C to 60°C).
2.  **Application Layer (Android)**:
    - **Outlier Rejection**: Discards any temperature jump greater than 10°C per second (physically impossible for air temperature).
    - **Smoothing**: Uses Exponential Moving Averages (EMA) for trend detection and stable "Launch Suitability" scoring.

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
*Legacy support for space-separated values `{windSpeed} {temperature}` is also maintained.*

## 📄 License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

Copyright (c) 2014-2026 Szabolcs Varadi
