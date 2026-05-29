package com.example.realiylens;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.example.realiylens.network.MainResponseModel;
import com.example.realiylens.network.ResultResponse;
import com.example.realiylens.network.RetrofitClient;
import com.example.realiylens.network.SubmitResponse;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SnippingService extends Service {
    private static final String TAG = "SnippingService";
    private static final String CHANNEL_ID = "SnippingServiceChannel";
    private static final String RESULT_CHANNEL_ID = "ResultNotificationChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final int RESULT_NOTIFICATION_ID = 1001;
    
    private WindowManager windowManager;
    private SelectionView selectionView;
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private int mScreenWidth, mScreenHeight, mScreenDensity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int pollRetryCount = 0;
    private static final int MAX_POLL_RETRIES = 45;
    private static final int POLL_INTERVAL_MS = 4000;

    private View globalLoadingBar;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        createNotificationChannels();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        updateDisplayMetrics();
        
        Notification notification = createStatusNotification("RealityLens", "Initializing...", true);
        startForeground(NOTIFICATION_ID, notification);
    }

    private void updateDisplayMetrics() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics metrics = windowManager.getCurrentWindowMetrics();
            Rect bounds = metrics.getBounds();
            mScreenWidth = bounds.width();
            mScreenHeight = bounds.height();
            mScreenDensity = (int) (getResources().getDisplayMetrics().density * 160f);
        } else {
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getRealMetrics(metrics);
            mScreenWidth = metrics.widthPixels;
            mScreenHeight = metrics.heightPixels;
            mScreenDensity = metrics.densityDpi;
        }
    }

    private Notification createStatusNotification(String title, String content, boolean isOngoing) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.icon)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(isOngoing)
                .build();
    }

    private void updateStatusNotification(String content) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createStatusNotification("RealityLens Analysis", content, true));
        }
    }

    private void showResultNotification(String jobId, String title, String message, @Nullable Double confidence, @Nullable Double realityScore) {
        Intent intent = new Intent(this, VerificationResultActivity.class);
        intent.putExtra("job_id", jobId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        String mainText = message;
        if (confidence != null && realityScore != null) {
            mainText = String.format("Confidence: %d%% | Reality Score: %.2f", (int)(confidence * 100), realityScore);
        }

        Notification notification = new NotificationCompat.Builder(this, RESULT_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(mainText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText(mainText + "\nTap to view full analysis."))
                .setSmallIcon(R.drawable.icon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(RESULT_NOTIFICATION_ID, notification);
        }
    }

    private void setupOverlay() {
        if (selectionView != null) return;
        selectionView = new SelectionView(this);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        windowManager.addView(selectionView, params);
        selectionView.setOnSelectionListener(this::captureAndHandle);
    }

    private void captureAndHandle(RectF rect) {
        if (rect.width() < 10 || rect.height() < 10) return;
        selectionView.setVisibility(android.view.View.GONE);

        setAnalyzingState(true);

        handler.postDelayed(() -> {
            if (imageReader == null) {
                stopSelf();
                return;
            }
            Image image = null;
            try {
                image = imageReader.acquireLatestImage();
                if (image != null) {
                    Bitmap bitmap = processImage(image, rect);
                    if (bitmap != null) {
                        MainActivity.saveImageToGallery(this, bitmap);
                        submitCapturedImage(bitmap);
                    }
                    image.close();
                } else {
                    stopSelf();
                }
            } catch (Exception e) {
                stopSelf();
            }
        }, 150);
    }

    private void setAnalyzingState(boolean isAnalyzing) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("is_analyzing", isAnalyzing).apply();
        
        Intent intent = new Intent(isAnalyzing ? "com.example.realiylens.ANALYSIS_STARTED" : "com.example.realiylens.ANALYSIS_FINISHED");
        sendBroadcast(intent);

        if (isAnalyzing) {
            showGlobalLoadingBar();
        } else {
            hideGlobalLoadingBar();
        }
    }

    private void showGlobalLoadingBar() {
        handler.post(() -> {
            if (globalLoadingBar != null) return;

            Context themedContext = new ContextThemeWrapper(this, R.style.Theme_RealiyLens);
            LinearProgressIndicator indicator = new LinearProgressIndicator(themedContext);
            indicator.setIndeterminate(true);
            indicator.setIndicatorColor(getResources().getColor(R.color.link_color));
            indicator.setTrackThickness((int) (4 * getResources().getDisplayMetrics().density));
            indicator.setTrackColor(0x40FFFFFF);

            globalLoadingBar = indicator;

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                            WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.TOP;
            windowManager.addView(globalLoadingBar, params);
        });
    }

    private void hideGlobalLoadingBar() {
        handler.post(() -> {
            if (globalLoadingBar != null) {
                try {
                    windowManager.removeView(globalLoadingBar);
                } catch (Exception ignored) {}
                globalLoadingBar = null;
            }
        });
    }

    private void submitCapturedImage(Bitmap bitmap) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String token = prefs.getString("access_token", "");

        if (token.isEmpty()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            setAnalyzingState(false);
            stopSelf();
            return;
        }

        updateStatusNotification("Uploading image...");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/png"), byteArray);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", "capture.png", requestFile);
        String authHeader = "Bearer " + token;

        RetrofitClient.getApiService().submitImage(authHeader, body).enqueue(new Callback<SubmitResponse>() {
            @Override
            public void onResponse(Call<SubmitResponse> call, Response<SubmitResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String jobId = response.body().getJobId();
                    pollRetryCount = 0;
                    pollForResult(jobId, authHeader);
                } else {
                    Toast.makeText(SnippingService.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    setAnalyzingState(false);
                    stopSelf();
                }
            }

            @Override
            public void onFailure(Call<SubmitResponse> call, Throwable t) {
                Toast.makeText(SnippingService.this, "Network error", Toast.LENGTH_SHORT).show();
                setAnalyzingState(false);
                stopSelf();
            }
        });
    }

    private void pollForResult(String jobId, String authHeader) {
        updateStatusNotification("Analyzing image... (" + pollRetryCount + ")");

        RetrofitClient.getApiService().getResult(authHeader, jobId).enqueue(new Callback<MainResponseModel>() {
            @Override
            public void onResponse(Call<MainResponseModel> call, Response<MainResponseModel> response) {
                if (response.code() == 202) {
                    retryPolling(jobId, authHeader);
                } else if (response.isSuccessful() && response.body() != null) {
                    MainResponseModel responseModel = response.body();
                    
                    // Support both flat fields and nested result object
                    String verdict = responseModel.getVerdict();
                    Double confidence = responseModel.getConfidence();
                    Double realityScore = responseModel.getRealityScore();
                    
                    if (verdict == null && responseModel.getResult() != null) {
                        verdict = responseModel.getResult().getVerdict();
                    }
                    if (confidence == null && responseModel.getResult() != null) {
                        confidence = responseModel.getResult().getConfidence();
                    }
                    if (realityScore == null && responseModel.getResult() != null) {
                        realityScore = responseModel.getResult().getRealityScore();
                    }
                    
                    String verdictText = verdict != null ? verdict.toUpperCase() : "Analysis Complete";
                    showResultNotification(jobId, verdictText, "Tap to view details.", confidence, realityScore);

                    setAnalyzingState(false);
                    stopSelf();
                } else {
                    showResultNotification(jobId, "Analysis Failed", "Something went wrong during processing.", null, null);
                    setAnalyzingState(false);
                    stopSelf();
                }
            }

            @Override
            public void onFailure(Call<MainResponseModel> call, Throwable t) {
                retryPolling(jobId, authHeader);
            }
        });
    }

    private void retryPolling(String jobId, String authHeader) {
        if (pollRetryCount < MAX_POLL_RETRIES) {
            pollRetryCount++;
            handler.postDelayed(() -> pollForResult(jobId, authHeader), POLL_INTERVAL_MS);
        } else {
            showResultNotification(jobId, "Analysis Timed Out", "Process took too long.", null, null);
            setAnalyzingState(false);
            stopSelf();
        }
    }

    private Bitmap processImage(Image image, RectF rect) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * mScreenWidth;

        Bitmap bitmap = Bitmap.createBitmap(mScreenWidth + rowPadding / pixelStride, mScreenHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        try {
            int left = Math.max(0, (int) rect.left);
            int top = Math.max(0, (int) rect.top);
            int width = Math.min(bitmap.getWidth() - left, (int) rect.width());
            int height = Math.min(bitmap.getHeight() - top, (int) rect.height());
            return Bitmap.createBitmap(bitmap, left, top, width, height);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            startForeground(NOTIFICATION_ID, createStatusNotification("RealityLens", "Closing...", false));
            stopSelf();
            return START_NOT_STICKY;
        }

        int resultCode = intent.getIntExtra("resultCode", 0);
        Intent data = intent.getParcelableExtra("data");

        if (data != null) {
            Log.d(TAG, "Starting projection...");
            
            Notification notification = createStatusNotification("RealityLens Active", "Tap and drag to capture screen", true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
                } catch (Exception e) {
                    startForeground(NOTIFICATION_ID, notification);
                }
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }

            MediaProjectionManager mpManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            try {
                mediaProjection = mpManager.getMediaProjection(resultCode, data);
            } catch (Exception e) {
                stopSelf();
                return START_NOT_STICKY;
            }
            
            if (mediaProjection != null) {
                mediaProjection.registerCallback(new MediaProjection.Callback() {
                    @Override
                    public void onStop() {
                        super.onStop();
                        stopSelf();
                    }
                }, handler);

                imageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 2);
                virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                        mScreenWidth, mScreenHeight, mScreenDensity,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        imageReader.getSurface(), null, null);
                
                setupOverlay();
            } else {
                stopSelf();
            }
        }
        
        return START_NOT_STICKY;
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel serviceChannel = new NotificationChannel(
                        CHANNEL_ID, "Analysis Service", NotificationManager.IMPORTANCE_LOW);
                manager.createNotificationChannel(serviceChannel);

                NotificationChannel resultChannel = new NotificationChannel(
                        RESULT_CHANNEL_ID, "Verification Results", NotificationManager.IMPORTANCE_HIGH);
                resultChannel.setVibrationPattern(new long[]{0, 500, 200, 500});
                manager.createNotificationChannel(resultChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        setAnalyzingState(false);
        super.onDestroy();
        if (selectionView != null) {
            try { windowManager.removeView(selectionView); } catch (Exception ignored) {}
        }
        if (virtualDisplay != null) virtualDisplay.release();
        if (imageReader != null) imageReader.close();
        if (mediaProjection != null) mediaProjection.stop();
        handler.removeCallbacksAndMessages(null);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
