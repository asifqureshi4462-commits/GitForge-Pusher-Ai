package com.asifqureshi.gitforge.data.model;

import java.util.List;

public final class Page<T> {
    private final List<T> items;
    private final boolean hasNext;

    public Page(List<T> items, boolean hasNext) {
        this.items = items;
        this.hasNext = hasNext;
    }

    public List<T> getItems() {
        return items;
    }

    public boolean hasNext() {
        return hasNext;
    }
}
