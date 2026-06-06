package com.example.realiylens;

import android.os.Bundle;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class AboutUsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        LinearLayout btnBack = findViewById(R.id.ll_back_about);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }
}
