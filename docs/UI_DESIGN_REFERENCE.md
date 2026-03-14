# Weather Station UI Design Reference

This document serves as a layout and UI reference for the "Pro-Pilot" dashboard, optimized for thermal hunting and free-flight aeromodelling.

## 📐 Layout: The "Split-Row" Dashboard

The dashboard uses a vertical stack of rows. Each row contains a primary **Line Chart** for long-term trends and an **Instant Gauge** on the right for immediate status.

### Visual Mockup

```text
+-------------------------------------------------------+
| [ SCORE: 85 ]   STABLE LIFT DETECTED                  |
+-------------------------------------------------------+
| RSSI: -65dBm                         Updated: 2s ago  |
+---------------------------------------+---------------+
|                                       |     ( N )     |
|  WIND SPEED CHART (Trend)             |    W--+--E    | <-- COMPASS
|  (Full height of row)                 |     ( S )     | (Directly next
|  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~  |  2.4 m/s       |  to Wind Speed)
+---------------------------------------+---------------+
|                                       |     |---|     |
|  TEMPERATURE CHART (Trend)            |     |BAR|     | <-- PRESSURE
|  (Full height of row)                 |     |---|     |     BAR
|  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~  |  1012.4 hPa   | (Directly next
|                                       |               |  to Temperature)
+---------------------------------------+---------------+
```

## 🧩 Component Details

### 1. Wind Compass (Top Right Sidebar)
*   **Purpose:** Instant visualization of wind vector shifts.
*   **Behavior:** 
    - A needle indicates the direction the wind is coming *from*.
    - Rapid spinning or oscillation indicates turbulence/thermal proximity.
*   **Data Source:** `windDirection` (0-359°).

### 2. Barometric Pressure Bar (Bottom Right Sidebar)
*   **Purpose:** Detect localized pressure drops that precede thermals.
*   **Behavior:** 
    - A vertical stack bar showing current pressure.
    - Optional: Add a "Delta" indicator (e.g., a small arrow showing if pressure has dropped in the last 10 seconds).
*   **Data Source:** `pressure` (hPa).

### 3. Trend Charts (Main Area)
*   **Implementation:** `MPAndroidChart` LineCharts with `CUBIC_BEZIER` smoothing.
*   **X-Axis:** Rolling window of samples (e.g., 300 samples).
*   **Interaction:** Full-width width allows for detailed analysis of air cycles.

## 🛠️ Technical Implementation Strategy

1.  **Layout:** Use a `LinearLayout` (Vertical) for the main container.
2.  **Rows:** Each row is a `LinearLayout` (Horizontal) or `GridLayout` with a weight-based split (e.g., 75% Chart / 25% Sidebar).
3.  **Custom Views:** 
    - Create a `CompassView` (Custom `View` using `onDraw` for the dial and needle).
    - Create a `PressureBarView` (Stylized `ProgressBar` or custom `View`).
4.  **Data Binding:** Update `DashboardFragment` to observe the new `pressure` and `windDirection` LiveData from the `WeatherViewModel`.
