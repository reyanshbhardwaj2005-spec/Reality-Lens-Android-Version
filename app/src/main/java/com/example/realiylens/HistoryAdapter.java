package com.example.realiylens;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.realiylens.network.MainResponseModel;
import com.example.realiylens.network.RetrofitClient;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final Context context;
    private List<MainResponseModel> items = new ArrayList<>();
    private final Map<String, SimpleDateFormat> sdfCache = new HashMap<>();

    public HistoryAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<MainResponseModel> newItems) {
        if (newItems == null) newItems = new ArrayList<>();
        
        // Using DiffUtil for better performance and smoother updates
        final List<MainResponseModel> finalNewItems = newItems;
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return items.size(); }
            @Override
            public int getNewListSize() { return finalNewItems.size(); }
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                String oldId = items.get(oldItemPosition).getId();
                String newId = finalNewItems.get(newItemPosition).getId();
                return oldId != null && oldId.equals(newId);
            }
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return items.get(oldItemPosition).equals(finalNewItems.get(newItemPosition));
            }
        });
        
        this.items = new ArrayList<>(newItems);
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_verification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MainResponseModel item = items.get(position);

        String verdict = item.getVerdict();
        String claim = item.getClaim();
        Double realityScore = item.getRealityScore();
        Double confidence = item.getConfidence();
        String imageUrlStr = item.getImageUrl();

        if (item.getResult() != null) {
            if (verdict == null) verdict = item.getResult().getVerdict();
            if (claim == null) claim = item.getResult().getClaim();
            if (realityScore == null) realityScore = item.getResult().getRealityScore();
            if (confidence == null) confidence = item.getResult().getConfidence();
            if (imageUrlStr == null || imageUrlStr.isEmpty() || imageUrlStr.equals("null")) {
                imageUrlStr = item.getResult().getImageUrl();
            }
        }

        holder.tvVerdict.setText(verdict != null ? verdict.toUpperCase() : "ANALYZED");
        applyVerdictColor(holder.tvVerdict, holder.pbRealityScore, verdict);
        holder.tvContent.setText(claim != null ? claim : "No captured claim available");

        StringBuilder stats = new StringBuilder();
        if (realityScore != null) {
            stats.append("Reality: ").append((int)(realityScore * 100)).append("%");
        }
        if (confidence != null) {
            if (stats.length() > 0) stats.append(" | ");
            stats.append("Conf: ").append((int) (confidence * 100)).append("%");
        }
        holder.tvStats.setText(stats.toString());

        holder.pbRealityScore.setProgress(realityScore != null ? (int) (realityScore * 100) : 0);
        holder.tvTimestamp.setText(formatToIST(item.getCreatedAt()));
        
        // Optimized Image Loading
        if (imageUrlStr != null && imageUrlStr.startsWith("data:image")) {
            // Loading Base64 via Glide is more efficient as it handles decoding off-thread
            try {
                String base64Data = imageUrlStr.substring(imageUrlStr.indexOf(",") + 1);
                byte[] decodedString = Base64.decode(base64Data, Base64.DEFAULT);
                Glide.with(context)
                        .asBitmap()
                        .load(decodedString)
                        .placeholder(R.drawable.image)
                        .into(holder.ivThumbnail);
            } catch (Exception e) {
                holder.ivThumbnail.setImageResource(R.drawable.image);
            }
        } else {
            String fullImageUrl = RetrofitClient.getFullImageUrl(imageUrlStr);
            if (fullImageUrl != null && !fullImageUrl.isEmpty()) {
                String token = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("access_token", "");
                Object loadTarget = fullImageUrl;
                
                if (!token.isEmpty()) {
                    loadTarget = new GlideUrl(fullImageUrl, new LazyHeaders.Builder()
                            .addHeader("Authorization", "Bearer " + token)
                            .build());
                }

                Glide.with(context)
                        .load(loadTarget)
                        .placeholder(R.drawable.image)
                        .error(R.drawable.image)
                        .into(holder.ivThumbnail);
            } else {
                holder.ivThumbnail.setImageResource(R.drawable.image);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, VerificationResultActivity.class);
            intent.putExtra("job_id", item.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatToIST(String dateString) {
        if (dateString == null || dateString.isEmpty()) return "Date & Time";
        try {
            String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss"
            };

            Date date = null;
            for (String pattern : patterns) {
                try {
                    SimpleDateFormat sdf = sdfCache.get(pattern);
                    if (sdf == null) {
                        sdf = new SimpleDateFormat(pattern, Locale.getDefault());
                        if (!pattern.contains("X")) sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        sdfCache.put(pattern, sdf);
                    }
                    date = sdf.parse(dateString);
                    if (date != null) break;
                } catch (Exception ignored) {}
            }

            if (date == null) return dateString;

            SimpleDateFormat outputFormat = sdfCache.get("OUT");
            if (outputFormat == null) {
                outputFormat = new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault());
                outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
                sdfCache.put("OUT", outputFormat);
            }
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateString;
        }
    }

    private void applyVerdictColor(TextView tvVerdict, LinearProgressIndicator pbRealityScore, String verdict) {
        if (verdict == null) return;
        int colorRes = R.color.white;
        String v = verdict.toUpperCase();
        if (v.contains("REAL")) colorRes = R.color.verdict_real;
        else if (v.contains("FAKE")) colorRes = R.color.verdict_fake;
        else if (v.contains("SUSPICIOUS")) colorRes = R.color.verdict_suspicious;
        else if (v.contains("UNVERIFIED")) colorRes = R.color.verdict_unverified;
        else if (v.contains("UNREADABLE")) colorRes = R.color.verdict_unreadable;
        else if (v.contains("SATIRE")) colorRes = R.color.verdict_satire;
        
        int color = ContextCompat.getColor(context, colorRes);
        tvVerdict.setTextColor(color);
        pbRealityScore.setIndicatorColor(color);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvVerdict, tvContent, tvStats, tvTimestamp;
        ImageView ivThumbnail;
        LinearProgressIndicator pbRealityScore;

        ViewHolder(View itemView) {
            super(itemView);
            tvVerdict = itemView.findViewById(R.id.tv_verdict_badge);
            tvContent = itemView.findViewById(R.id.tv_content_preview);
            tvStats = itemView.findViewById(R.id.tv_stats);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            pbRealityScore = itemView.findViewById(R.id.pb_item_reality_score);
        }
    }
}
