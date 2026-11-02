package com.asifqureshi.gitforge.data.remote.github;

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

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * The single description of every api.github.com endpoint the app uses.
 * Branch names are passed pre-encoded (slashes preserved) through {@code encoded = true} path parameters.
 */
public interface GitHubApi {

    @GET("user")
    Call<UserDto> getUser();

    /** Used only to validate a token before it is stored. */
    @GET("user")
    Call<UserDto> getUserWithToken(@Header("Authorization") String authorization);

    @GET("user/repos")
    Call<List<RepoDto>> listRepos(@Query("per_page") int perPage,
                                  @Query("page") int page,
                                  @Query("sort") String sort,
                                  @Query("affiliation") String affiliation);

    @GET("repos/{owner}/{repo}/branches")
    Call<List<BranchDto>> listBranches(@Path("owner") String owner,
                                       @Path("repo") String repo,
                                       @Query("per_page") int perPage,
                                       @Query("page") int page);

    @GET("repos/{owner}/{repo}/git/ref/heads/{branch}")
    Call<RefDto> getBranchRef(@Path("owner") String owner,
                              @Path("repo") String repo,
                              @Path(value = "branch", encoded = true) String encodedBranch);

    @GET("repos/{owner}/{repo}/git/commits/{sha}")
    Call<CommitDto> getCommit(@Path("owner") String owner,
                              @Path("repo") String repo,
                              @Path("sha") String sha);

    @GET("repos/{owner}/{repo}/git/trees/{sha}")
    Call<TreeDto> getTree(@Path("owner") String owner,
                          @Path("repo") String repo,
                          @Path("sha") String sha,
                          @Query("recursive") String recursive);

    @POST("repos/{owner}/{repo}/git/blobs")
    Call<ShaDto> createBlob(@Path("owner") String owner,
                            @Path("repo") String repo,
                            @Body BlobRequest body);

    @POST("repos/{owner}/{repo}/git/trees")
    Call<ShaDto> createTree(@Path("owner") String owner,
                            @Path("repo") String repo,
                            @Body TreeRequest body);

    @POST("repos/{owner}/{repo}/git/commits")
    Call<ShaDto> createCommit(@Path("owner") String owner,
                              @Path("repo") String repo,
                              @Body CommitRequest body);

    @PATCH("repos/{owner}/{repo}/git/refs/heads/{branch}")
    Call<RefDto> updateBranchRef(@Path("owner") String owner,
                                 @Path("repo") String repo,
                                 @Path(value = "branch", encoded = true) String encodedBranch,
                                 @Body UpdateRefRequest body);

    @POST("repos/{owner}/{repo}/git/refs")
    Call<RefDto> createRef(@Path("owner") String owner,
                           @Path("repo") String repo,
                           @Body CreateRefRequest body);
}
