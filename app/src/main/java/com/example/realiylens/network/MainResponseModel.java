package com.example.realiylens.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MainResponseModel {

    @SerializedName(value = "id", alternate = {"_id", "job_id", "jobId", "ID", "uuid", "pk", "key", "job", "analysis_id", "task_id", "record_id", "request_id", "uid"})
    private String id;

    @SerializedName("status")
    private String status;

    @SerializedName(value = "image_url", alternate = {
        "image", "imageUrl", "image_path", "img_url", "screenshot", 
        "captured_image", "thumbnail", "img", "photo", "picture", 
        "file_path", "url", "full_image", "result_image", "original_image", 
        "media_url", "img_src", "preview", "preview_url", "screenshot_url",
        "image_url_full", "image_url_thumb", "thumb", "thumbnail_url",
        "image_file", "file", "path", "src", "original", "icon",
        "Image", "ImageUrl", "Image_url", "IMAGE_URL", "URL", "FILE", "PATH",
        "result_image_url", "input_image", "screenshot_path", "image_uri", "img_path",
        "original_url", "image_url_path"
    })
    private String imageUrl;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("result")
    private ResultResponse result;

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
