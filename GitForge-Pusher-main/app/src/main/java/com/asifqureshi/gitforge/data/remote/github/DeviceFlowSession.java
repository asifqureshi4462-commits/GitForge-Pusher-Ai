package com.asifqureshi.gitforge.data.remote.github;

import androidx.annotation.NonNull;

import com.asifqureshi.gitforge.data.remote.github.dto.DeviceCodeResponse;
import com.asifqureshi.gitforge.data.remote.github.dto.TokenResponse;
import com.asifqureshi.gitforge.domain.github.DeviceFlowLogic;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import retrofit2.Response;

/** One run of the OAuth device flow: request a code, show it, poll until authorised, expired or cancelled. */
public final class DeviceFlowSession {

    /** Callbacks arrive on a background thread. The token must never be logged or shown. */
    public interface Listener {
        void onCode(@NonNull String userCode, @NonNull String verificationUri, int expiresInSeconds);

        void onToken(@NonNull String accessToken);

        void onFailure(@NonNull String message);
    }

    private final DeviceFlowApi api;
    private final String clientId;
    private final String scope;
    private final Listener listener;
    private final ExecutorService executor;
    private volatile boolean cancelled;

    public DeviceFlowSession(@NonNull DeviceFlowApi api, @NonNull String clientId, @NonNull String scope,
                             @NonNull ExecutorService executor, @NonNull Listener listener) {
        this.api = api;
        this.clientId = clientId;
        this.scope = scope;
        this.executor = executor;
        this.listener = listener;
    }

    public void start() {
        executor.execute(this::run);
    }

    public void cancel() {
        cancelled = true;
    }

    private void run() {
        try {
            Response<DeviceCodeResponse> codeResponse = api.requestCode(clientId, scope).execute();
            DeviceCodeResponse code = codeResponse.body();
            if (!codeResponse.isSuccessful() || code == null || code.deviceCode == null || code.userCode == null) {
                String reason = code != null && code.error != null
                        ? DeviceFlowLogic.messageFor(code.error)
                        : "GitHub did not start the sign-in (HTTP " + codeResponse.code() + ").";
                listener.onFailure(reason);
                return;
            }
            int interval = Math.max(5, code.interval);
            long deadline = System.currentTimeMillis() + code.expiresIn * 1000L;
            listener.onCode(code.userCode, code.verificationUri != null ? code.verificationUri
                    : "https://github.com/login/device", code.expiresIn);

            while (!cancelled && System.currentTimeMillis() < deadline) {
                if (!sleep(interval * 1000L)) {
                    return;
                }
                if (cancelled) {
                    return;
                }
                Response<TokenResponse> pr = api.pollToken(clientId, code.deviceCode, DeviceFlowApi.GRANT_TYPE).execute();
                TokenResponse t = pr.body();
                if (t == null) {
                    listener.onFailure("GitHub returned an unexpected response (HTTP " + pr.code() + ").");
                    return;
                }
                DeviceFlowLogic.Action action = DeviceFlowLogic.classify(t.error, t.accessToken);
                switch (action) {
                    case SUCCESS:
                        listener.onToken(t.accessToken);
                        return;
                    case KEEP_POLLING:
                        break;
                    case SLOW_DOWN:
                        interval = Math.max(DeviceFlowLogic.nextIntervalSeconds(interval, action), t.interval);
                        break;
                    default:
                        listener.onFailure(DeviceFlowLogic.messageFor(t.error));
                        return;
                }
            }
            if (!cancelled) {
                listener.onFailure(DeviceFlowLogic.messageFor("expired_token"));
            }
        } catch (IOException e) {
            if (!cancelled) {
                listener.onFailure("Network error. Check your connection and try again.");
            }
        }
    }

    /** Sleeps in short steps so cancel() takes effect quickly. Returns false if cancelled. */
    private boolean sleep(long millis) {
        long end = System.currentTimeMillis() + millis;
        try {
            while (System.currentTimeMillis() < end) {
                if (cancelled) {
                    return false;
                }
                Thread.sleep(250);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }
}
