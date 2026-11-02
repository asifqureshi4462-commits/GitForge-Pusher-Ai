package com.asifqureshi.gitforge.data.remote.github.dto;

import com.google.gson.annotations.SerializedName;

public class DeviceCodeResponse {
    @SerializedName("device_code")
    public String deviceCode;
    @SerializedName("user_code")
    public String userCode;
    @SerializedName("verification_uri")
    public String verificationUri;
    @SerializedName("expires_in")
    public int expiresIn;
    public int interval;
    public String error;
}
