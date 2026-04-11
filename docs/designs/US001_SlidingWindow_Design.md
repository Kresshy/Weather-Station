# Design Document: Sliding Window Weather Data (US001)

## Status
**Draft** - March 14, 2026

## Context
Users want a "live" view of weather data without the ability or need to scroll back into the past. Historical data older than a user-defined interval (2, 3, 5, or 10 minutes) should be discarded to save memory and simplify the UI.

## Current State
*   **Repository**: Keeps a fixed history of 300 samples (`MAX_HISTORY_SIZE`).
*   **Charts**: Display up to 300 samples (by default) and start "scrolling" when this limit is reached.
*   **Interval Setting**: Exists in preferences (`pref_interval`) with values 60, 120, 300, 600, 1200 (seconds), but is inconsistently used as a "sample count" in some places.

## Proposed Solution

### 1. Global Time-Based Window (Repository)
Modify `WeatherRepositoryImpl` to prune `historicalData` based on the current system time and the selected interval.
*   **Interval Selection**: Read `pref_interval` and convert it to milliseconds.
*   **Pruning**: Every time a new `WeatherData` arrives, remove all items in `historicalData` where `currentTime - item.timestamp > selectedInterval`.
*   **Max History**: Remove the hardcoded `MAX_HISTORY_SIZE` and let the time window govern the size.

### 2. Synchronized "Single Heartbeat" Charts
*   The `ProcessedWeatherData` heartbeat will continue to drive updates.
*   Charts in `GraphViewFragment` and `DashboardFragment` (if applicable) will always display exactly what is in `historicalData`.
*   **X-Axis Reset**: The X-axis of the charts should represent relative time (e.g., seconds from the start of the window) or actual timestamps.

### 3. Preferences Update
*   Ensure `pref_interval` options match the user story: 2, 3, 5, and 10 minutes.
*   Rename internal variables from `numberOfSamples` to `windowIntervalSeconds`.

## Technical Tasks (TDD Implementation Plan)

### Phase 1: Repository Time-Based Pruning (TDD)
**Goal**: Transition from sample-count pruning to time-interval pruning in `WeatherRepositoryImpl`.

1.  **Step 1.1: Red (Failing Test)**
    *   **File**: `weatherstation/app/src/test/java/com/kresshy/weatherstation/repository/WeatherRepositoryImplTest.java`
    *   **Action**: Add `onRawDataReceived_PrunesHistoricalDataByTimeWindow()`.
    *   **Test Logic**:
        1. Mock `SharedPreferences` to return "120" (2 minutes) for `pref_interval`.
        2. Insert one `WeatherData` point.
        3. Manually set its timestamp to 3 minutes ago.
        4. Insert a second `WeatherData` point with a current timestamp.
        5. **Assert**: `getHistoricalWeatherData()` should only contain 1 point (the new one).
2.  **Step 1.2: Green (Minimal Implementation)**
    *   **File**: `weatherstation/app/src/main/java/com/kresshy/weatherstation/repository/WeatherRepositoryImpl.java`
    *   **Action**: Implement the `while` loop to prune `historicalData` by timestamp in `onRawDataReceived`. Update `loadSettings` to read the interval.
3.  **Step 1.3: Refactor**
    *   Remove `MAX_HISTORY_SIZE` and the old count-based pruning logic.

### Phase 2: Preference Alignment
**Goal**: Ensure the user can select the requested 2, 3, 5, and 10 minute intervals.

1.  **Step 2.1: Update XML Resources**
    *   **File**: `weatherstation/app/src/main/res/values/arrays.xml`
    *   **Action**: Update `pref_interval_entries` and `pref_interval_values` to:
        *   2 minutes (120)
        *   3 minutes (180)
        *   5 minutes (300)
        *   10 minutes (600)
2.  **Step 2.2: Verification**
    *   Run the project build and verify that `WeatherRepositoryImpl` correctly parses these new values.

### Phase 3: Graph View Sliding Window (TDD)
**Goal**: Update `GraphViewFragment` to render a true sliding window using the repository's data.

1.  **Step 3.1: Red (Failing Test)**
    *   **File**: Create `weatherstation/app/src/test/java/com/kresshy/weatherstation/fragment/GraphViewFragmentTest.java` (using Robolectric).
    *   **Action**: Add `updateUI_PrunesOldEntriesFromChart()`.
    *   **Test Logic**:
        1. Initialize fragment with a 2-minute interval.
        2. Push data points spanning 3 minutes.
        3. **Assert**: The `LineDataSet` entry count should match the points within the 2-minute window.
2.  **Step 3.2: Green (Minimal Implementation)**
    *   **File**: `weatherstation/app/src/main/java/com/kresshy/weatherstation/fragment/GraphViewFragment.java`
    *   **Action**: 
        *   Modify `addValueToSet` to use relative timestamps for X-axis.
        *   Rely on `repository.getHistoricalWeatherData()` for the pruned dataset.
        *   Set the `XAxis` maximum to the selected interval in seconds.
3.  **Step 3.3: Refactor**
    *   Cleanup the legacy `numberOfSamples` variable and replace it with `windowIntervalSeconds`.

## Verification Plan
1.  **Automated**: Run `./gradlew test` and ensure 100% pass rate for new and existing tests.
2.  **Manual**: 
    *   Launch the app in Simulator Mode.
    *   Set interval to 2 minutes.
    *   Verify the chart "slides" and old data disappears from the left edge exactly after 120 seconds.

