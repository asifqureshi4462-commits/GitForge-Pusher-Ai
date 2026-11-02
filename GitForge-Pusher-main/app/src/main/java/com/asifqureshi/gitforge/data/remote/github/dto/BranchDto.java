package com.asifqureshi.gitforge.data.remote.github.dto;

import com.google.gson.annotations.SerializedName;

public class BranchDto {
    public String name;
    @SerializedName("protected")
    public boolean isProtected;
}
