package com.alexvasilkov.events.sample.data.api;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GitHubApi {

    private static final String BASE_URL = "https://api.github.com/";

    private static final GitHubApiService service;

    static {
        Interceptor errorsInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Response response = chain.proceed(chain.request());
                if (response.isSuccessful()) {
                    return response;
                } else {
                    throw new IOException("HTTP error code: " + response.code());
                }
            }
        };
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(errorsInterceptor)
                .build();

        service = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GitHubApiService.class);
    }

    private GitHubApi() {}

    public static GitHubApiService get() {
        return service;
    }

}
