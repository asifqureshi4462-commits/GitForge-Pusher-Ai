package com.asifqureshi.gitforge.domain;

/** Paths that are never uploaded: VCS internals, OS junk and dependency folders. */
public final class IgnoreRules {

    private IgnoreRules() {
    }

    public static boolean isIgnored(String relPath) {
        String[] segs = relPath.split("/");
        for (int i = 0; i < segs.length; i++) {
            String s = segs[i];
            boolean last = i == segs.length - 1;
            if (s.equals(".git") || s.equals("__MACOSX") || s.equals("node_modules")) {
                return true;
            }
            if (last && (s.equals(".DS_Store")
                    || s.equalsIgnoreCase("Thumbs.db")
                    || s.equalsIgnoreCase("desktop.ini"))) {
                return true;
            }
        }
        return false;
    }
}
