package com.example.realiylens;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
 import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.realiylens.network.MainResponseModel;
import com.example.realiylens.network.RetrofitClient;
import com.example.realiylens.network.SubmitResponse;
import com.example.realiylens.network.TextSubmitRequest;
import com.example.realiylens.network.UserResponse;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "Dashboard_Binding";
    private DrawerLayout drawerLayout;
    private LinearProgressIndicator progressBar;
    private RecyclerView rvVerifications;
    private HistoryAdapter historyAdapter;
    private LinearLayout llSkeletonContainer, llEmptyState;
    private TextView tvUserUsername, tvUserEmail;
    private EditText etSearch;

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.example.realiylens.ANALYSIS_STARTED".equals(action)) {
                if (progressBar != null) progressBar.show();
            } else if ("com.example.realiylens.ANALYSIS_FINISHED".equals(action)) {
                if (progressBar != null) progressBar.hide();
                fetchHistory();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        drawerLayout = findViewById(R.id.drawer_layout);
        progressBar = findViewById(R.id.pb_dashboard_loading);
        rvVerifications = findViewById(R.id.rv_verifications);
        llSkeletonContainer = findViewById(R.id.ll_skeleton_container);
        llEmptyState = findViewById(R.id.ll_empty_state);
        etSearch = findViewById(R.id.et_dashboard_search);
        ImageButton btnMenu = findViewById(R.id.btn_hamburger_menu);
        NavigationView navigationView = findViewById(R.id.nav_view_sidebar);
        
        rvVerifications.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(this);
        rvVerifications.setAdapter(historyAdapter);

        View headerView = navigationView.getHeaderView(0);
        tvUserUsername = headerView.findViewById(R.id.tv_user_username);
        tvUserEmail = headerView.findViewById(R.id.tv_user_email);

        // Listen for Search action on keyboard
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                String inputText = etSearch.getText().toString().trim();
                if (!inputText.isEmpty()) {
                    hideKeyboard();
                    submitTextForVerification(inputText);
                } else {
                    Toast.makeText(this, "Please enter text to verify", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        final boolean[] isRotated = {false};
        btnMenu.setOnClickListener(v -> {
            if (!isRotated[0]) {
                btnMenu.animate().rotation(180f).setDuration(300).start();
            } else {
                btnMenu.animate().rotation(0f).setDuration(300).start();
            }
            isRotated[0] = !isRotated[0];
            drawerLayout.openDrawer(GravityCompat.END);
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                logout();
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(DashboardActivity.this, SettingsActivity.class));
            } else if (id == R.id.nav_about_us) {
                startActivity(new Intent(DashboardActivity.this, AboutUsActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return false;
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.realiylens.ANALYSIS_STARTED");
        filter.addAction("com.example.realiylens.ANALYSIS_FINISHED");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(statusReceiver, filter);
        }
        
        checkAnalysisStatus();
        fetchUserInfo();
        fetchHistory();
    }

    private void submitTextForVerification(String text) {
        progressBar.show();
        
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String token = prefs.getString("access_token", "");
        String authHeader = "Bearer " + token;

        // Uses the "input" field as requested
        TextSubmitRequest request = new TextSubmitRequest(text);
        
        RetrofitClient.getApiService().submitText(authHeader, request).enqueue(new Callback<SubmitResponse>() {
            @Override
            public void onResponse(Call<SubmitResponse> call, Response<SubmitResponse> response) {
                progressBar.hide();
                if (response.isSuccessful() && response.body() != null) {
                    String jobId = response.body().getJobId();
                    etSearch.setText(""); // Clear input on success
                    
                    // Navigate to results using the returned job_id
                    Intent intent = new Intent(DashboardActivity.this, VerificationResultActivity.class);
                    intent.putExtra("job_id", jobId);
                    startActivity(intent);
                } else {
                    Toast.makeText(DashboardActivity.this, "Verification failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SubmitResponse> call, Throwable t) {
                progressBar.hide();
                Toast.makeText(DashboardActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void fetchUserInfo() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String token = prefs.getString("access_token", "");
        if (token.isEmpty()) return;

        RetrofitClient.getApiService().getUserInfo("Bearer " + token).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvUserUsername.setText(response.body().getUsername());
                    tvUserEmail.setText(response.body().getEmail());
                }
            }
            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e(TAG, "Failed to fetch user info: " + t.getMessage());
            }
        });
    }

    private void logout() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().remove("access_token").apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void checkAnalysisStatus() {
        if (getSharedPreferences("AppPrefs", MODE_PRIVATE).getBoolean("is_analyzing", false)) {
            if (progressBar != null) progressBar.show();
        } else {
            if (progressBar != null) progressBar.hide();
        }
    }

    private void fetchHistory() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String token = prefs.getString("access_token", "");
        if (token.isEmpty()) return;

        RetrofitClient.getApiService().getHistory("Bearer " + token).enqueue(new Callback<List<MainResponseModel>>() {
            @Override
            public void onResponse(Call<List<MainResponseModel>> call, Response<List<MainResponseModel>> response) {
                llSkeletonContainer.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<MainResponseModel> history = response.body();
                    if (history.isEmpty()) {
                        rvVerifications.setVisibility(View.GONE);
                        llEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        rvVerifications.setVisibility(View.VISIBLE);
                        llEmptyState.setVisibility(View.GONE);
                        historyAdapter.setItems(history);
                    }
                } else {
                    rvVerifications.setVisibility(View.GONE);
                    llEmptyState.setVisibility(View.VISIBLE);
                    if (response.code() == 401) logout();
                }
            }

            @Override
            public void onFailure(Call<List<MainResponseModel>> call, Throwable t) {
                llSkeletonContainer.setVisibility(View.GONE);
                rvVerifications.setVisibility(View.GONE);
                llEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchUserInfo();
        fetchHistory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(statusReceiver); } catch (Exception ignored) {}
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}
