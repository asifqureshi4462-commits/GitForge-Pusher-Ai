package com.asifqureshi.gitforge.domain.upload;

import com.asifqureshi.gitforge.domain.github.ApiException;
import com.asifqureshi.gitforge.domain.github.GitDataClient;
import com.asifqureshi.gitforge.domain.github.RemoteTree;
import com.asifqureshi.gitforge.domain.github.TreeEntry;
import com.asifqureshi.gitforge.domain.project.FileStatus;
import com.asifqureshi.gitforge.domain.project.ProjectFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Commits a set of files to a branch with the Git Data API:
 * blobs, then trees, then one commit, then a fast-forward-only branch update.
 *
 * <p>The branch is only touched by the very last call, so every earlier failure or cancellation leaves the
 * repository exactly as it was. The last call never forces: if the branch moved meanwhile GitHub refuses it and
 * nothing is overwritten.
 */
public final class UploadEngine {

    public static final int TREE_CHUNK = 500;
    private static final String DEFAULT_MODE = "100644";

    public enum Stage { CHECKING_BRANCH, UPLOADING_FILES, CREATING_TREE, CREATING_COMMIT, UPDATING_BRANCH }

    public enum Outcome { COMMITTED, NO_CHANGES, CANCELLED }

    public interface Listener {
        void onStage(Stage stage);

        void onProgress(int doneFiles, int totalFiles, long doneBytes, long totalBytes);
    }

    public static final class CancelToken {
        private volatile boolean cancelled;

        public void cancel() {
            cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }

    public static final class Request {
        public final String owner;
        public final String repo;
        public final String branch;
        public final String expectedHeadSha;
        public final String message;
        /** Target sub-folder without surrounding slashes, or "". */
        public final String pathPrefix;
        public final List<ProjectFile> files;
        public final RemoteTree remoteTree;

        public Request(String owner, String repo, String branch, String expectedHeadSha, String message,
                       String pathPrefix, List<ProjectFile> files, RemoteTree remoteTree) {
            this.owner = owner;
            this.repo = repo;
            this.branch = branch;
            this.expectedHeadSha = expectedHeadSha;
            this.message = message;
            this.pathPrefix = pathPrefix == null ? "" : pathPrefix;
            this.files = files;
            this.remoteTree = remoteTree;
        }
    }

    public static final class Result {
        public final Outcome outcome;
        public final String commitSha;
        public final int filesCommitted;

        Result(Outcome outcome, String commitSha, int filesCommitted) {
            this.outcome = outcome;
            this.commitSha = commitSha;
            this.filesCommitted = filesCommitted;
        }
    }

    /** A failure with the stage it happened in and whether the branch may already have been changed. */
    public static final class UploadFailedException extends Exception {
        private final Stage stage;
        private final boolean branchMayHaveChanged;
        private final ApiException.Kind kind;
        private final long retryAtEpochSeconds;

        UploadFailedException(Stage stage, String message, boolean branchMayHaveChanged,
                              ApiException.Kind kind, long retryAt) {
            super(message);
            this.stage = stage;
            this.branchMayHaveChanged = branchMayHaveChanged;
            this.kind = kind;
            this.retryAtEpochSeconds = retryAt;
        }

        public Stage getStage() {
            return stage;
        }

        public boolean branchMayHaveChanged() {
            return branchMayHaveChanged;
        }

        public ApiException.Kind getKind() {
            return kind;
        }

        public long getRetryAtEpochSeconds() {
            return retryAtEpochSeconds;
        }
    }

    private final GitDataClient client;

    public UploadEngine(GitDataClient client) {
        this.client = client;
    }

    public Result run(Request req, Listener listener, CancelToken cancel) throws UploadFailedException {
        Stage stage = Stage.CHECKING_BRANCH;
        try {
            listener.onStage(stage);
            String head = client.getBranchHeadSha(req.owner, req.repo, req.branch);
            if (!head.equals(req.expectedHeadSha)) {
                throw new UploadFailedException(stage,
                        "The branch changed on GitHub after the preview was created. Nothing was uploaded. "
                                + "Run the preview again to compare with the latest content.",
                        false, ApiException.Kind.CONFLICT, 0);
            }

            List<ProjectFile> toUpload = new ArrayList<>();
            long totalBytes = 0;
            for (ProjectFile f : req.files) {
                if (f.isSelected() && f.getStatus() != FileStatus.UNCHANGED) {
                    toUpload.add(f);
                    totalBytes += f.getSize();
                }
            }
            if (toUpload.isEmpty()) {
                return new Result(Outcome.NO_CHANGES, null, 0);
            }

            String baseTree = client.getCommitTreeSha(req.owner, req.repo, head);
            String prefix = req.pathPrefix.isEmpty() ? "" : req.pathPrefix + "/";

            stage = Stage.UPLOADING_FILES;
            listener.onStage(stage);
            List<TreeEntry> entries = new ArrayList<>();
            long doneBytes = 0;
            listener.onProgress(0, toUpload.size(), 0, totalBytes);
            for (int i = 0; i < toUpload.size(); i++) {
                if (cancel.isCancelled()) {
                    return new Result(Outcome.CANCELLED, null, 0);
                }
                ProjectFile f = toUpload.get(i);
                String blobSha = client.createBlob(req.owner, req.repo, f.getFile());
                String fullPath = prefix + f.getPath();
                String mode = req.remoteTree != null ? req.remoteTree.getMode(fullPath) : null;
                entries.add(new TreeEntry(fullPath, mode != null ? mode : DEFAULT_MODE, blobSha));
                doneBytes += f.getSize();
                listener.onProgress(i + 1, toUpload.size(), doneBytes, totalBytes);
            }

            if (cancel.isCancelled()) {
                return new Result(Outcome.CANCELLED, null, 0);
            }
            stage = Stage.CREATING_TREE;
            listener.onStage(stage);
            String tree = baseTree;
            for (int start = 0; start < entries.size(); start += TREE_CHUNK) {
                if (cancel.isCancelled()) {
                    return new Result(Outcome.CANCELLED, null, 0);
                }
                List<TreeEntry> chunk = entries.subList(start, Math.min(entries.size(), start + TREE_CHUNK));
                tree = client.createTree(req.owner, req.repo, tree, chunk);
            }

            stage = Stage.CREATING_COMMIT;
            listener.onStage(stage);
            String commitSha = client.createCommit(req.owner, req.repo, req.message, tree, head);

            if (cancel.isCancelled()) {
                return new Result(Outcome.CANCELLED, null, 0);
            }
            stage = Stage.UPDATING_BRANCH;
            listener.onStage(stage);
            try {
                client.updateBranch(req.owner, req.repo, req.branch, commitSha);
            } catch (ApiException e) {
                if (e.getKind() != ApiException.Kind.NETWORK) {
                    throw e;
                }
                // Outcome unknown: the request may have been applied. Look before reporting anything.
                String now;
                try {
                    now = client.getBranchHeadSha(req.owner, req.repo, req.branch);
                } catch (ApiException verifyFailure) {
                    throw new UploadFailedException(stage,
                            "The connection was lost while updating the branch, so the result is unknown. "
                                    + "Check the repository on GitHub before trying again.",
                            true, ApiException.Kind.NETWORK, 0);
                }
                if (!commitSha.equals(now)) {
                    throw e;
                }
            }
            return new Result(Outcome.COMMITTED, commitSha, toUpload.size());
        } catch (UploadFailedException e) {
            throw e;
        } catch (ApiException e) {
            throw new UploadFailedException(stage, e.getMessage(), false, e.getKind(), e.getRetryAtEpochSeconds());
        } catch (RuntimeException e) {
            throw new UploadFailedException(stage, "Unexpected error while uploading.", false,
                    ApiException.Kind.UNKNOWN, 0);
        }
    }
}
