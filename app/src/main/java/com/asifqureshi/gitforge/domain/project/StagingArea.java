package com.asifqureshi.gitforge.domain.project;

import com.asifqureshi.gitforge.domain.GitBlobHasher;
import com.asifqureshi.gitforge.domain.IgnoreRules;
import com.asifqureshi.gitforge.domain.PathSanitizer;
import com.asifqureshi.gitforge.domain.SecretScanner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Copies project content into app-private storage with hard limits, so that every import path
 * (ZIP, single files, folder tree) shares the same validation. Nothing here talks to the network.
 */
public final class StagingArea {

    public static final int MAX_FILES = 5000;
    public static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    public static final long MAX_TOTAL_BYTES = 150L * 1024 * 1024;

    /** Thrown when the whole import must be aborted (too many files, too much data). */
    public static final class StagingLimitException extends Exception {
        public StagingLimitException(String message) {
            super(message);
        }
    }

    private final File root;
    private final String canonicalRoot;
    private final List<ProjectFile> files = new ArrayList<>();
    private final List<SkippedEntry> skipped = new ArrayList<>();
    private final Set<String> seenPaths = new HashSet<>();
    private long totalBytes;
    private boolean rootStripped;

    public StagingArea(File root) throws IOException {
        if (!root.isDirectory() && !root.mkdirs()) {
            throw new IOException("Could not create staging directory");
        }
        this.root = root;
        this.canonicalRoot = root.getCanonicalPath();
    }

    public File getRoot() {
        return root;
    }

    /**
     * Validates and copies one entry. The stream is read but never closed (ZIP streams are shared).
     * Entries that are merely unsafe, ignored, duplicate or too large are recorded and skipped.
     */
    public void add(String rawPath, InputStream in) throws IOException, StagingLimitException {
        String path;
        try {
            path = PathSanitizer.sanitize(rawPath);
        } catch (PathSanitizer.UnsafePathException e) {
            skipped.add(new SkippedEntry(describe(rawPath), SkipReason.UNSAFE_PATH));
            return;
        }
        if (IgnoreRules.isIgnored(path)) {
            skipped.add(new SkippedEntry(path, SkipReason.IGNORED));
            return;
        }
        if (!seenPaths.add(path)) {
            skipped.add(new SkippedEntry(path, SkipReason.DUPLICATE));
            return;
        }
        if (files.size() >= MAX_FILES) {
            throw new StagingLimitException("The project has more than " + MAX_FILES + " files.");
        }

        File dest = new File(root, path.replace('/', File.separatorChar));
        File parent = dest.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
            skipped.add(new SkippedEntry(path, SkipReason.UNREADABLE));
            return;
        }
        String canonicalDest = dest.getCanonicalPath();
        if (!canonicalDest.startsWith(canonicalRoot + File.separator)) {
            skipped.add(new SkippedEntry(path, SkipReason.UNSAFE_PATH));
            return;
        }

        long copied = 0;
        boolean tooLarge = false;
        byte[] buf = new byte[32 * 1024];
        try (OutputStream out = new FileOutputStream(dest)) {
            int n;
            while ((n = in.read(buf)) != -1) {
                copied += n;
                if (copied > MAX_FILE_BYTES) {
                    tooLarge = true;
                    break;
                }
                out.write(buf, 0, n);
            }
        }
        if (tooLarge) {
            //noinspection ResultOfMethodCallIgnored
            dest.delete();
            skipped.add(new SkippedEntry(path, SkipReason.TOO_LARGE));
            return;
        }
        if (totalBytes + copied > MAX_TOTAL_BYTES) {
            //noinspection ResultOfMethodCallIgnored
            dest.delete();
            throw new StagingLimitException("The project is larger than "
                    + (MAX_TOTAL_BYTES / (1024 * 1024)) + " MB in total.");
        }
        totalBytes += copied;
        files.add(new ProjectFile(path, dest, copied));
    }

    /** Records an entry that the caller could not read at all. */
    public void addUnreadable(String rawPath) {
        skipped.add(new SkippedEntry(describe(rawPath), SkipReason.UNREADABLE));
    }

    /** Computes blob ids and secret findings. Files with findings start unselected. */
    public void finish() throws IOException {
        for (ProjectFile f : files) {
            f.setBlobSha(GitBlobHasher.hash(f.getFile()));
            List<String> findings = SecretScanner.scan(f.getPath(), f.getFile());
            f.setSecretFindings(findings);
            f.setSelected(findings.isEmpty());
        }
        Collections.sort(files, (a, b) -> a.getPath().compareTo(b.getPath()));
    }

    /** The single top-level folder shared by every file (typical for downloaded repo ZIPs), or null. */
    public String commonRoot() {
        String candidate = null;
        for (ProjectFile f : files) {
            String p = f.getOriginalPath();
            int slash = p.indexOf('/');
            if (slash <= 0) {
                return null;
            }
            String first = p.substring(0, slash);
            if (candidate == null) {
                candidate = first;
            } else if (!candidate.equals(first)) {
                return null;
            }
        }
        return candidate;
    }

    public void setStripCommonRoot(boolean strip) {
        String common = commonRoot();
        for (ProjectFile f : files) {
            if (strip && common != null) {
                f.setPath(f.getOriginalPath().substring(common.length() + 1));
            } else {
                f.setPath(f.getOriginalPath());
            }
        }
        rootStripped = strip && common != null;
        Collections.sort(files, (a, b) -> a.getPath().compareTo(b.getPath()));
    }

    public boolean isRootStripped() {
        return rootStripped;
    }

    public List<ProjectFile> getFiles() {
        return files;
    }

    public List<SkippedEntry> getSkipped() {
        return skipped;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void deleteAll() {
        deleteRecursively(root);
    }

    private static void deleteRecursively(File f) {
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) {
                deleteRecursively(c);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    private static String describe(String raw) {
        if (raw == null) {
            return "(unnamed entry)";
        }
        String cleaned = raw.replaceAll("[\\p{Cntrl}]", "?");
        return cleaned.length() > 120 ? cleaned.substring(0, 120) + "..." : cleaned;
    }
}
