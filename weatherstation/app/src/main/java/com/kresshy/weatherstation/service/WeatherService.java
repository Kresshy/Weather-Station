package com.kresshy.weatherstation.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;

import com.kresshy.weatherstation.R;
import com.kresshy.weatherstation.activity.WSActivity;
import com.kresshy.weatherstation.repository.WeatherRepository;

import dagger.hilt.android.AndroidEntryPoint;

import timber.log.Timber;

import javax.inject.Inject;

/**
 * Foreground service that keeps the weather station connection active in the background. Provides a
 * persistent notification displaying live wind and temperature data.
 */
@AndroidEntryPoint
public class WeatherService extends LifecycleService {

    /** Intent action to stop the service. */
    public static final String ACTION_STOP = "com.kresshy.weatherstation.service.ACTION_STOP";

    /** Intent action to trigger a manual reconnection. */
    public static final String ACTION_RECONNECT =
            "com.kresshy.weatherstation.service.ACTION_RECONNECT";

    private static final String CHANNEL_ID = "WeatherServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    @Inject WeatherRepository weatherRepository;
    @Inject com.kresshy.weatherstation.bluetooth.WeatherConnectionController connectionController;
    @Inject NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForegroundService();

        // Observe atomic heartbeat to update notification text in real-time
        weatherRepository
                .getProcessedWeatherData()
                .observe(
                        this,
                        processedData -> {
                            if (processedData != null && processedData.getWeatherData() != null) {
                                com.kresshy.weatherstation.weather.WeatherData weatherData =
                                        processedData.getWeatherData();
                                updateNotification(
                                        String.format(
                                                java.util.Locale.getDefault(),
                                                "Temp: %.1f°C, Wind: %.1f m/s",
                                                weatherData.getTemperature(),
                                                weatherData.getWindSpeed()));
                            }
                        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Timber.d("WeatherService received action: %s", action);
            if (ACTION_STOP.equals(action)) {
                stopSelf();
                return START_NOT_STICKY;
            } else if (ACTION_RECONNECT.equals(action)) {
                connectionController.startConnection();
            }
        }

        Timber.d("WeatherService started");
        connectionController.startConnection();
        return START_STICKY;
    }

    /** Promotes the service to the foreground with a persistent notification. */
    private void startForegroundService() {
        Notification notification = createNotification(getString(R.string.connecting_message));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    /**
     * Updates the content of the active foreground notification.
     *
     * @param content The new text to display.
     */
    private void updateNotification(String content) {
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, createNotification(content));
        }
    }

    /** Creates a {@link Notification} with actions for reconnecting and stopping. */
    private Notification createNotification(String content) {
        int pendingIntentFlags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE;
        }

        Intent notificationIntent = new Intent(this, WSActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags);

        Intent stopIntent = new Intent(this, WeatherService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent =
                PendingIntent.getService(this, 0, stopIntent, pendingIntentFlags);

        Intent reconnectIntent = new Intent(this, WeatherService.class);
        reconnectIntent.setAction(ACTION_RECONNECT);
        PendingIntent reconnectPendingIntent =
                PendingIntent.getService(this, 0, reconnectIntent, pendingIntentFlags);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_stat_weather)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_rotate, "Reconnect", reconnectPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
                .build();
    }

    /** Required for Android O+ to display foreground notifications. */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Weather Station Service Channel",
                            NotificationManager.IMPORTANCE_LOW);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.d("WeatherService destroyed");
        connectionController.stopConnection();
    }
}
