package com.example.realiylens;

import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.realiylens.network.RetrofitClient;

import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "RealityLens_Login";
    private EditText etEmail, etPassword;
    private Button btnContinue, btnGoogle, btnRegister;
    private ImageView iv_password_toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Activity started");

        // Persistent Login Check
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String savedToken = prefs.getString("access_token", null);
        if (savedToken != null && !savedToken.isEmpty()) {
            Intent intent = new Intent(LoginActivity.this, WelcomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        // Initialize views
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnContinue = findViewById(R.id.btn_continue);
        btnGoogle = findViewById(R.id.btn_google); // Matches ID in activity_login.xml
        btnRegister = findViewById(R.id.btn_register);
        iv_password_toggle = findViewById(R.id.iv_password_toggle);

        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> {
                Log.i(TAG, "Google Sign-In Button Clicked");
                Toast.makeText(LoginActivity.this, "Opening Google Sign-In...", Toast.LENGTH_SHORT).show();

                String baseUrl = RetrofitClient.BASE_URL;
                if (baseUrl == null || baseUrl.isEmpty()) {
                    Toast.makeText(this, "Configuration Error: API URL not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Construct the URL to your Python backend's Google Login endpoint
                String cleanBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
                String googleLoginUrl = cleanBase + "/login/google";
                
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(googleLoginUrl));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to open browser", e);
                    Toast.makeText(this, "Could not open browser for Sign-In", Toast.LENGTH_SHORT).show();
                }
            });
        }

        btnContinue.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all details", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(LoginActivity.this, WelcomeActivity.class);
                intent.putExtra("email", email);
                intent.putExtra("password", password);
                intent.putExtra("action", "login");
                startActivity(intent);
                finish();
            }
        });

        iv_password_toggle.setOnClickListener(v -> {
            if (etPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {
                etPassword.setTransformationMethod(null);
            } else {
                etPassword.setTransformationMethod(new PasswordTransformationMethod());
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        requestAddQuickSettingsTile();
        
        // Check if we are returning from Google Auth deep link
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        Uri data = intent.getData();
        if (data != null && "realitylens".equals(data.getScheme())) {
            // Extract the JWT token from the Python backend redirect
            String token = data.getQueryParameter("token");
            if (token != null && !token.isEmpty()) {
                // Save the JWT token exactly as your manual login flow does
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                prefs.edit().putString("access_token", token).apply();

                Toast.makeText(this, "Google Sign-In Successful", Toast.LENGTH_SHORT).show();
                
                // Redirect to WelcomeActivity to show success and skeleton
                Intent welcomeIntent = new Intent(this, WelcomeActivity.class);
                startActivity(welcomeIntent);
                finish();
            }
        }
    }

    private void requestAddQuickSettingsTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            if (!prefs.getBoolean("tile_requested", false)) {
                StatusBarManager statusBarManager = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
                if (statusBarManager != null) {
                    ComponentName componentName = new ComponentName(this, SnippingTileService.class);
                    Icon icon = Icon.createWithResource(this, R.drawable.icon);
                    statusBarManager.requestAddTileService(componentName, "Snip Area", icon, getMainExecutor(), result -> {
                        if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                            prefs.edit().putBoolean("tile_requested", true).apply();
                        }
                    });
                }
            }
        }
    }
}
