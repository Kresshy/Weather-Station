# Future Upgraded Wiring: High-Precision Thermal Hunting Station

This document covers the planned evolution of the Weather Station hardware, from adding a barometric sensor to a full 2D Ultrasonic Anemometer setup.

---

## 🛰️ Phase 1: Barometric Pressure (BMP280/BME280)
Adds micro-pressure drop detection to provide an early "Pre-Thermal" warning before the temperature rises.

### 🔌 Wiring (I2C)
*   **BMP280 SDA** -> Arduino Nano **A4**
*   **BMP280 SCL** -> Arduino Nano **A5**
*   **BMP280 VCC** -> 3.3V (or 5V if module has a regulator)
*   **BMP280 GND** -> Common GND

---

## 🌪️ Phase 2: 2D Ultrasonic Anemometer (Ultimate Setup)
Replaces the mechanical anemometer with a zero-inertia ultrasonic sensor that measures both **Wind Speed** and **Direction Angle (0-359°)**.

### 🔌 Ultimate Wiring Diagram (ASCII)

```text
       ┌────────────────────────────────────────────────────────┐
       │             ARDUINO NANO (ULTIMATE)                    │
       │                                                        │
       │             ┌───────┐          [USB]                   │
       │             │  [ ]  │ D1/TX ──────┐   HC-05 BT MODULE   │
       │             │  [ ]  │ D0/RX ────┐ │   ┌────────────┐    │
       │             │  [ ]  │ RESET     │ └───┤ RXD        │    │
       │             │  [ ]  │ GND       └─────┤ TXD        │    │
       │      ┌──────┤ D2    │             ┌───┤ GND        │    │
       │      │ ┌────┤ D3/RX ◄───────┐     │ ┌─┤ VCC        │    │
       │      │ │ ┌──┤ D4/TX ──────┐ │     │ │ └────────────┘    │
       │   ┌──┼─┼─┼──┤ D5    │     │ │     │ │                   │
       │   │  │ │ │  │  ...  │     │ │     │ │                   │
       │   │  │ │ │  │ A4/SDA◄──┐  │ │     │ │                   │
       │   │  │ │ │  │ A5/SCL◄┐ │  │ │     │ │                   │
       │   │  │ │ │  │  5V   ─┴─┴──┼─┼─────┴─┼──────────────────┐│
       │   │  │ │ │  │  GND  ──────┴─┴───────┴─────────────────┐││
       │   │  │ │ │  └───────┘                                 │││
       └───┼──┼─┼─┼────────────────────────────────────────────┼┼┘
           │  │ │ │                                            ││
           ▼  ▼ ▼ ▼                                            ▼ ▼
    DS18B20  BMP280         ANEMOMETER (ULTRASONIC)          POWER BUS
   ┌───────┐┌──────┐      ┌─────────────────────────┐      ┌──────────┐
   │ [1]GND││[1]VCC│      │ [1] VCC (12V-24V Input) │ <─── │ 12V (+)  │
   │ [2]DAT││[2]GND│      │ [2] GND (Common)        │ <─── │ GND (-)  │
   │ [3]VCC││[3]SCL│      │ [3] RX (from D4/TX)     │      └──────────┘
   └───────┘│[4]SDA│      │ [4] TX (from D3/RX)     │
            └──────┘      └─────────────────────────┘
```

### 📝 Pin Connections Summary (Full Setup)

| Component | Arduino Pin | Comm Type | Notes |
| :--- | :--- | :--- | :--- |
| **HC-05 Bluetooth** | **D0 / D1** | HW Serial | Primary mobile link. |
| **Ultrasonic Sensor**| **D3 / D4** | SoftSerial | Measures speed + direction. |
| **BMP280 Barometer**| **A4 / A5** | I2C | Micro-pressure detection. |
| **DS18B20 Temp** | **D5** | OneWire | High-res temperature. |

---

## ⚠️ Important Implementation Notes

### 1. Power Supply (External)
Most 2D Ultrasonic Anemometers are industrial devices and require **12V or 24V DC**. 
*   **Common Ground:** You **must** connect the ground (-) of your external 12V battery to the **Arduino GND**. Without a common ground, serial communication will fail.

### 2. SoftwareSerial for Anemometer
Since Pins 0 and 1 are used by Bluetooth, the Anemometer uses **SoftwareSerial** on Pins 3 and 4.
*   **Nano D3 (RX)** connects to Anemometer **TX**.
*   **Nano D4 (TX)** connects to Anemometer **RX**.
