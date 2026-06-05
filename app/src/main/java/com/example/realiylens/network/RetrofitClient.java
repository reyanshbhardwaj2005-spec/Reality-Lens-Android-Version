package com.example.realiylens.network;

import android.net.Uri;
import com.example.realiylens.BuildConfig;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    public static final String BASE_URL = BuildConfig.BASE_URL;
    private static Retrofit retrofit = null;

    public static ApiService getApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(90, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS)
                    .writeTimeout(90, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    /**
     * Helper to get full URL for images. 
     * Handles relative paths, legacy formats, and recovers paths from dead local development URLs.
     */
    public static String getFullImageUrl(String inputUrl) {
        if (inputUrl == null || inputUrl.trim().isEmpty() || 
            inputUrl.equalsIgnoreCase("null") || 
            inputUrl.equalsIgnoreCase("undefined") ||
            inputUrl.contains("[object")) {
            return null;
        }
        
        String url = inputUrl.trim().replace("\\", "/");
        if (url.startsWith("data:image")) return url;
        
        String cleanBase = BASE_URL != null ? BASE_URL.replaceAll("/+$", "") : "";
        
        // Recover path from absolute URLs that point to local development hosts.
        // We only strip the host if it's clearly a local environment that won't work on mobile.
        if (url.startsWith("http")) {
            try {
                Uri uri = Uri.parse(url);
                String host = uri.getHost();
                if (host != null && isLocalHost(host)) {
                    url = uri.getPath();
                } else if (host != null) {
                    // For all other absolute URLs (including other Render hosts), 
                    // we keep them as is. This ensures we don't break old links if the host is still alive.
                    return url; 
                }
            } catch (Exception ignored) {}
        }
        
        if (url == null || url.isEmpty()) return null;
        if (url.startsWith("//")) return "https:" + url;

        // Remove redundant legacy prefixes that might be baked into database records.
        // If the path starts with /api/ but our BASE_URL might or might not handle it,
        // we'll keep it simple: just ensure it's a valid relative path from the domain root.
        url = url.replaceFirst("^/?(api/|static/|public/|media/|v1/)", "/");
        
        // If it's just a filename (no slashes), assume it's in the common uploads/ directory.
        String pathOnly = url.startsWith("/") ? url.substring(1) : url;
        if (!pathOnly.contains("/") && !pathOnly.isEmpty()) {
            url = "/uploads/" + pathOnly;
        }

        String cleanRelative = url.startsWith("/") ? url : "/" + url;
        String result = cleanBase + cleanRelative;
        
        // Final cleanup of double slashes in path (preserving protocol slashes)
        return result.replaceAll("(?<!:)/{2,}", "/");
    }

    private static boolean isLocalHost(String host) {
        String h = host.toLowerCase();
        return h.equals("localhost") || 
               h.equals("127.0.0.1") || 
               h.equals("10.0.2.2") || 
               h.startsWith("192.168.") ||
               h.startsWith("172.") ||
               h.startsWith("10.");
    }
}
