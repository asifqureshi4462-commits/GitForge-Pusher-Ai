package com.asifqureshi.gitforge.domain;

import static org.junit.Assert.assertEquals;

import com.asifqureshi.gitforge.domain.github.RemoteTree;
import com.asifqureshi.gitforge.domain.project.FileStatus;
import com.asifqureshi.gitforge.domain.project.ProjectFile;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class DiffEngineTest {

    private static ProjectFile file(String path, String sha, long size) {
        ProjectFile f = new ProjectFile(path, new File(path), size);
        f.setBlobSha(sha);
        return f;
    }

    @Test
    public void classifiesNewModifiedUnchangedBySha() {
        RemoteTree remote = new RemoteTree(false);
        remote.putBlob("same.txt", "aaa", "100644");
        remote.putBlob("edited.txt", "bbb", "100644");
        List<ProjectFile> files = new ArrayList<>();
        files.add(file("same.txt", "aaa", 3));
        files.add(file("edited.txt", "ccc", 3)); // same size, different content: must be MODIFIED
        files.add(file("fresh.txt", "ddd", 3));
        DiffEngine.Summary s = DiffEngine.apply(files, "", remote);
        assertEquals(FileStatus.UNCHANGED, files.get(0).getStatus());
        assertEquals(FileStatus.MODIFIED, files.get(1).getStatus());
        assertEquals(FileStatus.NEW, files.get(2).getStatus());
        assertEquals(1, s.unchanged);
        assertEquals(1, s.modified);
        assertEquals(1, s.added);
    }

    @Test
    public void honoursTargetFolderPrefix() {
        RemoteTree remote = new RemoteTree(false);
        remote.putBlob("docs/a.txt", "aaa", "100644");
        List<ProjectFile> files = new ArrayList<>();
        files.add(file("a.txt", "aaa", 1));
        DiffEngine.apply(files, "docs", remote);
        assertEquals(FileStatus.UNCHANGED, files.get(0).getStatus());
        DiffEngine.apply(files, "", remote);
        assertEquals(FileStatus.NEW, files.get(0).getStatus());
    }

    @Test
    public void truncatedRemoteListingYieldsUnknownNotNew() {
        RemoteTree remote = new RemoteTree(true);
        List<ProjectFile> files = new ArrayList<>();
        files.add(file("x.txt", "aaa", 1));
        DiffEngine.Summary s = DiffEngine.apply(files, "", remote);
        assertEquals(FileStatus.UNKNOWN, files.get(0).getStatus());
        assertEquals(1, s.unknown);
    }
}
