package com.example.realiylens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.realiylens.network.LoginRequest;
import com.example.realiylens.network.LoginResponse;
import com.example.realiylens.network.RegisterRequest;
import com.example.realiylens.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WelcomeActivity extends AppCompatActivity {

    private View skeleton, content;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String action = getIntent().getStringExtra("action");
        if (action == null) {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            String savedToken = prefs.getString("access_token", null);
            if (savedToken == null || savedToken.isEmpty()) {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        }

        setContentView(R.layout.activity_welcome);

        skeleton = findViewById(R.id.welcome_skeleton);
        content = findViewById(R.id.welcome_content);
        tvStatus = findViewById(R.id.tv_login_status);

        skeleton.setVisibility(View.VISIBLE);
        content.setVisibility(View.GONE);

        if ("login".equals(action)) {
            String email = getIntent().getStringExtra("email");
            String password = getIntent().getStringExtra("password");
            performLogin(email, password);
        } else if ("register".equals(action)) {
            String name = getIntent().getStringExtra("name");
            String email = getIntent().getStringExtra("email");
            String password = getIntent().getStringExtra("password");
            performRegister(name, email, password);
        } else {
            showContent();
        }

        Button btnOpenDashboard = findViewById(R.id.btn_open_dashboard);
        Button btnMinimize = findViewById(R.id.btn_minimize);

        btnOpenDashboard.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, DashboardActivity.class));
        });

        btnMinimize.setOnClickListener(v -> {
            moveTaskToBack(true);
        });
    }

    private void performLogin(String email, String password) {
        LoginRequest loginRequest = new LoginRequest(email, password);
        RetrofitClient.getApiService().login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveTokenAndShow(response.body().getAccessToken(), "You are logged in successfully");
                } else {
                    handleError("Login failed");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                handleError("Network error: " + t.getMessage());
            }
        });
    }

    private void performRegister(String name, String email, String password) {
        RegisterRequest registerRequest = new RegisterRequest(name, email, password);
        RetrofitClient.getApiService().register(registerRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveTokenAndShow(response.body().getAccessToken(), "Account created successfully");
                } else {
                    handleError("Registration failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                handleError("Network error: " + t.getMessage());
            }
        });
    }

    private void saveTokenAndShow(String token, String statusText) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putString("access_token", token).apply();
        if (tvStatus != null) tvStatus.setText(statusText);
        showContent();
    }

    private void handleError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void showContent() {
        if (skeleton != null && content != null) {
            skeleton.setVisibility(View.GONE);
            content.setVisibility(View.VISIBLE);
            content.setAlpha(0f);
            content.animate().alpha(1f).setDuration(400).start();
        }
    }
}
