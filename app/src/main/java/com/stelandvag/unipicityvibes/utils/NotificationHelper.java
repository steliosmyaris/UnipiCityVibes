package com.stelandvag.unipicityvibes.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.stelandvag.unipicityvibes.R;
import com.stelandvag.unipicityvibes.activities.EventDetailActivity;
import com.stelandvag.unipicityvibes.models.Event;

import java.util.HashSet;
import java.util.Set;

public class NotificationHelper {

    private static final String CHANNEL_ID = "nearby_events";
    private static final String CHANNEL_NAME = "Nearby Events";
    private static final String PREFS_NOTIFIED_EVENTS = "notified_events";

    private Context context;
    private NotificationManager notificationManager;
    private SharedPreferences prefs;
    private Set<String> notifiedEventIds;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);

        // Load already notified events (to avoid spamming)
        this.notifiedEventIds = new HashSet<>(prefs.getStringSet(PREFS_NOTIFIED_EVENTS, new HashSet<>()));

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for nearby events");
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void showNearbyEventNotification(Event event, float distanceMeters) {
        // Check if notifications are enabled
        boolean notificationsEnabled = prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLED, true);
        if (!notificationsEnabled) {
            return;
        }

        // Check if we already notified about this event
        if (notifiedEventIds.contains(event.getEventId())) {
            return;
        }

        // Create intent to open event details
        Intent intent = new Intent(context, EventDetailActivity.class);
        intent.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.getEventId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                event.getEventId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        String distanceText = distanceMeters < 1000
                ? String.format("%.0f meters away", distanceMeters)
                : String.format("%.1f km away", distanceMeters / 1000);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("📍 " + event.getTitle())
                .setContentText("Happening nearby! " + distanceText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(event.getVenue() + "\n" + distanceText + "\nTap to view details"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show notification
        notificationManager.notify(event.getEventId().hashCode(), builder.build());

        // Mark as notified
        notifiedEventIds.add(event.getEventId());
        prefs.edit().putStringSet(PREFS_NOTIFIED_EVENTS, notifiedEventIds).apply();
    }

    public void clearNotifiedEvents() {
        notifiedEventIds.clear();
        prefs.edit().remove(PREFS_NOTIFIED_EVENTS).apply();
    }
}