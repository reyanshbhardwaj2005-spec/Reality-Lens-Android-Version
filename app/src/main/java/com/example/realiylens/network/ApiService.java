package com.example.realiylens.network;

import java.util.List;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {
    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("register")
    Call<LoginResponse> register(@Body RegisterRequest request);

    @GET("me")
    Call<UserResponse> getUserInfo(@Header("Authorization") String authHeader);

    @Multipart
    @POST("submit")
    Call<SubmitResponse> submitImage(
            @Header("Authorization") String authHeader,
            @Part MultipartBody.Part file
    );

    @GET("result/{job_id}")
    Call<MainResponseModel> getResult(
            @Header("Authorization") String authHeader,
            @Path("job_id") String jobId
    );

    @GET("history")
    Call<List<MainResponseModel>> getHistory(
            @Header("Authorization") String authHeader
    );
}
