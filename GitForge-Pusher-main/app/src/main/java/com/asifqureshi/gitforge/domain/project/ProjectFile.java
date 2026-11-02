package com.asifqureshi.gitforge.domain.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A file staged in app-private storage and (maybe) destined for the repository. */
public final class ProjectFile {

    private final String originalPath;
    private final File file;
    private final long size;

    private String path;
    private String blobSha;
    private FileStatus status = FileStatus.UNKNOWN;
    private List<String> secretFindings = new ArrayList<>();
    private boolean selected = true;

    public ProjectFile(String path, File file, long size) {
        this.originalPath = path;
        this.path = path;
        this.file = file;
        this.size = size;
    }

    /** Path inside the project after optional root-folder stripping (before any target sub-folder). */
    public String getPath() {
        return path;
    }

    void setPath(String path) {
        this.path = path;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public File getFile() {
        return file;
    }

    public long getSize() {
        return size;
    }

    public String getBlobSha() {
        return blobSha;
    }

    public void setBlobSha(String blobSha) {
        this.blobSha = blobSha;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        this.status = status;
    }

    public List<String> getSecretFindings() {
        return Collections.unmodifiableList(secretFindings);
    }

    public void setSecretFindings(List<String> findings) {
        this.secretFindings = new ArrayList<>(findings);
    }

    public boolean hasSecretFindings() {
        return !secretFindings.isEmpty();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
