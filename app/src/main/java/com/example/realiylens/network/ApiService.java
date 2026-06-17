package com.example.realiylens.network;

import com.google.gson.JsonElement;
import java.util.List;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("register")
    Call<LoginResponse> register(@Body RegisterRequest request);

    @POST("verify-otp")
    Call<LoginResponse> verifyOtp(@Body VerifyOtpRequest request);

    @POST("auth/google")
    Call<LoginResponse> googleLogin(@Body GoogleLoginRequest request);

    @GET("me")
    Call<UserResponse> getUserInfo(@Header("Authorization") String authHeader);

    @Multipart
    @POST("submit")
    Call<SubmitResponse> submitImage(
            @Header("Authorization") String authHeader,
            @Part MultipartBody.Part file
    );

    @POST("submit-text")
    Call<SubmitResponse> submitText(
            @Header("Authorization") String authHeader,
            @Body TextSubmitRequest request
    );

    @GET("result/{job_id}")
    Call<MainResponseModel> getResult(
            @Header("Authorization") String authHeader,
            @Path("job_id") String jobId
    );

    @GET("history")
    Call<JsonElement> getHistory(
            @Header("Authorization") String authHeader
    );

    @PUT("update-profile")
    Call<UserResponse> updateProfile(
            @Header("Authorization") String authHeader,
            @Body java.util.Map<String, String> updates
    );

    @PATCH("change-user-details")
    Call<UserResponse> changeUserDetails(
            @Header("Authorization") String authHeader,
            @Body java.util.Map<String, String> updates
    );

    @PATCH("change-user-details")
    Call<LoginResponse> changePassword(
            @Header("Authorization") String authHeader,
            @Body java.util.Map<String, String> updates
    );

    @POST("verify-update")
    Call<UserResponse> verifyUpdate(
            @Header("Authorization") String authHeader,
            @Body java.util.Map<String, String> body
    );

    @POST("request-delete-account")
    Call<LoginResponse> requestDeleteAccount(
            @Header("Authorization") String authHeader
    );

    @POST("confirm-delete-account")
    Call<Void> confirmDeleteAccount(
            @Header("Authorization") String authHeader,
            @Body java.util.Map<String, String> body
    );

    @DELETE("delete-account")
    Call<LoginResponse> deleteAccount(
            @Header("Authorization") String authHeader
    );

    @POST("verify-delete")
    Call<Void> verifyDelete(
            @Header("Authorization") String authHeader,
            @Body java.util.Map<String, String> body
    );
}
