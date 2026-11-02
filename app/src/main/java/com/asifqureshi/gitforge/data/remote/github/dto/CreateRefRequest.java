package com.asifqureshi.gitforge.data.remote.github.dto;

public class CreateRefRequest {
    public final String ref;
    public final String sha;

    public CreateRefRequest(String ref, String sha) {
        this.ref = ref;
        this.sha = sha;
    }
}
