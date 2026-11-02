package com.asifqureshi.gitforge.domain.ai;

import com.asifqureshi.gitforge.domain.project.ProjectFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds a compact text summary of a staged project for AI prompts. Only files the user has selected and that
 * were NOT flagged as containing credentials are ever read, and only text files are included.
 */
public final class ProjectDigest {

    public static final int MAX_LISTED_PATHS = 300;
    public static final int MAX_FILES_WITH_CONTENT = 6;
    public static final int MAX_CHARS_PER_FILE = 3000;

    private static final String[] PRIORITY_NAMES = {
            "readme.md", "package.json", "build.gradle", "build.gradle.kts", "pom.xml", "requirements.txt",
            "pyproject.toml", "composer.json", "cargo.toml", "go.mod", "gemfile", "main.java", "main.py",
            "index.js", "index.php", "app.js", "mainactivity.java"
    };

    private ProjectDigest() {
    }

    public static String build(List<ProjectFile> files) throws IOException {
        List<ProjectFile> eligible = new ArrayList<>();
        for (ProjectFile f : files) {
            if (f.isSelected() && !f.hasSecretFindings()) {
                eligible.add(f);
            }
        }
        StringBuilder sb = new StringBuilder("Project file list:\n");
        int listed = 0;
        for (ProjectFile f : eligible) {
            if (listed++ >= MAX_LISTED_PATHS) {
                sb.append("... (").append(eligible.size() - MAX_LISTED_PATHS).append(" more files)\n");
                break;
            }
            sb.append("- ").append(f.getPath()).append('\n');
        }

        List<ProjectFile> withContent = new ArrayList<>();
        for (String wanted : PRIORITY_NAMES) {
            for (ProjectFile f : eligible) {
                String name = f.getPath().substring(f.getPath().lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
                if (name.equals(wanted) && !withContent.contains(f) && withContent.size() < MAX_FILES_WITH_CONTENT) {
                    withContent.add(f);
                }
            }
        }
        for (ProjectFile f : withContent) {
            String text = readTextHead(f.getFile());
            if (text != null) {
                sb.append("\n--- ").append(f.getPath()).append(" ---\n").append(text).append('\n');
            }
        }
        return AiPrompts.truncate(sb.toString());
    }

    /** Returns up to {@link #MAX_CHARS_PER_FILE} characters, or null if the file does not look like text. */
    static String readTextHead(File file) throws IOException {
        byte[] buf = new byte[MAX_CHARS_PER_FILE];
        int n;
        try (InputStream in = new FileInputStream(file)) {
            n = in.read(buf);
        }
        if (n <= 0) {
            return "";
        }
        for (int i = 0; i < n; i++) {
            if (buf[i] == 0) {
                return null;
            }
        }
        return new String(buf, 0, n, StandardCharsets.UTF_8);
    }
}
