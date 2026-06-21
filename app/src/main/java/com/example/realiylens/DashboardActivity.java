package com.example.realiylens;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.realiylens.network.LoginResponse;
import com.example.realiylens.network.MainResponseModel;
import com.example.realiylens.network.RetrofitClient;
import com.example.realiylens.network.SubmitResponse;
import com.example.realiylens.network.TextSubmitRequest;
import com.example.realiylens.network.UserResponse;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "RealityLens_Dashboard";
    private static final Gson gson = new Gson();
    
    private DrawerLayout drawerLayout;
    private RecyclerView rvVerifications;
    private HistoryAdapter historyAdapter;
    private LinearLayout llSkeletonContainer, llEmptyState;
    private TextView tvUserUsername, tvUserEmail, tvWelcomeUser, tvAvatarLetter;
    private TextView tvToggleFile, tvToggleText;
    private View clUploadArea;
    private LinearLayout llUploadPlaceholder, llSelectedPreviewContainer;
    private ImageView ivSelectedPreview;
    private EditText etTextInput;
    private MaterialButton btnSendVerification, btnBrowse;
    private boolean isTextMode = false;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private Uri selectedImageUri;
    private boolean isFirstLoad = true;

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.realiylens.ANALYSIS_FINISHED".equals(intent.getAction())) {
                fetchHistory();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        drawerLayout = findViewById(R.id.drawer_layout);
        rvVerifications = findViewById(R.id.rv_verifications);
        llSkeletonContainer = findViewById(R.id.ll_skeleton_container);
        llEmptyState = findViewById(R.id.ll_empty_state);
        tvWelcomeUser = findViewById(R.id.tv_welcome_user);
        
        tvToggleFile = findViewById(R.id.tv_toggle_file);
        tvToggleText = findViewById(R.id.tv_toggle_text);
        clUploadArea = findViewById(R.id.cl_upload_area);
        llUploadPlaceholder = findViewById(R.id.ll_upload_placeholder);
        llSelectedPreviewContainer = findViewById(R.id.ll_selected_preview_container);
        ivSelectedPreview = findViewById(R.id.iv_selected_preview);
        etTextInput = findViewById(R.id.et_text_input);
        btnSendVerification = findViewById(R.id.btn_send_verification);
        btnBrowse = findViewById(R.id.btn_browse);
        MaterialButton btnMinimize = findViewById(R.id.btn_minimize_tray);
        ImageButton btnHamburger = findViewById(R.id.btn_hamburger_menu);
        
        NavigationView navigationView = findViewById(R.id.nav_view_sidebar);
        View headerView = navigationView.getHeaderView(0);
        tvUserUsername = headerView.findViewById(R.id.tv_user_username);
        tvUserEmail = headerView.findViewById(R.id.tv_user_email);
        tvAvatarLetter = headerView.findViewById(R.id.tv_avatar_letter);

        rvVerifications.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(this);
        rvVerifications.setAdapter(historyAdapter);

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                showImagePreview(uri);
            }
        });

        if (btnBrowse != null) btnBrowse.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        if (btnHamburger != null) btnHamburger.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        tvToggleFile.setOnClickListener(v -> switchMode(false));
        tvToggleText.setOnClickListener(v -> switchMode(true));
        btnMinimize.setOnClickListener(v -> moveTaskToBack(true));

        btnSendVerification.setOnClickListener(v -> handleVerification());
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetInputs());
        
        if (llSelectedPreviewContainer != null) {
            llSelectedPreviewContainer.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawer(GravityCompat.START);
            
            drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                @Override
                public void onDrawerClosed(View drawerView) {
                    super.onDrawerClosed(drawerView);
                    drawerLayout.removeDrawerListener(this);
                    
                    if (id == R.id.nav_logout) {
                        showLogoutConfirmationDialog();
                    } else if (id == R.id.nav_history) {
                        startActivity(new Intent(DashboardActivity.this, HistoryActivity.class));
                    } else if (id == R.id.nav_settings) {
                        startActivity(new Intent(DashboardActivity.this, SettingsActivity.class));
                    } else if (id == R.id.nav_about_us) {
                        startActivity(new Intent(DashboardActivity.this, AboutUsActivity.class));
                    } else if (id == R.id.nav_profile) {
                        showEditProfileDialog();
                    }
                }
            });
            return true;
        });

        registerAnalysisReceiver();
        fetchUserInfo();
    }

    private void showLogoutConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showImagePreview(Uri uri) {
        if (ivSelectedPreview != null && llUploadPlaceholder != null && llSelectedPreviewContainer != null) {
            ivSelectedPreview.setImageURI(uri);
            llSelectedPreviewContainer.setVisibility(View.VISIBLE);
            llUploadPlaceholder.setVisibility(View.GONE);
        }
    }

    private void showEditProfileDialog() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isGoogleLogin = prefs.getBoolean("is_google_login", false);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_edit_profile);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvModeUsername = dialog.findViewById(R.id.tv_mode_username);
        TextView tvModePassword = dialog.findViewById(R.id.tv_mode_password);
        LinearLayout llUsernameSection = dialog.findViewById(R.id.ll_username_section);
        LinearLayout llPasswordSection = dialog.findViewById(R.id.ll_password_section);
        EditText etNewUsername = dialog.findViewById(R.id.et_new_username);
        EditText etNewPassword = dialog.findViewById(R.id.et_new_password);
        ImageView ivPasswordToggle = dialog.findViewById(R.id.iv_password_toggle);
        EditText etOtpInput = dialog.findViewById(R.id.et_otp_input);
        MaterialButton btnSendOtp = dialog.findViewById(R.id.btn_send_otp);
        MaterialButton btnSave = dialog.findViewById(R.id.btn_save_changes);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close_dialog);

        final boolean[] isPasswordMode = {false};

        if (isGoogleLogin) {
            tvModePassword.setVisibility(View.GONE);
            tvModeUsername.setBackgroundResource(R.drawable.btn_gradient_simple);
            tvModeUsername.setTextColor(Color.WHITE);
        }

        if (tvUserUsername != null) {
            etNewUsername.setText(tvUserUsername.getText().toString());
        }

        tvModeUsername.setOnClickListener(v -> {
            isPasswordMode[0] = false;
            tvModeUsername.setBackgroundResource(R.drawable.btn_gradient_simple);
            tvModeUsername.setTextColor(Color.WHITE);
            tvModePassword.setBackground(null);
            tvModePassword.setTextColor(Color.parseColor("#B0BEC5"));
            llUsernameSection.setVisibility(View.VISIBLE);
            llPasswordSection.setVisibility(View.GONE);
        });

        tvModePassword.setOnClickListener(v -> {
            if (isGoogleLogin) return;
            isPasswordMode[0] = true;
            tvModePassword.setBackgroundResource(R.drawable.btn_gradient_simple);
            tvModePassword.setTextColor(Color.WHITE);
            tvModeUsername.setBackground(null);
            tvModeUsername.setTextColor(Color.parseColor("#B0BEC5"));
            llPasswordSection.setVisibility(View.VISIBLE);
            llUsernameSection.setVisibility(View.GONE);
        });

        if (ivPasswordToggle != null) {
            ivPasswordToggle.setOnClickListener(v -> {
                if (etNewPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {
                    etNewPassword.setTransformationMethod(null);
                } else {
                    etNewPassword.setTransformationMethod(new PasswordTransformationMethod());
                }
                etNewPassword.setSelection(etNewPassword.getText().length());
            });
        }

        btnSendOtp.setOnClickListener(v -> {
            String password = etNewPassword.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(this, "Enter new password first", Toast.LENGTH_SHORT).show();
                return;
            }
            sendPasswordChangeRequest(password, btnSendOtp);
        });

        btnSave.setOnClickListener(v -> {
            if (isPasswordMode[0]) {
                String password = etNewPassword.getText().toString().trim();
                String otp = etOtpInput.getText().toString().trim();
                String tempToken = prefs.getString("temp_password_token", "");

                if (password.isEmpty() || otp.isEmpty() || tempToken.isEmpty()) {
                    Toast.makeText(this, "Please enter password, send OTP and enter it", Toast.LENGTH_SHORT).show();
                    return;
                }

                long otpTimestamp = prefs.getLong("otp_timestamp", 0);
                if (System.currentTimeMillis() - otpTimestamp > 120000) { // 2 minutes
                    Toast.makeText(this, "Session Expired. Send the OTP again.", Toast.LENGTH_SHORT).show();
                    return;
                }

                verifyPasswordUpdate(tempToken, otp, dialog);
            } else {
                String username = etNewUsername.getText().toString().trim();
                if (username.isEmpty()) {
                    Toast.makeText(this, "Please enter new username", Toast.LENGTH_SHORT).show();
                    return;
                }
                updateProfile("username", username, dialog);
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void sendPasswordChangeRequest(String password, View btn) {
        String token = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("access_token", "");
        Map<String, String> body = new HashMap<>();
        body.put("password", password);

        btn.setEnabled(false);
        RetrofitClient.getApiService().changePassword("Bearer " + token, body).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btn.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    String tempToken = response.body().getAccessToken();
                    getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                            .putString("temp_password_token", tempToken)
                            .putLong("otp_timestamp", System.currentTimeMillis())
                            .apply();
                    Toast.makeText(DashboardActivity.this, "OTP sent to your email", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(DashboardActivity.this, "Requesting too much OTP's. Try again after 1 min.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btn.setEnabled(true);
                Toast.makeText(DashboardActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyPasswordUpdate(String tempToken, String otp, Dialog dialog) {
        String token = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("access_token", "");
        Map<String, String> body = new HashMap<>();
        body.put("token", tempToken);
        body.put("otp", otp);

        RetrofitClient.getApiService().verifyUpdate("Bearer " + token, body).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(DashboardActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                    getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                            .remove("temp_password_token")
                            .remove("otp_timestamp")
                            .apply();
                    dialog.dismiss();
                } else {
                    Toast.makeText(DashboardActivity.this, "Invalid OTP !", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProfile(String field, String value, Dialog dialog) {
        String token = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("access_token", "");
        Map<String, String> updates = new HashMap<>();
        updates.put(field, value);

        String authHeader = "Bearer " + token;
        Call<UserResponse> call;

        if ("username".equals(field)) {
            call = RetrofitClient.getApiService().changeUserDetails(authHeader, updates);
        } else {
            call = RetrofitClient.getApiService().updateProfile(authHeader, updates);
        }

        call.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(DashboardActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    fetchUserInfo();
                    dialog.dismiss();
                } else {
                    Toast.makeText(DashboardActivity.this, "This username has been taken. Please try a new username", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerAnalysisReceiver() {
        IntentFilter filter = new IntentFilter("com.example.realiylens.ANALYSIS_FINISHED");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(statusReceiver, filter);
        }
    }

    private void handleVerification() {
        if (isTextMode) {
            String text = etTextInput.getText().toString().trim();
            if (!text.isEmpty()) submitTextForVerification(text);
            else Toast.makeText(this, "Please enter text to verify", Toast.LENGTH_SHORT).show();
        } else {
            if (selectedImageUri != null) uploadImage(selectedImageUri);
            else Toast.makeText(this, "Please select a image first", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetInputs() {
        etTextInput.setText("");
        selectedImageUri = null;
        if (llSelectedPreviewContainer != null && llUploadPlaceholder != null) {
            llSelectedPreviewContainer.setVisibility(View.GONE);
            llUploadPlaceholder.setVisibility(View.VISIBLE);
        }
        Toast.makeText(this, "Reset successful", Toast.LENGTH_SHORT).show();
    }

    private void uploadImage(Uri uri) {
        try {
            File file = getFileFromUri(uri);
            if (file == null) return;

            RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(uri)), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            String token = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("access_token", "");
            btnSendVerification.setEnabled(false);
            btnSendVerification.setText("Uploading...");

            RetrofitClient.getApiService().submitImage("Bearer " + token, body).enqueue(new Callback<SubmitResponse>() {
                @Override
                public void onResponse(Call<SubmitResponse> call, Response<SubmitResponse> response) {
                    btnSendVerification.setEnabled(true);
                    btnSendVerification.setText("Send Verification");
                    if (response.isSuccessful() && response.body() != null) {
                        selectedImageUri = null;
                        if (llSelectedPreviewContainer != null) llSelectedPreviewContainer.setVisibility(View.GONE);
                        if (llUploadPlaceholder != null) llUploadPlaceholder.setVisibility(View.VISIBLE);
                        Intent intent = new Intent(DashboardActivity.this, VerificationResultActivity.class);
                        intent.putExtra("job_id", response.body().getJobId());
                        startActivity(intent);
                    } else {
                        Toast.makeText(DashboardActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<SubmitResponse> call, Throwable t) {
                    btnSendVerification.setEnabled(true);
                    btnSendVerification.setText("Send Verification");
                    Toast.makeText(DashboardActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "uploadImage error", e);
        }
    }

    private File getFileFromUri(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return null;
            File file = new File(getCacheDir(), "upload_image.jpg");
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
            }
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    private void switchMode(boolean textMode) {
        this.isTextMode = textMode;
        tvToggleText.setBackgroundResource(textMode ? R.drawable.btn_gradient_simple : 0);
        tvToggleText.setTextColor(textMode ? Color.WHITE : Color.parseColor("#B0BEC5"));
        tvToggleText.setTypeface(null, textMode ? Typeface.BOLD : Typeface.NORMAL);
        
        tvToggleFile.setBackgroundResource(!textMode ? R.drawable.btn_gradient_simple : 0);
        tvToggleFile.setTextColor(!textMode ? Color.WHITE : Color.parseColor("#B0BEC5"));
        tvToggleFile.setTypeface(null, !textMode ? Typeface.BOLD : Typeface.NORMAL);
        
        clUploadArea.setVisibility(textMode ? View.GONE : View.VISIBLE);
        etTextInput.setVisibility(textMode ? View.VISIBLE : View.GONE);
    }

    private void submitTextForVerification(String text) {
        String token = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("access_token", "");
        RetrofitClient.getApiService().submitText("Bearer " + token, new TextSubmitRequest(text)).enqueue(new Callback<SubmitResponse>() {
            @Override
            public void onResponse(Call<SubmitResponse> call, Response<SubmitResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    etTextInput.setText("");
                    Intent intent = new Intent(DashboardActivity.this, VerificationResultActivity.class);
                    intent.putExtra("job_id", response.body().getJobId());
                    startActivity(intent);
                } else {
                    Toast.makeText(DashboardActivity.this, "Verification failed", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<SubmitResponse> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUserInfo() {
        String token = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("access_token", "");
        if (token.isEmpty()) return;

        RetrofitClient.getApiService().getUserInfo("Bearer " + token).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String username = response.body().getUsername();
                    tvWelcomeUser.setText("Hey " + username + ",");
                    tvUserUsername.setText(username);
                    tvUserEmail.setText(response.body().getEmail());
                    if (username != null && !username.isEmpty()) {
                        tvAvatarLetter.setText(String.valueOf(username.charAt(0)).toUpperCase());
                    }
                }
            }
            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {}
        });
    }

    private void fetchHistory() {
        String token = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("access_token", "");
        if (token.isEmpty()) return;

        if (isFirstLoad) {
            llSkeletonContainer.setVisibility(View.VISIBLE);
            rvVerifications.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.GONE);
        }

        Log.d(TAG, "Fetching history...");
        RetrofitClient.getApiService().getHistory("Bearer " + token).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                isFirstLoad = false;
                llSkeletonContainer.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "History RAW JSON: " + response.body().toString());
                    List<MainResponseModel> history = extractHistoryList(response.body());
                    Log.d(TAG, "Extracted history items: " + (history != null ? history.size() : 0));
                    
                    if (history == null || history.isEmpty()) {
                        rvVerifications.setVisibility(View.GONE);
                        llEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        rvVerifications.setVisibility(View.VISIBLE);
                        llEmptyState.setVisibility(View.GONE);
                        historyAdapter.setItems(history);
                    }
                } else {
                    Log.e(TAG, "History fetch failed: " + response.code());
                    llEmptyState.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                isFirstLoad = false;
                llSkeletonContainer.setVisibility(View.GONE);
                Log.e(TAG, "History fetch error", t);
                llEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    private List<MainResponseModel> extractHistoryList(JsonElement element) {
        if (element == null || element.isJsonNull()) return new ArrayList<>();
        Type type = new TypeToken<List<MainResponseModel>>(){}.getType();

        try {
            if (element.isJsonArray()) {
                JsonArray arr = element.getAsJsonArray();
                if (arr.size() > 0 && arr.get(0).isJsonArray()) {
                    return extractHistoryList(arr.get(0));
                }
                return gson.fromJson(element, type);
            } else if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                String[] commonKeys = {"history", "data", "results", "verifications", "jobs", "records", "items"};
                for (String key : commonKeys) {
                    if (obj.has(key)) {
                        return extractHistoryList(obj.get(key));
                    }
                }
                for (String key : obj.keySet()) {
                    JsonElement child = obj.get(key);
                    if (child != null && (child.isJsonArray() || child.isJsonObject())) {
                        List<MainResponseModel> found = extractHistoryList(child);
                        if (!found.isEmpty()) return found;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Extraction error", e);
        }
        return new ArrayList<>();
    }

    private void logout() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener(task -> {
            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().remove("access_token").apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchHistory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(statusReceiver); } catch (Exception ignored) {}
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START);
        else super.onBackPressed();
    }
}
