package com.example.realiylens.network;

import com.google.gson.annotations.SerializedName;

public class TextSubmitRequest {
    @SerializedName("input")
    private String input;

    public TextSubmitRequest(String input) {
        this.input = input;
    }

    public String getInput() {
        return input;
    }
}
