package com.asifqureshi.gitforge.data.remote.github.dto;

import com.google.gson.annotations.SerializedName;

public class TokenResponse {
    @SerializedName("access_token")
    public String accessToken;
    @SerializedName("token_type")
    public String tokenType;
    public String scope;
    public String error;
    public int interval;
}
