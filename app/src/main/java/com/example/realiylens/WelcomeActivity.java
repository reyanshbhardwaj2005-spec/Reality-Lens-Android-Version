package com.example.realiylens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.realiylens.network.GoogleLoginRequest;
import com.example.realiylens.network.LoginRequest;
import com.example.realiylens.network.LoginResponse;
import com.example.realiylens.network.RegisterRequest;
import com.example.realiylens.network.RetrofitClient;
import com.example.realiylens.network.UserResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WelcomeActivity extends AppCompatActivity {

    private static final String TAG = "RealityLens_Welcome";
    private View skeleton, content;
    private TextView tvStatus;
    private boolean isGoogleLoginFlow = false;

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
        } else if ("google_login".equals(action)) {
            isGoogleLoginFlow = true;
            String idToken = getIntent().getStringExtra("id_token");
            if (idToken != null) {
                // Google ID Token Toast removed to restore original behavior
                performGoogleLogin(idToken);
            } else {
                handleError("ID Token missing from intent");
            }
        } else {
            fetchUserInfoAndShow();
        }

        Button btnOpenDashboard = findViewById(R.id.btn_open_dashboard);
        Button btnMinimize = findViewById(R.id.btn_minimize);

        if (btnOpenDashboard != null) {
            btnOpenDashboard.setOnClickListener(v -> {
                startActivity(new Intent(WelcomeActivity.this, DashboardActivity.class));
            });
        }

        if (btnMinimize != null) {
            btnMinimize.setOnClickListener(v -> {
                moveTaskToBack(true);
            });
        }
    }

    private void performGoogleLogin(String idToken) {
        GoogleLoginRequest request = new GoogleLoginRequest(idToken);
        RetrofitClient.getApiService().googleLogin(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveToken(response.body().getAccessToken());
                    fetchUserInfoAndShow();
                } else {
                    handleError("Google Auth Failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                handleError("Network error: " + t.getMessage());
            }
        });
    }

    private void performLogin(String email, String password) {
        LoginRequest loginRequest = new LoginRequest(email, password);
        RetrofitClient.getApiService().login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String tempToken = response.body().getAccessToken();
                    // Store temporary token
                    getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().putString("temp_token", tempToken).apply();
                    
                    // Redirect to OTP Verification Screen
                    Intent intent = new Intent(WelcomeActivity.this, VerifyOtpActivity.class);
                    intent.putExtra("temp_token", tempToken);
                    startActivity(intent);
                    finish();
                } else {
                    handleError("Login failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                handleError("Connection error: " + t.getMessage());
            }
        });
    }

    private void performRegister(String name, String email, String password) {
        RegisterRequest registerRequest = new RegisterRequest(name, email, password);
        RetrofitClient.getApiService().register(registerRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (isGoogleLoginFlow) {
                    isGoogleLoginFlow = false;
                    // Shadow Registration for Google users:
                    // If successful, we might get an updated token. If it fails (user exists), we continue with the Google login token.
                    if (response.isSuccessful() && response.body() != null) {
                        saveToken(response.body().getAccessToken());
                    }
                    fetchUserInfoAndShow();
                } else {
                    // Manual Registration: Redirect to OTP Verification Screen (Same as Login)
                    if (response.isSuccessful() && response.body() != null) {
                        String tempToken = response.body().getAccessToken();
                        // Store temporary token
                        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().putString("temp_token", tempToken).apply();
                        
                        Intent intent = new Intent(WelcomeActivity.this, VerifyOtpActivity.class);
                        intent.putExtra("temp_token", tempToken);
                        startActivity(intent);
                        finish();
                    } else {
                        handleError("Registration failed: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                if (isGoogleLoginFlow) {
                    isGoogleLoginFlow = false;
                    fetchUserInfoAndShow();
                } else {
                    handleError("Network error during registration: " + t.getMessage());
                }
            }
        });
    }

    private void fetchUserInfoAndShow() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String token = prefs.getString("access_token", "");
        if (token.isEmpty()) {
            handleError("Auth token missing");
            return;
        }

        String authHeader = "Bearer " + token;
        RetrofitClient.getApiService().getUserInfo(authHeader).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body();
                    if (isGoogleLoginFlow) {
                        // Shadow Registration using userId as password
                        performRegister(user.getUsername(), user.getEmail(), user.getId());
                    } else {
                        String name = user.getUsername() != null ? user.getUsername() : "User";
                        if (tvStatus != null) tvStatus.setText("Welcome back, " + name);
                        showContent();
                    }
                } else {
                    handleError("Session expired: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                handleError("Profile fetch failed: " + t.getMessage());
            }
        });
    }

    private void saveToken(String token) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putString("access_token", token).apply();
    }

    private void handleError(String message) {
        Log.e(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        
        // Clear all stored tokens to break circular redirect loops
        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                .remove("access_token")
                .remove("temp_token")
                .apply();
        
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
