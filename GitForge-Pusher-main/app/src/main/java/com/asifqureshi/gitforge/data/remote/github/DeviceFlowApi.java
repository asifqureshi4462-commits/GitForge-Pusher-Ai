package com.asifqureshi.gitforge.data.remote.github;

import com.asifqureshi.gitforge.data.remote.github.dto.DeviceCodeResponse;
import com.asifqureshi.gitforge.data.remote.github.dto.TokenResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/** GitHub OAuth device flow (https://github.com). Needs only the public client id - never a client secret. */
public interface DeviceFlowApi {

    String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code";

    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("login/device/code")
    Call<DeviceCodeResponse> requestCode(@Field("client_id") String clientId,
                                         @Field("scope") String scope);

    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("login/oauth/access_token")
    Call<TokenResponse> pollToken(@Field("client_id") String clientId,
                                  @Field("device_code") String deviceCode,
                                  @Field("grant_type") String grantType);
}
