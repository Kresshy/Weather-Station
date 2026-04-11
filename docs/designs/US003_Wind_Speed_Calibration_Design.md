# Design Document: Wind Speed Calibration (US003)

## Status
**Draft** - March 14, 2026

## Context
The current wind speed calculation in the Arduino firmware uses a single constant factor (`2.25`). Users report that measurements are not accurate across different wind speed ranges. To ensure precision, we need a more flexible calibration formula.

## Current State
*   **Formula**: `wind = (pulseCount / 2.0) * (2.25 / timeDeltaSeconds)`
*   **Issues**:
    1.  **Single-Point Calibration**: Only one constant is used, which doesn't account for friction/inertia at low speeds (offset) or non-linear aerodynamic behavior at high speeds.
    2.  **Hardcoded Constant**: The constant `2.25` may not match the physical characteristics of the anemometer in use.
    3.  **Overflow Risk**: `pulseCount` is a `byte` (max 255). With 5 windows and 2 edges per window, it overflows at ~25 m/s, causing incorrect readings.

## Proposed Solution

### 1. High-Efficiency Linear Formula (v = f * a + b)
We will implement a linear regression formula tailored for custom carbon-cup hardware:
`v = f * SCALE_FACTOR + OFFSET_CONSTANT`
*   `f`: Frequency in Hz (pulses per second).
*   `SCALE_FACTOR`: The slope of the calibration curve (m/s per Hz).
*   `OFFSET_CONSTANT`: The starting threshold (m/s) to overcome minimal bearing friction.

**Hardware Specifics (Custom Carbon 4-Cup Anemometer)**:
*   **Radius (R)**: 28.5 mm (0.0285 m)
*   **Encoder**: 5 Windows (5 pulses per full rotation)
*   **Anemometer Factor (K)**: 2.7 (Standard for 4-cup high-efficiency drag)
*   **Circumference (C)**: 2 * PI * R = 0.179 m
*   **Calculated Scale**: (K * C) / 5 = **0.0967**

### 2. Implementation in `weatherstation.ino`
*   **Configuration**:
    *   `WIND_SCALE`: Set to `0.097` (m/s per Hz).
    *   `WIND_OFFSET`: Set to `0.05` (minimal friction threshold).
*   **Safety Fix**: Change `pulseCount` from `byte` to `volatile unsigned int` to prevent counter overflow at high wind speeds.
*   **Stationary Check**: Implement a `0.01 Hz` threshold to ensure the offset isn't reported when the cups are not moving.

## Technical Tasks

### 1. Refactor `arduino/weatherstation.ino`
*   **Variable Update**: Change `pulseCount` declaration to `volatile unsigned int pulseCount = 0;`.
*   **Configuration Update**:
    ```cpp
    // --- Calibration Constants (Custom 4-Cup 5-Window Carbon Anemometer) ---
    // R = 28.5mm, 4 cups, 5 pulses/rev, K = 2.7
    const double WIND_SCALE = 0.097;   // Calculated: 0.0967 m/s per pulse/sec
    const double WIND_OFFSET = 0.05;  // Tunable: Starting threshold
    ```
*   **Calculation Update**:
    ```cpp
    // 1. Calculate Wind Speed (Atomic read of pulses)
    noInterrupts();
    unsigned int currentPulses = pulseCount;
    pulseCount = 0;
    interrupts();

    double frequency = (currentPulses / 2.0) / timeDeltaSeconds;
    // Apply linear calibration if moving, otherwise 0
    double wind = (frequency > 0.01) ? (frequency * WIND_SCALE + WIND_OFFSET) : 0.0;
    ```

## Verification Plan
*   **Bench Test**: Verify that at 10 Hz (50 pulses in 1 second), the reported speed is ~1.02 m/s.
*   **High-Speed Check**: Verify the counter does not reset or jump to zero at simulated speeds above 25 m/s.
