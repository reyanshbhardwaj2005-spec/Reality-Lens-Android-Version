package com.example.realiylens;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_REQ_CODE = 101;
    private static final int MEDIA_PROJECTION_REQUEST_CODE = 102;
    private static final int NOTIFICATION_PERMISSION_REQ_CODE = 103;
    private static final String PREFS_NAME = "SnippingPrefs";
    private static final String PREF_TILE_REQUESTED = "tile_requested";
    
    private MediaProjectionManager projectionManager;

    private final BroadcastReceiver finishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.realiylens.FINISH_ACTIVITY".equals(intent.getAction())) {
                finishAffinity();
            }
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize projection manager early
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        IntentFilter filter = new IntentFilter("com.example.realiylens.FINISH_ACTIVITY");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(finishReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(finishReceiver, filter);
        }

        // Check if we should start snipping immediately (from Tile)
        boolean startSnip = getIntent().getBooleanExtra("START_SNIP", false);
        
        if (startSnip) {
            // Don't set content view, stay transparent
            checkPermissionsAndStart();
        } else {
            // Normal launch, show UI
            setContentView(R.layout.activity_main);
            Button startBtn = findViewById(R.id.btn_start_snipping);
            startBtn.setOnClickListener(v -> checkPermissionsAndStart());
        }

        // Request to add Tile on first launch (Android 13+)
        requestAddTileOnce();
    }

    private void requestAddTileOnce() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            if (!prefs.getBoolean(PREF_TILE_REQUESTED, false)) {
                StatusBarManager statusBarManager = getSystemService(StatusBarManager.class);
                ComponentName componentName = new ComponentName(this, SnippingTileService.class);
                
                if (statusBarManager != null) {
                    statusBarManager.requestAddTileService(
                            componentName,
                            "Snip Area",
                            Icon.createWithResource(this, R.drawable.icon),
                            getMainExecutor(),
                            result -> {
                                prefs.edit().putBoolean(PREF_TILE_REQUESTED, true).apply();
                            }
                    );
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra("START_SNIP", false)) {
            checkPermissionsAndStart();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(finishReceiver);
        } catch (Exception e) {
            // Ignore
        }
    }

    private void checkPermissionsAndStart() {
        // First check for basic permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQ_CODE);
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // Show toast so user knows why the app opened
            Toast.makeText(this, "Overlay permission required for snipping", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
        } else {
            requestMediaProjection();
        }
    }

    private void requestMediaProjection() {
        if (projectionManager != null) {
            startActivityForResult(projectionManager.createScreenCaptureIntent(), MEDIA_PROJECTION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkPermissionsAndStart();
            } else {
                Toast.makeText(this, "Notification permission needed to show capture status", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                requestMediaProjection();
            }
        } else if (requestCode == MEDIA_PROJECTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Intent serviceIntent = new Intent(this, SnippingService.class);
                serviceIntent.putExtra("resultCode", resultCode);
                serviceIntent.putExtra("data", data);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                moveTaskToBack(true);
            } else {
                // If it was a tile launch and user canceled, we should probably close
                if (getIntent().getBooleanExtra("START_SNIP", false)) {
                    finish();
                }
            }
        }
    }

    public static void saveImageToGallery(Context context, Bitmap bitmap) {
        String filename = "Snip_" + System.currentTimeMillis() + ".png";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
                Uri imageUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri != null) {
                    try (OutputStream fos = context.getContentResolver().openOutputStream(imageUri)) {
                        if (fos != null) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                            Toast.makeText(context, "Image has been sent to server. Please wait for the result.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                java.io.File image = new java.io.File(imagesDir, filename);
                try (OutputStream fos = new java.io.FileOutputStream(image)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    Toast.makeText(context, "Image has been sent to server. Please wait for the result.", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show();
        }
    }
}