package com.example.realiylens;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

        // Fixed image loading with full URL
        String fullUrl = RetrofitClient.getFullImageUrl(imageUrl);
        Glide.with(this)
                .load(fullUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(ivEnlarged);

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
        // Fetch data from flat fields (new API structure)
        String verdict = responseModel.getVerdict();
        String claim = responseModel.getClaim();
        Double confidence = responseModel.getConfidence();
        Double realityScore = responseModel.getRealityScore();
        String explanation = responseModel.getExplanation();
        List<ResultResponse.EvidenceItem> evidenceItems = responseModel.getEvidence();
        currentImageUrl = responseModel.getImageUrl();

        // Fallback to nested result object if flat fields are null (backward compatibility)
        if (verdict == null && responseModel.getResult() != null) {
            verdict = responseModel.getResult().getVerdict();
        }
        if (claim == null && responseModel.getResult() != null) {
            claim = responseModel.getResult().getClaim();
        }
        if (confidence == null && responseModel.getResult() != null) {
            confidence = responseModel.getResult().getConfidence();
        }
        if (realityScore == null && responseModel.getResult() != null) {
            realityScore = responseModel.getResult().getRealityScore();
        }
        if (explanation == null && responseModel.getResult() != null) {
            explanation = responseModel.getResult().getExplanation();
        }
        if (evidenceItems == null && responseModel.getResult() != null) {
            evidenceItems = responseModel.getResult().getEvidence();
        }

        // Set Verdict with dynamic color
        if (verdict != null) {
            tvVerdict.setText(verdict.toUpperCase());
            applyVerdictColor(verdict);
        } else {
            tvVerdict.setText("ANALYSIS COMPLETE");
            tvVerdict.setTextColor(ContextCompat.getColor(this, R.color.white));
        }

        // Set Claim
        if (tvClaim != null) {
            tvClaim.setText(claim != null ? claim : "No captured claim available");
        }
        
        // Set Confidence
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
        
        // Set Reality Score as percentage
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
        
        // Set Explanation
        tvExplanation.setText(explanation != null ? explanation : "No explanation available.");
        
        // Populate Evidence List
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
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getUrl()));
                        startActivity(browserIntent);
                    } else {
                        Toast.makeText(this, "Link not available", Toast.LENGTH_SHORT).show();
                    }
                });

                llEvidenceContainer.addView(itemView);
            }
        } else {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No evidence found.");
            try {
                tvEmpty.setTextColor(ContextCompat.getColor(this, R.color.white));
            } catch (Exception e) {
                // Ignore
            }
            llEvidenceContainer.addView(tvEmpty);
        }

        // Load Result Image - Fixed with full URL
        String fullImageUrl = RetrofitClient.getFullImageUrl(currentImageUrl);
        if (fullImageUrl != null) {
            Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(ivResultImage);
        }
    }

    private void applyVerdictColor(String verdict) {
        if (verdict == null) return;
        int colorRes = R.color.white;
        String v = verdict.toUpperCase();
        
        if (v.contains("LIKELY REAL") || v.equals("REAL")) {
            colorRes = R.color.verdict_real;
        } else if (v.contains("LIKELY FAKE") || v.equals("FAKE")) {
            colorRes = R.color.verdict_fake;
        } else if (v.contains("SUSPICIOUS")) {
            colorRes = R.color.verdict_suspicious;
        } else if (v.contains("UNVERIFIED")) {
            colorRes = R.color.verdict_unverified;
        } else if (v.contains("UNREADABLE")) {
            colorRes = R.color.verdict_unreadable;
        } else if (v.contains("SATIRE")) {
            colorRes = R.color.verdict_satire;
        }
        
        int color = ContextCompat.getColor(this, colorRes);
        tvVerdict.setTextColor(color);
        if (pbRealityScore != null) {
            pbRealityScore.setIndicatorColor(color);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pollHandler.removeCallbacksAndMessages(null);
    }
}
