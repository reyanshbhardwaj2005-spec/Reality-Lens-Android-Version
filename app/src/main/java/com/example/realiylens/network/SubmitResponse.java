package com.example.realiylens.network;

import com.google.gson.annotations.SerializedName;

public class SubmitResponse {
    @SerializedName("job_id")
    private String jobId;

    public String getJobId() {
        return jobId;
    }
}
