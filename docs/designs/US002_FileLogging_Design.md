# Design Document: Restore and Modernize Timber File Logging (US002)

## Status
**Draft** - March 14, 2026

## Context
The application uses **Timber 5.0.1** for logging. The `FileLoggingTree` extension, intended to save logs for field testers, is currently non-functional on modern Android devices (Android 11+).

## Current Issues
1.  **Scoped Storage Violation**: `FileLoggingTree` uses `Environment.getExternalStoragePublicDirectory`, which is restricted on SDK 35. Writing to the `Downloads` folder via the `File` API is blocked.
2.  **Logic Bug in Retention**: `pref_logging_days_to_keep_values` provides negative integers (e.g., `-1`), but `FileLoggingTree` only executes cleanup if the value is `> 0`. This effectively disables log rotation.
3.  **Synchronous I/O**: File writes and directory scans for cleanup occur on the calling thread (often the UI or the high-frequency "Single Heartbeat" thread), causing performance degradation.
4.  **Inefficient Cleanup**: The app scans the entire log directory and checks every file's age on *every single log entry*.

## Proposed Solution

### 1. Storage Strategy: Internal App Storage
Shift logging to `context.getExternalFilesDir("logs")`.
*   **Compliance**: Fully compatible with Scoped Storage.
*   **Security**: No `WRITE_EXTERNAL_STORAGE` permission required.
*   **Cleanup**: Logs are automatically removed if the app is uninstalled.

### 2. Asynchronous Logging with Executor
Implement a single-thread `ExecutorService` within `FileLoggingTree`.
*   Offload all `FileOutputStream` writes and `File` operations to a background thread.
*   Ensure thread safety for log file access.

### 3. Optimized Lifecycle & Retention Fix
*   **Initialization Cleanup**: Perform the "delete old logs" scan **only once** when the `FileLoggingTree` is initialized (on app startup).
*   **Value Correction**: Handle the negative values from `pref_logging_days` correctly (using `Math.abs()` or updating the preference values).

### 4. User Interface: "Selective Log Export" (Settings Only)
The management interface will be strictly nested within the Settings flow to avoid cluttering the main navigation.
*   **Access Point**: A new `Preference` entry in `xml/preferences.xml` labeled "Manage Log Files" will launch the management screen.
*   **Log List**: Display all `.html` files in the internal logs directory.
*   **Share Action**: Use `FileProvider` to share selected files via `Intent.ACTION_SEND_MULTIPLE`.
*   **Delete Action**: Allow manual cleanup of specific files.

## Technical Tasks (Detailed)

### 1. Refactor `FileLoggingTree.java`
**File Path**: `weatherstation/app/src/main/java/com/kresshy/weatherstation/logging/FileLoggingTree.java`

*   **Change Path**: Update the directory to use `context.getExternalFilesDir("logs")`.
*   **Asynchronous Execution**: Add a `private final ExecutorService executor = Executors.newSingleThreadExecutor();` to the class.
*   **Offload `log()`**: Wrap the entire logic inside `log()` with `executor.execute(() -> { ... });`.
*   **One-Time Cleanup**: Create a `public void cleanup()` method and call it from `WSApplication.java` during initialization instead of inside `log()`.

```java
// Logic for getting the directory
File directory = new File(context.getExternalFilesDir(null), "logs");
if (!directory.exists()) {
    directory.mkdirs();
}

// Logic for one-time cleanup (run only on startup)
public void cleanup() {
    executor.execute(() -> {
        // ... (existing deleteLogFilesOld logic moved here)
    });
}
```

### 2. Fix Preference Values
**File Path**: `weatherstation/app/src/main/res/values/arrays.xml`

*   Change the entries in `pref_logging_days_to_keep_values` from `-1`, `-2`, `-3` to `1`, `2`, `3` to ensure the `daystokeep > 0` check in `FileLoggingTree` works correctly.

### 3. Configure FileProvider for Sharing
**File Path**: `weatherstation/app/src/main/AndroidManifest.xml`
*   Add a `<provider>` tag inside the `<application>` block.

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

**File Path**: `weatherstation/app/src/main/res/xml/file_paths.xml` (Create this file)
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path name="logs" path="logs/" />
</paths>
```

### 4. Implement `LogManagerFragment.java`
**File Path**: `weatherstation/app/src/main/java/com/kresshy/weatherstation/fragment/LogManagerFragment.java`

*   Create a simple fragment with a `RecyclerView` to list files in `context.getExternalFilesDir("logs")`.
*   Implement a long-press or checkbox selection for multiple files.
*   **Share Logic**: Use `Intent.ACTION_SEND_MULTIPLE` with `FileProvider.getUriForFile()`.

```java
Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
intent.setType("text/html");
ArrayList<Uri> uris = new ArrayList<>();
for (File file : selectedFiles) {
    uris.add(FileProvider.getUriForFile(getContext(), 
        getContext().getPackageName() + ".fileprovider", file));
}
intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
startActivity(Intent.createChooser(intent, "Share Logs"));
```

### 5. Update Navigation & Settings
**File Path**: `weatherstation/app/src/main/res/xml/preferences.xml`
*   Add a clickable preference to launch the `LogManagerFragment`.

```xml
<Preference
    android:key="pref_manage_logs"
    android:title="Manage Log Files"
    android:summary="View, share, or delete stored log files" />
```

**File Path**: `weatherstation/app/src/main/java/com/kresshy/weatherstation/fragment/SettingsFragment.java`
*   Add an `onPreferenceClickListener` for `pref_manage_logs` that uses `Navigation.findNavController(...)` to navigate to the new fragment.

**File Path**: `weatherstation/app/src/main/res/navigation/nav_graph.xml`
*   Register the `LogManagerFragment` as a destination.


## Verification Plan
*   **Unit Tests**: Mock `Context` and `SharedPreferences` to verify `FileLoggingTree` behavior.
*   **Manual Test**: 
    1. Enable logging in Settings.
    2. Perform some actions.
    3. Trigger "Export Logs" and verify file content.
    4. Check performance impact during high-frequency weather updates.
