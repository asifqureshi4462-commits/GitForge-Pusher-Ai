package com.asifqureshi.gitforge.data.remote.github.dto;

public class UpdateRefRequest {
    public final String sha;
    /** Always false: the app never force-pushes. */
    public final boolean force = false;

    public UpdateRefRequest(String sha) {
        this.sha = sha;
    }
}
