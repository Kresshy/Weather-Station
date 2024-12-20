package com.kresshy.weatherstation.logging;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
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

public class FileLoggingTree extends Timber.DebugTree {

    private static final String TAG = FileLoggingTree.class.getSimpleName();
    private final Context context;

    public FileLoggingTree(Context context) {
        this.context = context;
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {

        try {
            File directory =
                    new File(
                            Environment.getExternalStoragePublicDirectory(
                                            Environment.DIRECTORY_DOWNLOADS)
                                    + "/WeatherStationLogs");

            if (!directory.exists()) {
                directory.mkdir();
            }

            ArrayList<File> files = getAllFilesInDir(directory);
            deleteLogFilesOld(files);

            String fileNameTimeStamp =
                    new SimpleDateFormat("dd-MM-yyyy-HH", Locale.ENGLISH).format(new Date());

            String logTimeStamp =
                    new SimpleDateFormat("E MMM dd yyyy 'at' HH:mm:ss:SSS aaa", Locale.ENGLISH)
                            .format(new Date());

            String fileName = fileNameTimeStamp + ".html";

            File file =
                    new File(
                            Environment.getExternalStoragePublicDirectory(
                                            Environment.DIRECTORY_DOWNLOADS)
                                    + "/WeatherStationLogs"
                                    + File.separator
                                    + fileName);

            if (file.createNewFile()) {
                Timber.d("Logging file created!");
            } else {
                Timber.d("Failed to create file for logging");
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
    }

    private ArrayList<File> getAllFilesInDir(File dir) {
        if (dir == null) return null;

        ArrayList<File> files = new ArrayList<>();

        Stack<File> dirlist = new Stack<>();
        dirlist.clear();
        dirlist.push(dir);

        while (!dirlist.isEmpty()) {
            File dirCurrent = dirlist.pop();

            File[] fileList = dirCurrent.listFiles();
            for (File aFileList : fileList) {
                if (aFileList.isDirectory()) dirlist.push(aFileList);
                else files.add(aFileList);
            }
        }

        return files;
    }

    private void deleteLogFilesOld(List<File> files) {
        for (File file : files) {
            if (file.exists()) {
                Calendar time = Calendar.getInstance();
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);

                int daystokeep =
                        Integer.parseInt(sharedPreferences.getString("pref_logging_days", "-1"));
                time.add(Calendar.DAY_OF_YEAR, daystokeep);

                // I store the required attributes here and delete them
                Date lastModified = new Date(file.lastModified());
                if (lastModified.before(time.getTime())) {
                    // file is older than a week
                    file.delete();
                }
            }
        }
    }
}
