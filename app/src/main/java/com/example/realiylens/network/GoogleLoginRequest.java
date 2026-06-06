package com.example.realiylens.network;

import com.google.gson.annotations.SerializedName;

public class GoogleLoginRequest {
    @SerializedName("id_token")
    private String idToken;

    public GoogleLoginRequest(String idToken) {
        this.idToken = idToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
