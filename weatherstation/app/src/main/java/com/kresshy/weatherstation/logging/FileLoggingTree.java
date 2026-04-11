package com.kresshy.weatherstation.logging;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import timber.log.Timber;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A Timber Tree that logs messages to a local HTML file in the internal app directory. Useful for
 * debugging field tests where a computer is not available to read Logcat. Automatically rotates
 * logs based on user-defined retention settings.
 */
public class FileLoggingTree extends Timber.DebugTree {

    private static final String TAG = FileLoggingTree.class.getSimpleName();
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Initializes the logging tree with the application context. This context is used to retrieve
     * user preferences regarding log retention.
     *
     * @param context Application context for accessing SharedPreferences.
     */
    @javax.inject.Inject
    public FileLoggingTree(@dagger.hilt.android.qualifiers.ApplicationContext Context context) {
        this.context = context;
    }

    /**
     * Performs a one-time cleanup of old log files based on user preferences. This should be called
     * during application initialization.
     */
    public void cleanup() {
        executor.execute(
                () -> {
                    File directory = new File(context.getExternalFilesDir(null), "logs");
                    if (directory.exists()) {
                        ArrayList<File> files = getAllFilesInDir(directory);
                        deleteLogFilesOld(files);
                    }
                });
    }

    /** Captures a log event and appends it to an HTML file asynchronously. */
    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        executor.execute(
                () -> {
                    try {
                        File directory = new File(context.getExternalFilesDir(null), "logs");

                        if (!directory.exists()) {
                            directory.mkdirs();
                        }

                        String fileNameTimeStamp =
                                new SimpleDateFormat("dd-MM-yyyy-HH", Locale.ENGLISH)
                                        .format(new Date());

                        String logTimeStamp =
                                new SimpleDateFormat(
                                                "E MMM dd yyyy 'at' HH:mm:ss:SSS aaa",
                                                Locale.ENGLISH)
                                        .format(new Date());

                        String fileName = fileNameTimeStamp + ".html";

                        File file = new File(directory, fileName);

                        if (!file.exists()) {
                            if (file.createNewFile()) {
                                Log.d(TAG, "Logging file created: " + file.getAbsolutePath());
                            }
                        }

                        if (file.exists()) {
                            OutputStream fileOutputStream = new FileOutputStream(file, true);
                            fileOutputStream.write(
                                    ("<p style=\"background:lightgray; padding:10px;\"><strong"
                                                    + " style=\"background:lightblue;\"> "
                                                    + logTimeStamp
                                                    + " | "
                                                    + tag
                                                    + ":</strong> "
                                                    + message
                                                    + "</p>")
                                            .getBytes());
                            fileOutputStream.close();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error while logging into file : " + e);
                    }
                });
    }

    /** Recursively retrieves all files in a directory. */
    private ArrayList<File> getAllFilesInDir(File dir) {
        if (dir == null) return null;

        ArrayList<File> files = new ArrayList<>();

        Stack<File> dirlist = new Stack<>();
        dirlist.clear();
        dirlist.push(dir);

        while (!dirlist.isEmpty()) {
            File dirCurrent = dirlist.pop();

            File[] fileList = dirCurrent.listFiles();
            if (fileList == null) continue;

            for (File aFileList : fileList) {
                if (aFileList.isDirectory()) dirlist.push(aFileList);
                else files.add(aFileList);
            }
        }

        return files;
    }

    /** Deletes log files that exceed the user-defined retention period. */
    private void deleteLogFilesOld(List<File> files) {
        for (File file : files) {
            if (file.exists()) {
                Calendar time = Calendar.getInstance();
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);

                int daystokeep =
                        Integer.parseInt(sharedPreferences.getString("pref_logging_days", "-1"));

                // If days to keep is positive, apply the negative delta to the current time
                if (daystokeep > 0) {
                    time.add(Calendar.DAY_OF_YEAR, -daystokeep);

                    Date lastModified = new Date(file.lastModified());
                    if (lastModified.before(time.getTime())) {
                        file.delete();
                    }
                }
            }
        }
    }
}
