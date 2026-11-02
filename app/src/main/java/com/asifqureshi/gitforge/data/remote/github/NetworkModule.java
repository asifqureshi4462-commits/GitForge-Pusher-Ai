package com.asifqureshi.gitforge.data.remote.github;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Builds the HTTP stack. TLS validation is left at the platform default and no request or response bodies or
 * headers are ever logged (there is intentionally no logging interceptor).
 */
public final class NetworkModule {

    private static final String GITHUB_API_HOST = "api.github.com";

    private NetworkModule() {
    }

    public static GitHubApi createGitHubApi(@NonNull TokenProvider tokens, @NonNull String userAgent) {
        OkHttpClient client = baseClient()
                .addInterceptor(new HeaderInterceptor(userAgent))
                .addInterceptor(new AuthInterceptor(tokens))
                .build();
        return new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GitHubApi.class);
    }

    public static DeviceFlowApi createDeviceFlowApi(@NonNull String userAgent) {
        OkHttpClient client = baseClient()
                .addInterceptor(new HeaderInterceptor(userAgent))
                .build();
        return new Retrofit.Builder()
                .baseUrl("https://github.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DeviceFlowApi.class);
    }

    /** Shared client for the AI providers (dynamic URLs, so plain OkHttp is used there). */
    public static OkHttpClient createAiHttpClient(@NonNull String userAgent) {
        return new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(new HeaderInterceptor(userAgent))
                .build();
    }

    private static OkHttpClient.Builder baseClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS);
    }

    private static final class HeaderInterceptor implements Interceptor {
        private final String userAgent;

        HeaderInterceptor(String userAgent) {
            this.userAgent = userAgent;
        }

        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request original = chain.request();
            Request.Builder b = original.newBuilder().header("User-Agent", userAgent);
            if (GITHUB_API_HOST.equals(original.url().host())) {
                b.header("Accept", "application/vnd.github+json");
                b.header("X-GitHub-Api-Version", "2022-11-28");
            }
            return chain.proceed(b.build());
        }
    }

    /** Attaches the stored token, only to api.github.com, and only if the call did not set its own. */
    private static final class AuthInterceptor implements Interceptor {
        private final TokenProvider tokens;

        AuthInterceptor(TokenProvider tokens) {
            this.tokens = tokens;
        }

        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request request = chain.request();
            if (request.header("Authorization") == null && GITHUB_API_HOST.equals(request.url().host())) {
                String token = tokens.getToken();
                if (token != null && !token.isEmpty()) {
                    request = request.newBuilder().header("Authorization", "Bearer " + token).build();
                }
            }
            return chain.proceed(request);
        }
    }
}
