# Arduino Nano Wiring Diagram: Thermal Hunting Station

This diagram is based on the `arduino/weatherstation.ino` and `JIDownWind_micro_v1.0.ino` firmware. It is optimized for a single-sensor thermal detection setup.

## 🧩 Components Needed
*   **Arduino Nano** (Atmega328P)
*   **DS18B20** (High-precision Temperature Sensor)
*   **HC-05 or HC-06** (Bluetooth Serial Module)
*   **Anemometer** (Pulse-based/Magnetic Reed Switch or Hall Effect)
*   **Resistor**: 4.7kΩ (Critical for the DS18B20 OneWire bus)

---

## 🔌 ASCII Wiring Diagram

```text
       ┌────────────────────────────────────────────────────────┐
       │                    ARDUINO NANO                        │
       │                                                        │
       │             ┌───────┐         [USB]                    │
       │             │  [ ]  │ D1/TX ◄───────┐                  │
       │             │  [ ]  │ D0/RX ◄─────┐ │                  │
       │             │  [ ]  │ RESET       │ │  HC-05 BT MODULE │
       │             │  [ ]  │ GND         │ │  ┌────────────┐  │
       │             │  [ ]  │ D2  ◄───┐   │ └──┤ RXD        │  │
       │             │  [ ]  │ D3      │   └────┤ TXD        │  │
       │             │  [ ]  │ D4      │        │ GND        │  │
       │   ┌─────────┤ D5    │         │        │ VCC        │  │
       │   │         │  ...  │         │        └──────┬──┬──┘  │
       │   │         │  [ ]  │         │               │  │     │
       │   │         │  A2   │         │               │  │     │
       │   │         │  5V   ├─────────┼───────────────┘  │     │
       │   │         │  GND  ├─────────┼──────────────────┘     │
       │   │         └───────┘         │                        │
       └───┼───────────────────────────┼────────────────────────┘
           │                           │
           ▼                           ▼
    DS18B20 SENSOR                ANEMOMETER
   ┌──────────────┐            ┌──────────────┐
   │ [1] GND      │            │ [1] Signal   │───┐
   │ [2] Data     │◄──┐        │ [2] GND      │───┤
   │ [3] VCC      │───┼──┐     └──────────────┘   │
   └──────────────┘   │  │                        │
                      │  │       (To Nano GND) ───┘
       4.7kΩ PULLUP   │  │
       RESISTOR       │  │
       [Data to VCC] ─┘  │
                         │
       (To Nano 5V) ─────┘
```

---

## 📝 Pin Connections Summary

| Component | Arduino Pin | Notes |
| :--- | :--- | :--- |
| **DS18B20 (GND)** | GND | Common ground. |
| **DS18B20 (VCC)** | 5V | Power supply. |
| **DS18B20 (Data)** | **D5** | Connect the **4.7kΩ resistor** between Data and VCC. |
| **Anemometer** | **D2** | Connect one leg to D2 and the other to GND. |
| **HC-05 (TXD)** | **D0 (RX)** | Bluetooth module transmits to Arduino RX. |
| **HC-05 (RXD)** | **D1 (TX)** | Arduino transmits to Bluetooth module RX. |
| **HC-05 (VCC)** | 5V | Power supply. |
| **HC-05 (GND)** | GND | Common ground. |

---

## ⚠️ Critical Hardware Notes

### 1. USB Programming Conflict
Because the Bluetooth module shares the **D0 (RX)** and **D1 (TX)** hardware serial lines, you **must disconnect the Bluetooth RX/TX wires** whenever you upload new firmware to the Nano via the USB port. If left connected, the upload will likely fail.

### 2. The Pull-up Resistor
The DS18B20 will **not** be detected without the 4.7kΩ resistor. It acts as a pull-up on the OneWire data line to ensure signal integrity.

### 3. Logic Level Safety
The HC-05/06 RX pin is usually 3.3V logic. While many users connect it directly to the Nano's 5V TX (D1) pin, using a simple voltage divider (e.g., a 1kΩ and 2kΩ resistor) on that specific wire is safer for the module's long-term health.
