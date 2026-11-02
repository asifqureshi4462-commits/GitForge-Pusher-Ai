package com.asifqureshi.gitforge.data.remote.github.dto;

public class BlobRequest {
    public final String content;
    public final String encoding = "base64";

    public BlobRequest(String contentBase64) {
        this.content = contentBase64;
    }
}
