package com.example.realiylens;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Button btnContinue, btnGoogle;
    private androidx.appcompat.widget.AppCompatButton btnLoginHere;
    private ImageView ivPasswordToggle;

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
            Toast.makeText(this, "Google Sign-In clicked", Toast.LENGTH_SHORT).show();
        });

        btnLoginHere.setOnClickListener(v -> finish());
    }
}
