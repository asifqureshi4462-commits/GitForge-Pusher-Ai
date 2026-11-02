package com.asifqureshi.gitforge.data.remote.github;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.asifqureshi.gitforge.data.model.Branch;
import com.asifqureshi.gitforge.data.model.Page;
import com.asifqureshi.gitforge.data.model.Repo;
import com.asifqureshi.gitforge.data.model.TokenCheck;
import com.asifqureshi.gitforge.data.model.UserProfile;
import com.asifqureshi.gitforge.data.remote.github.dto.BlobRequest;
import com.asifqureshi.gitforge.data.remote.github.dto.BranchDto;
import com.asifqureshi.gitforge.data.remote.github.dto.CommitDto;
import com.asifqureshi.gitforge.data.remote.github.dto.CommitRequest;
import com.asifqureshi.gitforge.data.remote.github.dto.CreateRefRequest;
import com.asifqureshi.gitforge.data.remote.github.dto.RefDto;
import com.asifqureshi.gitforge.data.remote.github.dto.RepoDto;
import com.asifqureshi.gitforge.data.remote.github.dto.ShaDto;
import com.asifqureshi.gitforge.data.remote.github.dto.TreeDto;
import com.asifqureshi.gitforge.data.remote.github.dto.TreeRequest;
import com.asifqureshi.gitforge.data.remote.github.dto.UpdateRefRequest;
import com.asifqureshi.gitforge.data.remote.github.dto.UserDto;
import com.asifqureshi.gitforge.domain.github.ApiErrorMapper;
import com.asifqureshi.gitforge.domain.github.ApiException;
import com.asifqureshi.gitforge.domain.github.GitDataClient;
import com.asifqureshi.gitforge.domain.github.LinkHeader;
import com.asifqureshi.gitforge.domain.github.RefPath;
import com.asifqureshi.gitforge.domain.github.RemoteTree;
import com.asifqureshi.gitforge.domain.github.TreeEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Headers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * The only class that executes GitHub API calls. Turns every failure into an {@link ApiException}
 * with a safe message and implements the Git Data operations the upload engine needs.
 */
public final class GitHubClient implements GitDataClient {

    public static final int PAGE_SIZE = 100;

    /** Notified when a call made with the stored token is rejected with HTTP 401. */
    public interface UnauthorizedListener {
        void onTokenRejected();
    }

    private final GitHubApi api;
    @Nullable
    private volatile UnauthorizedListener unauthorizedListener;

    public GitHubClient(@NonNull GitHubApi api) {
        this.api = api;
    }

    public void setUnauthorizedListener(@Nullable UnauthorizedListener listener) {
        this.unauthorizedListener = listener;
    }

    // ---------------------------------------------------------------- account

    /** Validates a token WITHOUT storing it and without triggering the sign-out listener. */
    @NonNull
    public TokenCheck checkToken(@NonNull String token) throws ApiException {
        Response<UserDto> r = execute(api.getUserWithToken("Bearer " + token), false);
        return new TokenCheck(toProfile(r.body()), r.headers().get("X-OAuth-Scopes"));
    }

    @NonNull
    public UserProfile getProfile() throws ApiException {
        return toProfile(execute(api.getUser(), true).body());
    }

    // ----------------------------------------------------------- repositories

    @NonNull
    public Page<Repo> listRepos(int page) throws ApiException {
        Response<List<RepoDto>> r = execute(
                api.listRepos(PAGE_SIZE, page, "pushed", "owner,collaborator,organization_member"), true);
        List<Repo> out = new ArrayList<>();
        List<RepoDto> body = r.body();
        if (body != null) {
            for (RepoDto d : body) {
                if (d.name == null || d.owner == null || d.owner.login == null) {
                    continue;
                }
                out.add(new Repo(d.owner.login, d.name, d.isPrivate, d.description, d.defaultBranch,
                        d.htmlUrl != null ? d.htmlUrl : "https://github.com/" + d.owner.login + "/" + d.name,
                        d.permissions != null && d.permissions.push, d.archived));
            }
        }
        return new Page<>(out, LinkHeader.hasNext(r.headers().get("Link")));
    }

    @NonNull
    public Page<Branch> listBranches(@NonNull String owner, @NonNull String repo, int page) throws ApiException {
        Response<List<BranchDto>> r = execute(api.listBranches(owner, repo, PAGE_SIZE, page), true);
        List<Branch> out = new ArrayList<>();
        List<BranchDto> body = r.body();
        if (body != null) {
            for (BranchDto d : body) {
                if (d.name != null) {
                    out.add(new Branch(d.name, d.isProtected));
                }
            }
        }
        return new Page<>(out, LinkHeader.hasNext(r.headers().get("Link")));
    }

    /** Creates {@code refs/heads/<newBranch>} pointing at {@code fromSha}. Fails if the name already exists. */
    public void createBranch(@NonNull String owner, @NonNull String repo, @NonNull String newBranch,
                             @NonNull String fromSha) throws ApiException {
        execute(api.createRef(owner, repo, new CreateRefRequest("refs/heads/" + newBranch, fromSha)), true);
    }

    // -------------------------------------------------------- GitDataClient

    @Override
    public String getBranchHeadSha(String owner, String repo, String branch) throws ApiException {
        RefDto ref = execute(api.getBranchRef(owner, repo, RefPath.encode(branch)), true).body();
        if (ref == null || ref.object == null || ref.object.sha == null) {
            throw unexpected();
        }
        return ref.object.sha;
    }

    @Override
    public String getCommitTreeSha(String owner, String repo, String commitSha) throws ApiException {
        CommitDto c = execute(api.getCommit(owner, repo, commitSha), true).body();
        if (c == null || c.tree == null || c.tree.sha == null) {
            throw unexpected();
        }
        return c.tree.sha;
    }

    @Override
    public RemoteTree getTree(String owner, String repo, String treeSha) throws ApiException {
        TreeDto t = execute(api.getTree(owner, repo, treeSha, "1"), true).body();
        if (t == null) {
            throw unexpected();
        }
        RemoteTree tree = new RemoteTree(t.truncated);
        if (t.tree != null) {
            for (TreeDto.Item item : t.tree) {
                if ("blob".equals(item.type) && item.path != null && item.sha != null) {
                    tree.putBlob(item.path, item.sha, item.mode != null ? item.mode : "100644");
                }
            }
        }
        return tree;
    }

    @Override
    public String createBlob(String owner, String repo, File content) throws ApiException {
        String encoded;
        try {
            encoded = Base64.encodeToString(readAll(content), Base64.NO_WRAP);
        } catch (IOException e) {
            throw new ApiException(ApiException.Kind.UNKNOWN, 0, "A staged file could not be read: " + content.getName());
        }
        return requireSha(execute(api.createBlob(owner, repo, new BlobRequest(encoded)), true).body());
    }

    @Override
    public String createTree(String owner, String repo, String baseTreeSha, List<TreeEntry> entries)
            throws ApiException {
        List<TreeRequest.Node> nodes = new ArrayList<>(entries.size());
        for (TreeEntry e : entries) {
            nodes.add(new TreeRequest.Node(e.getPath(), e.getMode(), e.getSha()));
        }
        return requireSha(execute(api.createTree(owner, repo, new TreeRequest(baseTreeSha, nodes)), true).body());
    }

    @Override
    public String createCommit(String owner, String repo, String message, String treeSha, String parentSha)
            throws ApiException {
        CommitRequest body = new CommitRequest(message, treeSha, Collections.singletonList(parentSha));
        return requireSha(execute(api.createCommit(owner, repo, body), true).body());
    }

    @Override
    public void updateBranch(String owner, String repo, String branch, String commitSha) throws ApiException {
        execute(api.updateBranchRef(owner, repo, RefPath.encode(branch), new UpdateRefRequest(commitSha)), true);
    }

    // ---------------------------------------------------------------- helpers

    private static byte[] readAll(File file) throws IOException {
        long len = file.length();
        if (len > Integer.MAX_VALUE) {
            throw new IOException("File too large");
        }
        byte[] data = new byte[(int) len];
        int off = 0;
        try (InputStream in = new FileInputStream(file)) {
            while (off < data.length) {
                int n = in.read(data, off, data.length - off);
                if (n < 0) {
                    break;
                }
                off += n;
            }
        }
        if (off != data.length) {
            throw new IOException("Short read");
        }
        return data;
    }

    private static String requireSha(@Nullable ShaDto dto) throws ApiException {
        if (dto == null || dto.sha == null) {
            throw unexpected();
        }
        return dto.sha;
    }

    private static ApiException unexpected() {
        return new ApiException(ApiException.Kind.UNKNOWN, 0, "GitHub returned an unexpected response.");
    }

    private static UserProfile toProfile(@Nullable UserDto d) throws ApiException {
        if (d == null || d.login == null) {
            throw unexpected();
        }
        return new UserProfile(d.login, d.name, d.avatarUrl, d.publicRepos, d.totalPrivateRepos);
    }

    private <T> Response<T> execute(Call<T> call, boolean notifyOnUnauthorized) throws ApiException {
        Response<T> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            // Deliberately do not include e.getMessage(): it can contain request details.
            throw ApiException.network();
        }
        if (!response.isSuccessful()) {
            ApiException error = toException(response);
            if (notifyOnUnauthorized && error.isAuthProblem()) {
                UnauthorizedListener l = unauthorizedListener;
                if (l != null) {
                    l.onTokenRejected();
                }
            }
            throw error;
        }
        return response;
    }

    private static ApiException toException(Response<?> response) {
        String body = null;
        try (ResponseBody eb = response.errorBody()) {
            if (eb != null) {
                body = eb.string();
            }
        } catch (IOException ignored) {
            // The status code alone is enough to classify the failure.
        }
        Headers h = response.headers();
        return ApiErrorMapper.map(response.code(), body,
                h.get("X-RateLimit-Remaining"), h.get("X-RateLimit-Reset"), h.get("Retry-After"));
    }
}
