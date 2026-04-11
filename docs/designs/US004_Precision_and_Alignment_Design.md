# Design Document: Initial Point Alignment & High-Accuracy Precision (US004)

## Status
**Draft** - March 14, 2026

## Context
When a session begins, the user wants the first data point to align perfectly with the left edge of the chart (X:0). Additionally, the precision of weather values in the UI needs to be increased from 1 to 2 decimal places to support more granular analysis.

## Current State
*   **Initial Point**: The X-axis offset is calculated using `(timestamp - firstTimestamp) / 1000f`. This works well but doesn't explicitly ensure that the first point is always at X:0 if the `firstTimestamp` is reset.
*   **UI Precision**: Current `String.format` calls use `%.1f` (1 decimal place) for both wind speed and temperature.

## Proposed Solution

### 1. Right-to-Left Sliding Window
Instead of calculating offsets from a fixed `firstTimestamp`, the chart will use relative offsets from the **latest** data point to ensure data "flows" from right to left.
*   **Implementation**: 
    *   The latest data point will always have the highest X value.
    *   The X-axis window will be set to `[latestX - interval, latestX]`.
    *   This ensures new data appears on the right, and as time passes, it moves to the left until it is pruned.
    *   **Initial State**: When the first point arrives, it will be at X:0, and the window will be `[0, interval]`, appearing in the bottom-left as requested. As more points arrive, they fill towards the right. Once the interval is exceeded, the window slides.

### 2. High-Accuracy Precision (2 Decimals)
*   **Implementation**: Update all UI string formatting to use `%.2f`.
*   **Locations**:
    *   `DashboardFragment.java` (Overlay text)
    *   `GraphViewFragment.java` (Overlay text)
    *   `strings.xml` (Trend formats)

## Technical Tasks

### 1. Refactor `GraphViewFragment.java`
*   Update `addValueToSet` and `populateChartsFromHistory` to use 2-decimal formatting: `%.2f`.
*   Ensure the `XAxis` is configured to follow the latest data point:
    ```java
    chart.getXAxis().setAxisMinimum(latestX - windowIntervalSeconds);
    chart.getXAxis().setAxisMaximum(latestX);
    ```

### 2. Update `DashboardFragment.java`
*   Locate and update all `String.format` calls to use `%.2f` for wind and temperature.

### 3. Update `res/values/strings.xml`
*   Update trend string formats to `%.2f`.


## Verification Plan
*   **Manual Test**:
    1. Connect to a station.
    2. Verify the first point appears at the far left (X:0) of the chart.
    3. Verify current value displays (e.g., "12.34 m/s") show two decimal places.
    4. Verify trend displays (e.g., "+0.05°") show two decimal places.
