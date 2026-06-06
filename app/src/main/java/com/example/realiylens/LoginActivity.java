package com.example.realiylens;

import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import java.security.MessageDigest;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "RealityLens_Login";
    private EditText etEmail, etPassword;
    private Button btnContinue, btnGoogle, btnRegister;
    private ImageView iv_password_toggle;
    private ProgressBar progressBar;

    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // This prints your SHA-1 to Logcat automatically!
        printAppSignature();

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String savedToken = prefs.getString("access_token", null);
        if (savedToken != null && !savedToken.isEmpty()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        // Step 4: Request ID Token using the WEB CLIENT ID from console
        String webClientId = getString(R.string.default_web_client_id);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(webClientId) 
                .build();
        
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    setLoading(false);
                    handleSignInResult(GoogleSignIn.getSignedInAccountFromIntent(result.getData()));
                }
        );

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnContinue = findViewById(R.id.btn_continue);
        btnGoogle = findViewById(R.id.btn_google);
        btnRegister = findViewById(R.id.btn_register);
        iv_password_toggle = findViewById(R.id.iv_password_toggle);
        progressBar = findViewById(R.id.login_progress);

        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> {
                setLoading(true);
                // Step 1: User taps button
                mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                    // Step 2: Open account picker
                    googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent());
                });
            });
        }

        btnContinue.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all details", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, WelcomeActivity.class);
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

        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        requestAddQuickSettingsTile();
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null && account.getIdToken() != null) {
                // Step 4: Android got ID Token. Step 5: Send to WelcomeActivity to call FastAPI
                Intent intent = new Intent(this, WelcomeActivity.class);
                intent.putExtra("action", "google_login");
                intent.putExtra("id_token", account.getIdToken());
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Error: Google returned no ID Token. Check Web Client ID.", Toast.LENGTH_LONG).show();
            }
        } catch (ApiException e) {
            int code = e.getStatusCode();
            Log.e(TAG, "Sign-in error: " + code + " - " + e.getMessage());
            String msg;
            if (code == 10) msg = "Developer Error (10): Ensure SHA-1 and Web Client ID are correct in Google Console.";
            else if (code == 12501) msg = "Sign-in cancelled.";
            else if (code == 7) msg = "Network error. Check connection.";
            else msg = "Sign-in failed (Error " + code + ")";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    private void printAppSignature() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA1");
                md.update(signature.toByteArray());
                byte[] digest = md.digest();
                StringBuilder hexString = new StringBuilder();
                for (byte b : digest) hexString.append(String.format("%02X:", b));
                String sha1 = hexString.toString().replaceAll(":$", "");
                Log.d(TAG, "--------------------------------------------");
                Log.d(TAG, "GOOGLE CONSOLE SHA-1: " + sha1);
                Log.d(TAG, "--------------------------------------------");
            }
        } catch (Exception e) { Log.e(TAG, "Failed to get signature", e); }
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (btnGoogle != null) btnGoogle.setEnabled(!isLoading);
    }

    private void requestAddQuickSettingsTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            if (!prefs.getBoolean("tile_requested", false)) {
                StatusBarManager sbm = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
                if (sbm != null) {
                    ComponentName cn = new ComponentName(this, SnippingTileService.class);
                    sbm.requestAddTileService(cn, "Snip Area", Icon.createWithResource(this, R.drawable.icon), getMainExecutor(), res -> {
                        if (res == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) 
                            prefs.edit().putBoolean("tile_requested", true).apply();
                    });
                }
            }
        }
    }
}
