package com.example.realiylens.network;

import com.google.gson.annotations.SerializedName;

public class VerifyOtpRequest {
    @SerializedName("token")
    private String token;

    @SerializedName("otp")
    private String otp;

    public VerifyOtpRequest(String token, String otp) {
        this.token = token;
        this.otp = otp;
    }
}
