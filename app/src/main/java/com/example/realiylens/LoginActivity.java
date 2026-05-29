package com.example.realiylens;

import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnContinue, btnGoogle, btnRegister;
    private ImageView iv_password_toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Persistent Login Check - Redirect to WelcomeActivity if already logged in
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String savedToken = prefs.getString("access_token", null);
        if (savedToken != null && !savedToken.isEmpty()) {
            Intent intent = new Intent(LoginActivity.this, WelcomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnContinue = findViewById(R.id.btn_continue);
        btnGoogle = findViewById(R.id.btn_google);
        btnRegister = findViewById(R.id.btn_register);
        iv_password_toggle = findViewById(R.id.iv_password_toggle);

        requestAddQuickSettingsTile();

        btnContinue.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all details", Toast.LENGTH_SHORT).show();
            } else {
                // Navigate immediately to WelcomeActivity to show its skeleton and perform login
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

        btnGoogle.setOnClickListener(v -> {
            Toast.makeText(this, "Google Sign-In clicked", Toast.LENGTH_SHORT).show();
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void requestAddQuickSettingsTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            boolean tileRequested = prefs.getBoolean("tile_requested", false);

            if (!tileRequested) {
                StatusBarManager statusBarManager = getSystemService(StatusBarManager.class);
                if (statusBarManager != null) {
                    ComponentName componentName = new ComponentName(this, SnippingTileService.class);
                    Icon icon = Icon.createWithResource(this, R.drawable.icon);
                    String label = "Snip Area";

                    Executor executor = getMainExecutor();
                    statusBarManager.requestAddTileService(
                            componentName,
                            label,
                            icon,
                            executor,
                            result -> {
                                if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED || 
                                    result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                                    prefs.edit().putBoolean("tile_requested", true).apply();
                                }
                            }
                    );
                }
            }
        }
    }
}
