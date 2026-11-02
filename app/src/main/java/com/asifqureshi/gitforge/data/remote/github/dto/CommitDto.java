package com.asifqureshi.gitforge.data.remote.github.dto;

public class CommitDto {
    public String sha;
    public TreeRef tree;

    public static class TreeRef {
        public String sha;
    }
}
