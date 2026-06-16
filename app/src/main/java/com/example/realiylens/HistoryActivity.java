package com.example.realiylens;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.realiylens.network.MainResponseModel;
import com.example.realiylens.network.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "RealityLens_History";
    private static final Gson gson = new Gson();
    
    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;
    private LinearLayout llSkeletonContainer, llEmptyState;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        rvHistory = findViewById(R.id.rv_history);
        llSkeletonContainer = findViewById(R.id.ll_skeleton_container);
        llEmptyState = findViewById(R.id.ll_empty_state);
        btnBack = findViewById(R.id.btn_back);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(this);
        rvHistory.setAdapter(historyAdapter);

        btnBack.setOnClickListener(v -> finish());

        fetchHistory();
    }

    private void fetchHistory() {
        llSkeletonContainer.setVisibility(View.VISIBLE);
        rvHistory.setVisibility(View.GONE);
        llEmptyState.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String token = prefs.getString("access_token", "");
        if (token.isEmpty()) {
            llSkeletonContainer.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        Log.d(TAG, "Fetching history for HistoryActivity...");
        RetrofitClient.getApiService().getHistory("Bearer " + token).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                llSkeletonContainer.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "History RAW JSON: " + response.body().toString());
                    List<MainResponseModel> history = extractHistoryList(response.body());
                    Log.d(TAG, "Extracted items: " + (history != null ? history.size() : 0));

                    if (history == null || history.isEmpty()) {
                        rvHistory.setVisibility(View.GONE);
                        llEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        rvHistory.setVisibility(View.VISIBLE);
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
                Log.e(TAG, "Network failure", t);
                llSkeletonContainer.setVisibility(View.GONE);
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
                // Check for nested array: [[{...}]]
                if (arr.size() > 0 && arr.get(0).isJsonArray()) {
                    return extractHistoryList(arr.get(0));
                }
                List<MainResponseModel> result = gson.fromJson(element, type);
                return result != null ? result : new ArrayList<>();
            } else if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                String[] commonKeys = {"history", "data", "results", "verifications", "jobs", "records", "items"};
                for (String key : commonKeys) {
                    if (obj.has(key)) {
                        List<MainResponseModel> found = extractHistoryList(obj.get(key));
                        if (!found.isEmpty()) return found;
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
}
