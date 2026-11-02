package com.asifqureshi.gitforge.data.remote.github.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TreeRequest {
    @SerializedName("base_tree")
    public final String baseTree;
    public final List<Node> tree;

    public TreeRequest(String baseTree, List<Node> tree) {
        this.baseTree = baseTree;
        this.tree = tree;
    }

    public static class Node {
        public final String path;
        public final String mode;
        public final String type = "blob";
        public final String sha;

        public Node(String path, String mode, String sha) {
            this.path = path;
            this.mode = mode;
            this.sha = sha;
        }
    }
}
