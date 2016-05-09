package com.alexvasilkov.events.sample.data.api;

import com.alexvasilkov.events.sample.data.api.model.JsonGitBlob;
import com.alexvasilkov.events.sample.data.api.model.JsonGitTree;
import com.alexvasilkov.events.sample.data.api.model.JsonRepositories;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface GitHubApiService {

    @GET("search/repositories")
    Call<JsonRepositories> search(@Query("q") String query);

    @GET("repos/{user}/{repo}/git/trees/{sha}")
    Call<JsonGitTree> gitTree(@Path("user") String user,
            @Path("repo") String repo, @Path("sha") String sha);

    @GET("repos/{user}/{repo}/git/blobs/{sha}")
    Call<JsonGitBlob> gitBlob(@Path("user") String user,
            @Path("repo") String repo, @Path("sha") String sha);

}
