package com.example.realiylens.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainResponseModel {

    @SerializedName(value = "id", alternate = {"_id", "job_id", "jobId", "ID", "uuid", "pk", "key", "job", "analysis_id", "task_id", "record_id", "request_id", "uid", "recordId"})
    private String id;

    @SerializedName(value = "status", alternate = {"Status", "STATUS", "state", "job_status", "analysis_status"})
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
        "original_url", "image_url_path", "ImagePath", "ImgUrl"
    })
    private String imageUrl;

    @SerializedName(value = "created_at", alternate = {"createdAt", "timestamp", "time", "date", "created", "CreatedAt", "Created_At", "date_created"})
    private String createdAt;

    @SerializedName(value = "result", alternate = {"data", "analysis", "output", "response", "Result", "ResultData"})
    private JsonElement result;

    @SerializedName(value = "claim", alternate = {"Claim", "CLAIM", "text", "content", "captured_text", "input_text", "input", "query"})
    private String claim;

    @SerializedName(value = "verdict", alternate = {"Verdict", "VERDICT", "status_text", "final_verdict", "result_text", "classification"})
    private String verdict;

    @SerializedName(value = "confidence", alternate = {"Confidence", "CONFIDENCE", "conf", "probability", "score"})
    private Double confidence;

    @SerializedName(value = "reality_score", alternate = {"realityScore", "RealityScore", "REALITY_SCORE", "authenticity_score", "truth_score"})
    private Double realityScore;

    @SerializedName(value = "explanation", alternate = {"Explanation", "EXPLANATION", "reason", "summary", "description", "details"})
    private String explanation;

    @SerializedName(value = "evidence", alternate = {"Evidence", "EVIDENCE", "sources", "links", "references", "supporting_evidence"})
    private List<ResultResponse.EvidenceItem> evidence;

    public String getId() { return id; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }

    public ResultResponse getResult() {
        if (result == null) return null;
        Gson gson = new Gson();
        if (result.isJsonObject()) {
            return gson.fromJson(result, ResultResponse.class);
        } else if (result.isJsonArray()) {
            JsonArray arr = result.getAsJsonArray();
            if (arr.size() > 0 && arr.get(0).isJsonObject()) {
                return gson.fromJson(arr.get(0), ResultResponse.class);
            }
        }
        return null;
    }

    public String getImageUrl() {
        if (imageUrl != null) return imageUrl;
        ResultResponse res = getResult();
        return res != null ? res.getImageUrl() : null;
    }

    public String getClaim() {
        if (claim != null) return claim;
        ResultResponse res = getResult();
        return res != null ? res.getClaim() : null;
    }

    public String getVerdict() {
        if (verdict != null) return verdict;
        ResultResponse res = getResult();
        return res != null ? res.getVerdict() : null;
    }

    public Double getConfidence() {
        if (confidence != null) return confidence;
        ResultResponse res = getResult();
        return res != null ? res.getConfidence() : null;
    }

    public Double getRealityScore() {
        if (realityScore != null) return realityScore;
        ResultResponse res = getResult();
        return res != null ? res.getRealityScore() : null;
    }

    public String getExplanation() {
        if (explanation != null) return explanation;
        ResultResponse res = getResult();
        return res != null ? res.getExplanation() : null;
    }

    public List<ResultResponse.EvidenceItem> getEvidence() {
        if (evidence != null) return evidence;
        ResultResponse res = getResult();
        return res != null ? res.getEvidence() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MainResponseModel that = (MainResponseModel) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(status, that.status) &&
                Objects.equals(verdict, that.getVerdict());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status, verdict);
    }
}
