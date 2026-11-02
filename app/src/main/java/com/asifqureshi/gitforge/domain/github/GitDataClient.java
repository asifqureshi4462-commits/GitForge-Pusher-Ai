package com.asifqureshi.gitforge.domain.github;

import java.io.File;
import java.util.List;

/** The Git Data API operations needed to commit many files atomically. Implemented over Retrofit. */
public interface GitDataClient {

    /** Commit sha at the head of the branch. */
    String getBranchHeadSha(String owner, String repo, String branch) throws ApiException;

    String getCommitTreeSha(String owner, String repo, String commitSha) throws ApiException;

    RemoteTree getTree(String owner, String repo, String treeSha) throws ApiException;

    String createBlob(String owner, String repo, File content) throws ApiException;

    String createTree(String owner, String repo, String baseTreeSha, List<TreeEntry> entries) throws ApiException;

    String createCommit(String owner, String repo, String message, String treeSha, String parentSha)
            throws ApiException;

    /** Moves the branch to {@code commitSha}. Must be fast-forward only (never forced). */
    void updateBranch(String owner, String repo, String branch, String commitSha) throws ApiException;
}
