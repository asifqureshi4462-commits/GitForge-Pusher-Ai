package com.asifqureshi.gitforge.data.repository;

import androidx.annotation.NonNull;

import com.asifqureshi.gitforge.data.model.Branch;
import com.asifqureshi.gitforge.data.model.Page;
import com.asifqureshi.gitforge.data.model.Repo;
import com.asifqureshi.gitforge.data.model.UserProfile;
import com.asifqureshi.gitforge.data.remote.github.GitHubClient;
import com.asifqureshi.gitforge.domain.github.ApiException;
import com.asifqureshi.gitforge.domain.github.RemoteTree;
import com.asifqureshi.gitforge.domain.upload.UploadEngine;

/** Facade over the single {@link GitHubClient}. All methods block: call them off the main thread. */
public final class GitHubRepository {

    /** Head commit of a branch plus the file tree at that commit. */
    public static final class RemoteSnapshot {
        public final String headSha;
        public final RemoteTree tree;

        RemoteSnapshot(String headSha, RemoteTree tree) {
            this.headSha = headSha;
            this.tree = tree;
        }
    }

    private final GitHubClient client;

    public GitHubRepository(@NonNull GitHubClient client) {
        this.client = client;
    }

    @NonNull
    public UserProfile getProfile() throws ApiException {
        return client.getProfile();
    }

    @NonNull
    public Page<Repo> getRepos(int page) throws ApiException {
        return client.listRepos(page);
    }

    @NonNull
    public Page<Branch> getBranches(@NonNull Repo repo, int page) throws ApiException {
        return client.listBranches(repo.getOwner(), repo.getName(), page);
    }

    /** Creates a branch pointing at the current head of {@code fromBranch}. Returns that head commit sha. */
    @NonNull
    public String createBranch(@NonNull Repo repo, @NonNull String newBranch, @NonNull String fromBranch)
            throws ApiException {
        String head = client.getBranchHeadSha(repo.getOwner(), repo.getName(), fromBranch);
        client.createBranch(repo.getOwner(), repo.getName(), newBranch, head);
        return head;
    }

    @NonNull
    public RemoteSnapshot loadSnapshot(@NonNull Repo repo, @NonNull String branch) throws ApiException {
        String head = client.getBranchHeadSha(repo.getOwner(), repo.getName(), branch);
        String treeSha = client.getCommitTreeSha(repo.getOwner(), repo.getName(), head);
        return new RemoteSnapshot(head, client.getTree(repo.getOwner(), repo.getName(), treeSha));
    }

    @NonNull
    public UploadEngine newUploadEngine() {
        return new UploadEngine(client);
    }
}
