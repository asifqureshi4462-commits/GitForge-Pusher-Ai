package com.asifqureshi.gitforge.data.remote.github.dto;

import java.util.List;

public class CommitRequest {
    public final String message;
    public final String tree;
    public final List<String> parents;

    public CommitRequest(String message, String tree, List<String> parents) {
        this.message = message;
        this.tree = tree;
        this.parents = parents;
    }
}
