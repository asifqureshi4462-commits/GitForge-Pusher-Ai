package com.asifqureshi.gitforge.domain.upload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.asifqureshi.gitforge.domain.github.ApiException;
import com.asifqureshi.gitforge.domain.github.GitDataClient;
import com.asifqureshi.gitforge.domain.github.RemoteTree;
import com.asifqureshi.gitforge.domain.github.TreeEntry;
import com.asifqureshi.gitforge.domain.project.FileStatus;
import com.asifqureshi.gitforge.domain.project.ProjectFile;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class UploadEngineTest {

    /** Records calls and can be told to fail at a chosen step. */
    private static class FakeClient implements GitDataClient {
        String head = "head1";
        final List<String> calls = new ArrayList<>();
        final List<List<TreeEntry>> treeChunks = new ArrayList<>();
        int blobs;
        ApiException updateFailure;
        String headAfterFailedUpdate;
        boolean verifyFails;
        Runnable onBlob;

        @Override public String getBranchHeadSha(String o, String r, String b) throws ApiException {
            calls.add("head");
            if (updateFailure != null && calls.contains("update")) {
                if (verifyFails) {
                    throw ApiException.network();
                }
                return headAfterFailedUpdate;
            }
            return head;
        }

        @Override public String getCommitTreeSha(String o, String r, String c) { calls.add("commitTree"); return "tree0"; }

        @Override public RemoteTree getTree(String o, String r, String t) { return new RemoteTree(false); }

        @Override public String createBlob(String o, String r, File f) throws ApiException {
            calls.add("blob");
            if (onBlob != null) {
                onBlob.run();
            }
            return "blob" + (++blobs);
        }

        @Override public String createTree(String o, String r, String base, List<TreeEntry> e) {
            calls.add("tree");
            treeChunks.add(new ArrayList<>(e));
            return "tree" + treeChunks.size();
        }

        @Override public String createCommit(String o, String r, String m, String t, String p) {
            calls.add("commit");
            return "commit1";
        }

        @Override public void updateBranch(String o, String r, String b, String c) throws ApiException {
            calls.add("update");
            if (updateFailure != null) {
                throw updateFailure;
            }
        }
    }

    private static final UploadEngine.Listener QUIET = new UploadEngine.Listener() {
        @Override public void onStage(UploadEngine.Stage stage) { }
        @Override public void onProgress(int d, int t, long db, long tb) { }
    };

    private static ProjectFile file(String path, FileStatus status) {
        ProjectFile f = new ProjectFile(path, new File(path), 10);
        f.setBlobSha("sha-" + path);
        f.setStatus(status);
        return f;
    }

    private static UploadEngine.Request request(List<ProjectFile> files, String head) {
        return new UploadEngine.Request("o", "r", "main", head, "msg", "", files, new RemoteTree(false));
    }

    @Test
    public void commitsChangedFilesAndSkipsUnchanged() throws Exception {
        FakeClient c = new FakeClient();
        List<ProjectFile> files = new ArrayList<>();
        files.add(file("a.txt", FileStatus.NEW));
        files.add(file("b.txt", FileStatus.MODIFIED));
        files.add(file("c.txt", FileStatus.UNCHANGED));
        UploadEngine.Result r = new UploadEngine(c).run(request(files, "head1"), QUIET, new UploadEngine.CancelToken());
        assertEquals(UploadEngine.Outcome.COMMITTED, r.outcome);
        assertEquals("commit1", r.commitSha);
        assertEquals(2, r.filesCommitted);
        assertEquals(2, c.blobs);
        assertEquals("update", c.calls.get(c.calls.size() - 1));
    }

    @Test
    public void unselectedFilesAreNotUploaded() throws Exception {
        FakeClient c = new FakeClient();
        ProjectFile skipMe = file("secret.env", FileStatus.NEW);
        skipMe.setSelected(false);
        List<ProjectFile> files = new ArrayList<>();
        files.add(skipMe);
        files.add(file("ok.txt", FileStatus.NEW));
        UploadEngine.Result r = new UploadEngine(c).run(request(files, "head1"), QUIET, new UploadEngine.CancelToken());
        assertEquals(1, r.filesCommitted);
    }

    @Test
    public void nothingChangedMeansNoCommitAndNoRemoteWrites() throws Exception {
        FakeClient c = new FakeClient();
        List<ProjectFile> files = new ArrayList<>();
        files.add(file("a.txt", FileStatus.UNCHANGED));
        UploadEngine.Result r = new UploadEngine(c).run(request(files, "head1"), QUIET, new UploadEngine.CancelToken());
        assertEquals(UploadEngine.Outcome.NO_CHANGES, r.outcome);
        assertNull(r.commitSha);
        assertFalse(c.calls.contains("blob"));
        assertFalse(c.calls.contains("update"));
    }

    @Test
    public void staleBranchStopsBeforeAnyWrite() {
        FakeClient c = new FakeClient();
        c.head = "someone-else-pushed";
        List<ProjectFile> files = new ArrayList<>();
        files.add(file("a.txt", FileStatus.NEW));
        UploadEngine.UploadFailedException e = assertThrows(UploadEngine.UploadFailedException.class,
                () -> new UploadEngine(c).run(request(files, "head1"), QUIET, new UploadEngine.CancelToken()));
        assertEquals(UploadEngine.Stage.CHECKING_BRANCH, e.getStage());
        assertFalse(e.branchMayHaveChanged());
        assertFalse(c.calls.contains("blob"));
        assertFalse(c.calls.contains("update"));
    }

    @Test
    public void cancellationNeverMovesTheBranch() throws Exception {
        FakeClient c = new FakeClient();
        UploadEngine.CancelToken token = new UploadEngine.CancelToken();
        c.onBlob = token::cancel; // cancel while the first blob is uploading
        List<ProjectFile> files = new ArrayList<>();
        files.add(file("a.txt", FileStatus.NEW));
        files.add(file("b.txt", FileStatus.NEW));
        UploadEngine.Result r = new UploadEngine(c).run(request(files, "head1"), QUIET, token);
        assertEquals(UploadEngine.Outcome.CANCELLED, r.outcome);
        assertFalse(c.calls.contains("commit"));
        assertFalse(c.calls.contains("update"));
        assertEquals(1, c.blobs);
    }

    @Test
    public void largeUploadsAreSplitIntoTreeChunks() throws Exception {
        FakeClient c = new FakeClient();
        List<ProjectFile> files = new ArrayList<>();
        for (int i = 0; i < UploadEngine.TREE_CHUNK + 1; i++) {
            files.add(file("f" + i + ".txt", FileStatus.NEW));
        }
        new UploadEngine(c).run(request(files, "head1"), QUIET, new UploadEngine.CancelToken());
        assertEquals(2, c.treeChunks.size());
        assertEquals(UploadEngine.TREE_CHUNK, c.treeChunks.get(0).size());
        assertEquals(1, c.treeChunks.get(1).size());
    }

    @Test
    public void targetFolderPrefixAndRemoteModeAreApplied() throws Exception {
        FakeClient c = new FakeClient();
        RemoteTree remote = new RemoteTree(false);
        remote.putBlob("tools/run.sh", "old", "100755");
        List<ProjectFile> files = new ArrayList<>();
        files.add(file("run.sh", FileStatus.MODIFIED));
        files.add(file("new.txt", FileStatus.NEW));
        UploadEngine.Request req = new UploadEngine.Request("o", "r", "main", "head1", "m", "tools", files, remote);
        new UploadEngine(c).run(req, QUIET, new UploadEngine.CancelToken());
        List<TreeEntry> entries = c.treeChunks.get(0);
        assertEquals("tools/run.sh", entries.get(0).getPath());
        assertEquals("100755", entries.get(0).getMode());
        assertEquals("tools/new.txt", entries.get(1).getPath());
        assertEquals("100644", entries.get(1).getMode());
    }

    @Test
    public void nonFastForwardFailureIsReportedAsConflict() {
        FakeClient c = new FakeClient();
        c.updateFailure = new ApiException(ApiException.Kind.CONFLICT, 422, "moved");
        List<ProjectFile> files = new ArrayList<>();
        files.add(file("a.txt", FileStatus.NEW));
        UploadEngine.UploadFailedException e = assertThrows(UploadEngine.UploadFailedException.class,
                () -> new UploadEngine(c).run(request(files, "head1"), QUIET, new UploadEngine.CancelToken()));
        assertEquals(UploadEngine.Stage.UPDATING_BRANCH, e.getStage());
        assertEquals(ApiException.Kind.CONFLICT, e.getKind());
        assertFalse(e.branchMayHaveChanged());
    }

    @Test
    public void lostConnectionOnFinalCallIsVerifiedBeforeReportingFailure() throws Exception {
        FakeClient c = new FakeClient();
        c.updateFailure = ApiException.network();
        c.headAfterFailedUpdate = "commit1"; // the update actually succeeded
        List<ProjectFile> files = new ArrayList<>();
        files.add(file("a.txt", FileStatus.NEW));
        UploadEngine.Result r = new UploadEngine(c).run(request(files, "head1"), QUIET, new UploadEngine.CancelToken());
        assertEquals(UploadEngine.Outcome.COMMITTED, r.outcome);
    }

    @Test
    public void lostConnectionWithUnverifiableResultIsFlaggedAsUnknown() {
        FakeClient c = new FakeClient();
        c.updateFailure = ApiException.network();
        c.verifyFails = true;
        List<ProjectFile> files = new ArrayList<>();
        files.add(file("a.txt", FileStatus.NEW));
        UploadEngine.UploadFailedException e = assertThrows(UploadEngine.UploadFailedException.class,
                () -> new UploadEngine(c).run(request(files, "head1"), QUIET, new UploadEngine.CancelToken()));
        assertTrue(e.branchMayHaveChanged());
    }

    @Test
    public void apiFailureDuringBlobUploadReportsStage() {
        FakeClient c = new FakeClient() {
            @Override public String createBlob(String o, String r, File f) throws ApiException {
                throw new ApiException(ApiException.Kind.RATE_LIMITED, 403, "limit", 1234L);
            }
        };
        List<ProjectFile> files = new ArrayList<>();
        files.add(file("a.txt", FileStatus.NEW));
        UploadEngine.UploadFailedException e = assertThrows(UploadEngine.UploadFailedException.class,
                () -> new UploadEngine(c).run(request(files, "head1"), QUIET, new UploadEngine.CancelToken()));
        assertEquals(UploadEngine.Stage.UPLOADING_FILES, e.getStage());
        assertEquals(1234L, e.getRetryAtEpochSeconds());
        assertFalse(e.branchMayHaveChanged());
    }
}
