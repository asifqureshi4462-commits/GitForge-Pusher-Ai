package com.asifqureshi.gitforge.domain.github;

/** Minimal parser for GitHub's pagination Link header. */
public final class LinkHeader {

    private LinkHeader() {
    }

    /** True when the header advertises another page (rel="next"). */
    public static boolean hasNext(String header) {
        if (header == null) {
            return false;
        }
        for (String part : header.split(",")) {
            if (part.contains("rel=\"next\"")) {
                return true;
            }
        }
        return false;
    }
}
