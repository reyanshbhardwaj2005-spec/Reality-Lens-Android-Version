package com.example.realiylens.network;

import com.example.realiylens.BuildConfig;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // BASE_URL is now fetched from BuildConfig which reads from .env file
    public static final String BASE_URL = BuildConfig.BASE_URL;
    private static Retrofit retrofit = null;

    public static ApiService getApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Timeouts increased to 90s to handle server cold starts
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
     * Helper to get full URL for images if the API returns a relative path
     */
    public static String getFullImageUrl(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isEmpty()) return null;
        if (relativeUrl.startsWith("http")) return relativeUrl;
        
        String baseUrl = BASE_URL;
        if (baseUrl.endsWith("/") && relativeUrl.startsWith("/")) {
            return baseUrl + relativeUrl.substring(1);
        } else if (!baseUrl.endsWith("/") && !relativeUrl.startsWith("/")) {
            return baseUrl + "/" + relativeUrl;
        } else {
            return baseUrl + relativeUrl;
        }
    }
}
