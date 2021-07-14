package com.example.screen_recorder3;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordingService extends Service {

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_RESUME = "ACTION_RESUME";
    public static final String RESULT_CODE = "RESULT_CODE";

    public static final String QUERY_RESULT = "QUERY_RESULT";

    private static final String TAG = "RecordingService";
    public static final String IS_RECORDING = "RECORDING_STATUS";

    public static final int NOTIFICATION_ID = 231;

    private MediaProjectionManager mediaProjectionManager;
    private NotificationManager notificationManager;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;
    private MediaProjection mediaProjection;
    private String uri;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = getSystemService(NotificationManager.class);
        mediaProjectionManager = getSystemService(MediaProjectionManager.class);
        mediaRecorder = new MediaRecorder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int result = START_STICKY;
        if (intent.getAction() == null) return result;

        Notification notification = displayNotification(this);
        startForeground(NOTIFICATION_ID, notification);

        switch (intent.getAction()) {
            case ACTION_START: {
                startScreenRecord(intent);
                updateStatus();
                break;
            }
            case ACTION_STOP: {
                stopScreenRecord();
                updateStatus();
                result = START_NOT_STICKY;
                break;
            }
            case ACTION_PAUSE: {
                pauseScreenRecord();
                break;
            }
            case ACTION_RESUME: {
                resumeScreenRecord();
                break;
            }
        }

        return result;
    }

    private void resumeScreenRecord() {
        if (mediaRecorder != null) {
            mediaRecorder.resume();
            notificationManager.notify(NOTIFICATION_ID, displayNotification(this));
        }
    }

    private void pauseScreenRecord() {
        if (mediaRecorder != null) {
            mediaRecorder.pause();
            notificationManager.notify(NOTIFICATION_ID, pauseNotification(this));
        }
    }


    private void stopScreenRecord() {
        if (virtualDisplay == null) return;
        virtualDisplay.release();
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        Toast.makeText(this, "Recording stopped", Toast.LENGTH_LONG).show();
        Intent recordingIntent = new Intent();
        recordingIntent.setAction(QUERY_RESULT);
        recordingIntent.putExtra(IS_RECORDING, false);
        recordingIntent.putExtra("URI", uri);
        sendBroadcast(recordingIntent);
        stopSelf();
    }

    private void updateStatus() {

    }

    private void initRecorder() {
        uri = getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/Record-" + new SimpleDateFormat("dd-MM-yyy-hh_mm_ss", Locale.ROOT)
                .format(new Date()) +
                ".mp4";
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoEncodingBitRate(512 * 1000);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoSize(720, 1520);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setOutputFile(uri);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "initRecorder: ", e);
        }
    }

    private void startScreenRecord(Intent intent) {
        int densityDpi = getApplicationContext().getResources().getDisplayMetrics().densityDpi;
        initRecorder();
        int result_code = intent.getIntExtra(RESULT_CODE, 0);
        mediaProjection = mediaProjectionManager.getMediaProjection(result_code, intent);
        if (mediaProjection != null) {
            mediaProjection.registerCallback(callback, null);
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    TAG, 720, 1080, densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mediaRecorder.getSurface(), null, null
            );
            mediaRecorder.start();
            Toast.makeText(this, "Recording started", Toast.LENGTH_LONG).show();
            Intent recordingIntent = new Intent();
            recordingIntent.setAction(QUERY_RESULT);
            recordingIntent.putExtra(IS_RECORDING, true);
            sendBroadcast(recordingIntent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopScreenRecord();
    }

    private final MediaProjection.Callback callback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.reset();
            }
        }
    };


    public static Notification displayNotification(Context context) {
        Intent stop = new Intent(context, RecordingService.class)
                .setAction(RecordingService.ACTION_STOP);
        PendingIntent stopIntent = PendingIntent.getService(context, 0, stop, PendingIntent.FLAG_ONE_SHOT);

        Intent pause = new Intent(context, RecordingService.class)
                .setAction(RecordingService.ACTION_PAUSE);
        PendingIntent pauseIntent = PendingIntent.getService(context, 0, pause, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, ScreenRecordApplication.channelID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Screen Recorder")
                .setContentText("Your screen is recording")
                .addAction(new NotificationCompat.Action(R.drawable.ic_baseline_pause_24, "PAUSE", pauseIntent))
                .addAction(new NotificationCompat.Action(R.drawable.ic_baseline_stop_24, "STOP", stopIntent));
        return notificationBuilder.build();
    }

    public static Notification pauseNotification(Context context) {
        Intent stop = new Intent(context, RecordingService.class)
                .setAction(RecordingService.ACTION_STOP);
        PendingIntent stopIntent = PendingIntent.getService(context, 0, stop, PendingIntent.FLAG_ONE_SHOT);

        Intent resume = new Intent(context, RecordingService.class)
                .setAction(RecordingService.ACTION_RESUME);
        PendingIntent resumeIntent = PendingIntent.getService(context, 0, resume, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, ScreenRecordApplication.channelID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Screen Recorder")
                .setContentText("Screen recording is paused")
                .addAction(new NotificationCompat.Action(R.drawable.ic_baseline_play_arrow_24, "RESUME", resumeIntent))
                .addAction(new NotificationCompat.Action(R.drawable.ic_baseline_stop_24, "STOP", stopIntent));
        return notificationBuilder.build();
    }

}
