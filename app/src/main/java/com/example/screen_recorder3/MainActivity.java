package com.example.screen_recorder3;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.screen_recorder3.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 1001;

    private static final SparseIntArray ORIENTATION = new SparseIntArray();
    private MediaProjectionManager mediaprojectionmanager;
    private static final String TAG = "MainActivity";

    private final String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };

    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    private ActivityMainBinding binding;

    private final ActivityResultLauncher<Intent> screenCapture = registerForActivityResult(new StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            Intent intent = new Intent(result.getData());
            intent.setAction(RecordingService.ACTION_START);
            intent.putExtra(RecordingService.RESULT_CODE, result.getResultCode());
            intent.setClass(this, RecordingService.class);
            ContextCompat.startForegroundService(this, intent);
        }
    });

    private Boolean isRecording;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        isRecording = false;

        mediaprojectionmanager = getSystemService(MediaProjectionManager.class);

        binding.button.setOnClickListener(v -> requestPermission(() -> {
            if (isRecording) {
                Intent intent = new Intent(this, RecordingService.class);
                intent.setAction(RecordingService.ACTION_STOP);
                startService(intent);
            } else
                screenCapture.launch(mediaprojectionmanager.createScreenCaptureIntent());

        }));

    }


    private void requestPermission(PermissionCallback callback) {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) +
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            callback.onGranted();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO)) {
                Snackbar.make(binding.getRoot(), "Permission denied", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Enable", v1 -> openSettings()).show();
            } else requestPermissions(permissions, REQUEST_PERMISSION);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && (grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                screenCapture.launch(mediaprojectionmanager.createScreenCaptureIntent());
            } else {
                Snackbar.make(binding.getRoot(), "Permission", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Enable", v -> requestPermissions(permissions, REQUEST_PERMISSION))
                        .show();
            }
        }
    }

    private final BroadcastReceiver recordingStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            isRecording = intent.getBooleanExtra(RecordingService.IS_RECORDING, false);
            String uri = intent.getStringExtra("URI");
            Log.d(TAG, "onReceive: received data is " + isRecording);
            if (isRecording) {
                binding.button.setText(R.string.stop);
            } else {
                binding.button.setText(R.string.start);
                // Send intent to stop the service
                if (uri != null)
                    playVideo(uri);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (isRecording)
            binding.button.setText(R.string.stop);
    }

    // Executes when video recording finishes
    private void playVideo(String path) {
        binding.videoView.setVideoURI(Uri.fromFile(new File(path)));
        binding.videoView.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(recordingStatusReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RecordingService.QUERY_RESULT);
        registerReceiver(recordingStatusReceiver, intentFilter);
    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    interface PermissionCallback {
        void onGranted();
    }
}