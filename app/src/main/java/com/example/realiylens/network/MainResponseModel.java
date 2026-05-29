package com.example.realiylens.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MainResponseModel {

    @SerializedName("id")
    private String id;

    @SerializedName("status")
    private String status;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("result")
    private ResultResponse result;

    // Added fields for flat response structure
    @SerializedName("claim")
    private String claim;

    @SerializedName("verdict")
    private String verdict;

    @SerializedName("confidence")
    private Double confidence;

    @SerializedName("reality_score")
    private Double realityScore;

    @SerializedName("explanation")
    private String explanation;

    @SerializedName("evidence")
    private List<ResultResponse.EvidenceItem> evidence;

    @SerializedName("time_taken")
    private Double timeTaken;

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public ResultResponse getResult() {
        return result;
    }

    public String getClaim() {
        return claim;
    }

    public String getVerdict() {
        return verdict;
    }

    public Double getConfidence() {
        return confidence;
    }

    public Double getRealityScore() {
        return realityScore;
    }

    public String getExplanation() {
        return explanation;
    }

    public List<ResultResponse.EvidenceItem> getEvidence() {
        return evidence;
    }

    public Double getTimeTaken() {
        return timeTaken;
    }
}
