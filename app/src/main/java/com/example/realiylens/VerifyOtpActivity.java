package com.example.realiylens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.realiylens.network.LoginResponse;
import com.example.realiylens.network.RetrofitClient;
import com.example.realiylens.network.VerifyOtpRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyOtpActivity extends AppCompatActivity {

    private EditText etOtp;
    private Button btnVerify;
    private ProgressBar progressBar;
    private String tempToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        tempToken = getIntent().getStringExtra("temp_token");
        if (tempToken == null || tempToken.isEmpty()) {
            Toast.makeText(this, "Error: Missing temporary token", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etOtp = findViewById(R.id.et_otp);
        btnVerify = findViewById(R.id.btn_verify_otp);
        progressBar = findViewById(R.id.pb_otp_loading);

        btnVerify.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (otp.length() < 4) {
                Toast.makeText(this, "Please enter a valid OTP", Toast.LENGTH_SHORT).show();
            } else {
                verifyOtp(otp);
            }
        });
    }

    private void verifyOtp(String otp) {
        progressBar.setVisibility(View.VISIBLE);
        btnVerify.setEnabled(false);

        VerifyOtpRequest request = new VerifyOtpRequest(tempToken, otp);
        RetrofitClient.getApiService().verifyOtp(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnVerify.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    String finalToken = response.body().getAccessToken();
                    saveFinalToken(finalToken);
                    
                    Toast.makeText(VerifyOtpActivity.this, "OTP Verified Successfully", Toast.LENGTH_SHORT).show();
                    
                    // Proceed directly to DashboardActivity
                    Intent intent = new Intent(VerifyOtpActivity.this, DashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(VerifyOtpActivity.this, "Invalid OTP or Token expired", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnVerify.setEnabled(true);
                Toast.makeText(VerifyOtpActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveFinalToken(String token) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putString("access_token", token).apply();
    }
}
