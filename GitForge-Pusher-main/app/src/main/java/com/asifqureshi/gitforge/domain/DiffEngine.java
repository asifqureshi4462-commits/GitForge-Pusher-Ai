package com.asifqureshi.gitforge.domain;

import com.asifqureshi.gitforge.domain.github.RemoteTree;
import com.asifqureshi.gitforge.domain.project.FileStatus;
import com.asifqureshi.gitforge.domain.project.ProjectFile;

import java.util.List;

/** Compares staged files with the remote tree using exact Git blob ids (not file sizes). */
public final class DiffEngine {

    public static final class Summary {
        public int added;
        public int modified;
        public int unchanged;
        public int unknown;
    }

    private DiffEngine() {
    }

    /**
     * @param pathPrefix target sub-folder without leading/trailing slash, or "" for the repository root
     */
    public static Summary apply(List<ProjectFile> files, String pathPrefix, RemoteTree remote) {
        Summary s = new Summary();
        String prefix = pathPrefix == null || pathPrefix.isEmpty() ? "" : pathPrefix + "/";
        for (ProjectFile f : files) {
            String remoteSha = remote.getBlobSha(prefix + f.getPath());
            if (remoteSha == null) {
                if (remote.isTruncated()) {
                    f.setStatus(FileStatus.UNKNOWN);
                    s.unknown++;
                } else {
                    f.setStatus(FileStatus.NEW);
                    s.added++;
                }
            } else if (remoteSha.equals(f.getBlobSha())) {
                f.setStatus(FileStatus.UNCHANGED);
                s.unchanged++;
            } else {
                f.setStatus(FileStatus.MODIFIED);
                s.modified++;
            }
        }
        return s;
    }
}
