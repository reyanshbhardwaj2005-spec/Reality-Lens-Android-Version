package com.example.realiylens.network;

import com.google.gson.annotations.SerializedName;

public class UserResponse {
    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
}
