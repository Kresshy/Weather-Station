# User Stories

## US001: Sliding Window Weather Data
**As a user**, I have no interest in preserving historical weather data for the chart to scroll back and forth. I want to be able to see 2, 3, 5 & 10 minute intervals, but once the data is older, it should be discarded.

## US002: Restore File Logging
**As a developer**, the latest version of the application does not produce log files. I rely on the log files received from my testers to identify issues with the application. We need to fix the file logging feature.

## US003: Wind Speed Calibration
**As a user**, I want to calibrate the wind speed in the Arduino binary. Currently the wind speed is not accurate, so we need to come up with a formula to ensure it is precise.

## US004: Initial Point Alignment & High-Accuracy Precision
**As a user**, when the first data point comes in, I want it to be displayed in the bottom corner of the chart (X:0). Additionally, I want to preserve high-accuracy values for wind and temperature down to 2 decimal places in the UI overlays.
