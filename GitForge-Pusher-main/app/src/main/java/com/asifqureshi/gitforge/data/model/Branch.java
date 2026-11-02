package com.asifqureshi.gitforge.data.model;

public final class Branch {
    private final String name;
    private final boolean isProtected;

    public Branch(String name, boolean isProtected) {
        this.name = name;
        this.isProtected = isProtected;
    }

    public String getName() {
        return name;
    }

    public boolean isProtected() {
        return isProtected;
    }
}
