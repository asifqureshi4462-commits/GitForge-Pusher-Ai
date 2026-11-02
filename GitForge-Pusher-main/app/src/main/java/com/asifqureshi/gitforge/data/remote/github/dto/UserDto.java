package com.asifqureshi.gitforge.data.remote.github.dto;

import com.google.gson.annotations.SerializedName;

public class UserDto {
    public String login;
    public String name;
    @SerializedName("avatar_url")
    public String avatarUrl;
    @SerializedName("public_repos")
    public int publicRepos;
    @SerializedName("total_private_repos")
    public Integer totalPrivateRepos;
}
