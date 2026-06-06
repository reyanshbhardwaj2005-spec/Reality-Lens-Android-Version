package com.example.realiylens;

import android.content.Intent;
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

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RealityLens_Register";
    private EditText etName, etEmail, etPassword;
    private Button btnContinue, btnGoogle;
    private androidx.appcompat.widget.AppCompatButton btnLoginHere;
    private ImageView ivPasswordToggle;
    private ProgressBar progressBar;

    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnContinue = findViewById(R.id.btn_continue);
        btnGoogle = findViewById(R.id.btn_google_signup);
        btnLoginHere = findViewById(R.id.tv_login_here);
        ivPasswordToggle = findViewById(R.id.iv_password_toggle);
        progressBar = findViewById(R.id.register_progress);

        // Google Sign-In setup (Same as LoginActivity)
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

        btnContinue.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all details", Toast.LENGTH_SHORT).show();
            } else {
                // Navigate immediately to show the skeleton without delay
                Intent intent = new Intent(RegisterActivity.this, WelcomeActivity.class);
                intent.putExtra("name", name);
                intent.putExtra("email", email);
                intent.putExtra("password", password);
                intent.putExtra("action", "register");
                startActivity(intent);
                finishAffinity();
            }
        });

        ivPasswordToggle.setOnClickListener(v -> {
            if (etPassword.getTransformationMethod() instanceof PasswordTransformationMethod) {
                etPassword.setTransformationMethod(null);
            } else {
                etPassword.setTransformationMethod(new PasswordTransformationMethod());
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        btnGoogle.setOnClickListener(v -> {
            setLoading(true);
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent());
            });
        });

        btnLoginHere.setOnClickListener(v -> finish());
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null && account.getIdToken() != null) {
                Intent intent = new Intent(this, WelcomeActivity.class);
                intent.putExtra("action", "google_login");
                intent.putExtra("id_token", account.getIdToken());
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Error: Google returned no ID Token.", Toast.LENGTH_LONG).show();
            }
        } catch (ApiException e) {
            int code = e.getStatusCode();
            Log.e(TAG, "Sign-in error: " + code + " - " + e.getMessage());
            String msg = "Sign-in failed (Error " + code + ")";
            if (code == 12501) msg = "Sign-in cancelled.";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (btnGoogle != null) btnGoogle.setEnabled(!isLoading);
    }
}
