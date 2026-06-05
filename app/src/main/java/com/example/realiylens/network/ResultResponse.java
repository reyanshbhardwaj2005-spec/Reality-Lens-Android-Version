package com.example.realiylens.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ResultResponse {
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

    @SerializedName(value = "image_url", alternate = {
        "image", "imageUrl", "image_path", "img_url", "screenshot", 
        "captured_image", "thumbnail", "img", "photo", "picture", 
        "file_path", "url", "full_image", "result_image", "original_image", 
        "media_url", "img_src", "preview", "preview_url", "screenshot_url",
        "image_url_full", "image_url_thumb", "thumb", "thumbnail_url",
        "image_file", "file", "path", "src", "original", "icon",
        "Image", "ImageUrl", "Image_url", "IMAGE_URL", "URL", "FILE", "PATH"
    })
    private String imageUrl;

    @SerializedName("evidence")
    private List<EvidenceItem> evidence;

    public String getClaim() { return claim; }
    public String getVerdict() { return verdict; }
    public Double getConfidence() { return confidence; }
    public Double getRealityScore() { return realityScore; }
    public String getExplanation() { return explanation; }
    public String getImageUrl() { return imageUrl; }
    public List<EvidenceItem> getEvidence() { return evidence; }

    public static class EvidenceItem {
        @SerializedName("url")
        private String url;
        @SerializedName("title")
        private String title;
        @SerializedName("source")
        private String source;
        @SerializedName("stance")
        private String stance;

        public String getUrl() { return url; }
        public String getTitle() { return title; }
        public String getSource() { return source; }
        public String getStance() { return stance; }
    }
}
