package com.example.realiylens;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import com.example.realiylens.network.LoginResponse;
import com.example.realiylens.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String PREF_DARK_MODE = "dark_mode";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        LinearLayout btnBack = findViewById(R.id.ll_back_dashboard);
        CardView btnLogout = findViewById(R.id.cv_logout);
        CardView btnDeleteAccount = findViewById(R.id.cv_delete_account);
        Switch switchDarkMode = findViewById(R.id.switchDarkMode);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(PREF_DARK_MODE, true); 
        switchDarkMode.setChecked(isDarkMode);

        btnBack.setOnClickListener(v -> finish());

        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

        btnDeleteAccount.setOnClickListener(v -> initiateAccountDeletion());

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                prefs.edit().putBoolean(PREF_DARK_MODE, true).apply();
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                prefs.edit().putBoolean(PREF_DARK_MODE, false).apply();
            }
        });
    }

    private void showLogoutConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void initiateAccountDeletion() {
        String token = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString("access_token", "");
        String authHeader = "Bearer " + token;

        // 1. Hit the 'delete-account' endpoint using DELETE method
        RetrofitClient.getApiService().deleteAccount(authHeader).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 2. Server sends a temporary access_token
                    String tempDeleteToken = response.body().getAccessToken();
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                            .putLong("otp_timestamp", System.currentTimeMillis())
                            .apply();
                    showDeleteAccountDialog(tempDeleteToken);
                } else {
                    Toast.makeText(SettingsActivity.this, "Failed to initiate deletion. Try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(SettingsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteAccountDialog(String tempDeleteToken) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_delete_account);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText etOtp = dialog.findViewById(R.id.et_deletion_otp);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btn_confirm_delete);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel_deletion);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close_dialog);

        btnConfirm.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (otp.isEmpty()) {
                Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            long otpTimestamp = prefs.getLong("otp_timestamp", 0);
            if (System.currentTimeMillis() - otpTimestamp > 120000) { // 2 minutes
                Toast.makeText(this, "Session Expired. Send the OTP again.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. Verify deletion with temp token and OTP
            verifyDeletion(tempDeleteToken, otp, dialog);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void verifyDeletion(String tempDeleteToken, String otp, Dialog dialog) {
        String token = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString("access_token", "");
        String authHeader = "Bearer " + token;

        Map<String, String> body = new HashMap<>();
        body.put("token", tempDeleteToken);
        body.put("otp", otp);

        // 4. Send to 'verify-delete' using POST method
        RetrofitClient.getApiService().verifyDelete(authHeader, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    dialog.dismiss();
                    Toast.makeText(SettingsActivity.this, "Account deleted successfully", Toast.LENGTH_LONG).show();
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().remove("otp_timestamp").apply();
                    logout(); // Clear session and exit
                } else {
                    Toast.makeText(SettingsActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(SettingsActivity.this, "Verification error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logout() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .remove("access_token")
                .remove("is_google_login")
                .apply();
        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
