package com.asifqureshi.gitforge.data.remote.github.dto;

import com.google.gson.annotations.SerializedName;

public class RepoDto {
    public String name;
    @SerializedName("full_name")
    public String fullName;
    @SerializedName("private")
    public boolean isPrivate;
    public String description;
    @SerializedName("default_branch")
    public String defaultBranch;
    @SerializedName("html_url")
    public String htmlUrl;
    public boolean archived;
    public Owner owner;
    public Permissions permissions;

    public static class Owner {
        public String login;
    }

    public static class Permissions {
        public boolean push;
    }
}
