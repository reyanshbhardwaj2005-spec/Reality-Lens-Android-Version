package com.example.realiylens;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.realiylens.network.MainResponseModel;
import com.example.realiylens.network.ResultResponse;
import com.example.realiylens.network.RetrofitClient;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerificationResultActivity extends AppCompatActivity {

    private TextView tvVerdict, tvConfidence, tvRealityScore, tvExplanation, tvClaim;
    private LinearProgressIndicator pbConfidence, pbRealityScore;
    private LinearLayout llEvidenceContainer;
    private ImageView ivResultImage;
    private LinearProgressIndicator progressBar;
    private String jobId;
    private String currentImageUrl;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private int retryCount = 0;
    
    private static final int MAX_RETRIES = 45; 
    private static final int POLL_INTERVAL = 4000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_result);

        // Initialize views
        tvVerdict = findViewById(R.id.tv_verdict);
        tvClaim = findViewById(R.id.tv_claim);
        ivResultImage = findViewById(R.id.iv_result_image);
        tvConfidence = findViewById(R.id.tv_confidence);
        tvRealityScore = findViewById(R.id.tv_reality_score);
        pbConfidence = findViewById(R.id.pb_confidence);
        pbRealityScore = findViewById(R.id.pb_reality_score);
        tvExplanation = findViewById(R.id.tv_explanation);
        llEvidenceContainer = findViewById(R.id.ll_evidence_container);
        progressBar = findViewById(R.id.loading_progress);
        Button btnBack = findViewById(R.id.btn_back_dashboard);

        jobId = getIntent().getStringExtra("job_id");

        if (jobId != null && !jobId.isEmpty()) {
            fetchResult();
        } else {
            Toast.makeText(this, "Job ID not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnBack.setOnClickListener(v -> finish());

        ivResultImage.setOnClickListener(v -> {
            if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                showEnlargedImage(currentImageUrl);
            }
        });
    }

    private void showEnlargedImage(String imageUrl) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_enlarged_image);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        ImageView ivEnlarged = dialog.findViewById(R.id.iv_enlarged_image);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close_dialog);

        loadImageIntoView(imageUrl, ivEnlarged);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        ivEnlarged.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void fetchResult() {
        progressBar.show();
        
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String token = prefs.getString("access_token", "");
        String authHeader = "Bearer " + token;

        RetrofitClient.getApiService().getResult(authHeader, jobId).enqueue(new Callback<MainResponseModel>() {
            @Override
            public void onResponse(Call<MainResponseModel> call, Response<MainResponseModel> response) {
                if (response.code() == 202) {
                    handleRetry();
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    progressBar.hide();
                    displayData(response.body());
                } else {
                    handleFailure("Server Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<MainResponseModel> call, Throwable t) {
                handleFailure(t.getMessage());
            }
        });
    }

    private void handleRetry() {
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            pollHandler.postDelayed(this::fetchResult, POLL_INTERVAL);
        } else {
            progressBar.hide();
            Toast.makeText(this, "Analysis timed out. Please check later.", Toast.LENGTH_LONG).show();
        }
    }

    private void handleFailure(String error) {
        if (retryCount < MAX_RETRIES) {
            handleRetry();
        } else {
            progressBar.hide();
            Toast.makeText(this, "Failed to load result", Toast.LENGTH_LONG).show();
        }
    }

    private void displayData(MainResponseModel responseModel) {
        String verdict = responseModel.getVerdict();
        String claim = responseModel.getClaim();
        Double confidence = responseModel.getConfidence();
        Double realityScore = responseModel.getRealityScore();
        String explanation = responseModel.getExplanation();
        List<ResultResponse.EvidenceItem> evidenceItems = responseModel.getEvidence();
        currentImageUrl = responseModel.getImageUrl();

        // Robust fallback for older records
        boolean isImageUrlInvalid = currentImageUrl == null || currentImageUrl.isEmpty() || 
                                   currentImageUrl.equalsIgnoreCase("null") || 
                                   currentImageUrl.equalsIgnoreCase("undefined");

        if (responseModel.getResult() != null) {
            if (verdict == null) verdict = responseModel.getResult().getVerdict();
            if (claim == null) claim = responseModel.getResult().getClaim();
            if (confidence == null) confidence = responseModel.getResult().getConfidence();
            if (realityScore == null) realityScore = responseModel.getResult().getRealityScore();
            if (explanation == null) explanation = responseModel.getResult().getExplanation();
            if (evidenceItems == null) evidenceItems = responseModel.getResult().getEvidence();
            if (isImageUrlInvalid) {
                currentImageUrl = responseModel.getResult().getImageUrl();
            }
        }

        if (verdict != null) {
            tvVerdict.setText(verdict.toUpperCase());
            applyVerdictColor(verdict);
        } else {
            tvVerdict.setText("ANALYSIS COMPLETE");
            tvVerdict.setTextColor(ContextCompat.getColor(this, R.color.white));
        }

        if (tvClaim != null) {
            tvClaim.setText(claim != null ? claim : "No captured claim available");
        }
        
        if (confidence != null) {
            int confValue = (int)(confidence * 100);
            tvConfidence.setText(confValue + "%");
            if (pbConfidence != null) {
                pbConfidence.setProgress(confValue, true);
            }
        } else {
            tvConfidence.setText("N/A");
            if (pbConfidence != null) pbConfidence.setProgress(0);
        }
        
        if (realityScore != null) {
            int realityPercent = (int)(realityScore * 100);
            tvRealityScore.setText(realityPercent + "%");
            if (pbRealityScore != null) {
                pbRealityScore.setProgress(realityPercent, true);
            }
        } else {
            tvRealityScore.setText("N/A");
            if (pbRealityScore != null) pbRealityScore.setProgress(0);
        }
        
        tvExplanation.setText(explanation != null ? explanation : "No explanation available.");
        
        llEvidenceContainer.removeAllViews();
        if (evidenceItems != null && !evidenceItems.isEmpty()) {
            LayoutInflater inflater = LayoutInflater.from(this);
            for (ResultResponse.EvidenceItem item : evidenceItems) {
                View itemView = inflater.inflate(R.layout.item_evidence, llEvidenceContainer, false);
                TextView tvTitle = itemView.findViewById(R.id.tv_evidence_title);
                TextView tvSource = itemView.findViewById(R.id.tv_evidence_source);
                tvTitle.setText(item.getTitle() != null ? item.getTitle() : "No Title");
                tvSource.setText("Source: " + (item.getSource() != null ? item.getSource() : "Unknown"));
                itemView.setOnClickListener(v -> {
                    if (item.getUrl() != null && !item.getUrl().isEmpty()) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.getUrl())));
                    } else {
                        Toast.makeText(this, "Link not available", Toast.LENGTH_SHORT).show();
                    }
                });
                llEvidenceContainer.addView(itemView);
            }
        } else {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No evidence found.");
            tvEmpty.setTextColor(ContextCompat.getColor(this, R.color.white));
            llEvidenceContainer.addView(tvEmpty);
        }

        loadImageIntoView(currentImageUrl, ivResultImage);
    }

    private void loadImageIntoView(String imageUrl, ImageView imageView) {
        // Handle Base64 images
        if (imageUrl != null && imageUrl.startsWith("data:image")) {
            try {
                String base64Data = imageUrl.substring(imageUrl.indexOf(",") + 1);
                byte[] decodedString = Base64.decode(base64Data, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                imageView.setImageBitmap(decodedByte);
            } catch (Exception e) {
                imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            }
            return;
        }

        String fullImageUrl = RetrofitClient.getFullImageUrl(imageUrl);
        if (fullImageUrl != null && !fullImageUrl.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            String token = prefs.getString("access_token", "");
            
            Object loadTarget = fullImageUrl;
            if (!token.isEmpty()) {
                loadTarget = new GlideUrl(fullImageUrl, new LazyHeaders.Builder()
                        .addHeader("Authorization", "Bearer " + token)
                        .build());
            }

            Glide.with(this)
                    .load(loadTarget)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(imageView);
        } else if (jobId != null) {
            // Fallback for older records: try uploads/jobId.png
            String fallbackUrl = RetrofitClient.getFullImageUrl("uploads/" + jobId + ".png");
            Glide.with(this)
                    .load(fallbackUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(imageView);
        }
    }

    private void applyVerdictColor(String verdict) {
        if (verdict == null) return;
        int colorRes = R.color.white;
        String v = verdict.toUpperCase();
        if (v.contains("LIKELY REAL") || v.equals("REAL")) colorRes = R.color.verdict_real;
        else if (v.contains("LIKELY FAKE") || v.equals("FAKE")) colorRes = R.color.verdict_fake;
        else if (v.contains("SUSPICIOUS")) colorRes = R.color.verdict_suspicious;
        else if (v.contains("UNVERIFIED")) colorRes = R.color.verdict_unverified;
        else if (v.contains("UNREADABLE")) colorRes = R.color.verdict_unreadable;
        else if (v.contains("SATIRE")) colorRes = R.color.verdict_satire;
        int color = ContextCompat.getColor(this, colorRes);
        tvVerdict.setTextColor(color);
        if (pbRealityScore != null) pbRealityScore.setIndicatorColor(color);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pollHandler.removeCallbacksAndMessages(null);
    }
}
