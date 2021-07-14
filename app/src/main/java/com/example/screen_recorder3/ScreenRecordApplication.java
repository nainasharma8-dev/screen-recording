package com.example.screen_recorder3;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationManagerCompat;

public class ScreenRecordApplication  extends Application {

    public static final String channelID = "screen_recording";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(this);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(channelID, "Screen recording", NotificationManager.IMPORTANCE_HIGH);
        NotificationManagerCompat.from(context).createNotificationChannel(channel);
    }
}
