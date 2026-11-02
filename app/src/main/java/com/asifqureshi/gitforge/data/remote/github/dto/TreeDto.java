package com.asifqureshi.gitforge.data.remote.github.dto;

import java.util.List;

public class TreeDto {
    public String sha;
    public boolean truncated;
    public List<Item> tree;

    public static class Item {
        public String path;
        public String mode;
        public String type;
        public String sha;
    }
}
