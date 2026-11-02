package com.asifqureshi.gitforge.domain.github;

/** Decision logic for GitHub's OAuth device flow polling loop. */
public final class DeviceFlowLogic {

    public enum Action { SUCCESS, KEEP_POLLING, SLOW_DOWN, EXPIRED, DENIED, FAILED }

    private DeviceFlowLogic() {
    }

    public static Action classify(String error, String accessToken) {
        if (accessToken != null && !accessToken.isEmpty()) {
            return Action.SUCCESS;
        }
        if (error == null) {
            return Action.FAILED;
        }
        switch (error) {
            case "authorization_pending":
                return Action.KEEP_POLLING;
            case "slow_down":
                return Action.SLOW_DOWN;
            case "expired_token":
                return Action.EXPIRED;
            case "access_denied":
                return Action.DENIED;
            default:
                return Action.FAILED;
        }
    }

    /** GitHub requires the polling interval to grow by 5 seconds after a slow_down response. */
    public static int nextIntervalSeconds(int current, Action action) {
        int base = Math.max(current, 5);
        return action == Action.SLOW_DOWN ? base + 5 : base;
    }

    public static String messageFor(String error) {
        if (error == null) {
            return "GitHub returned an unexpected response.";
        }
        switch (error) {
            case "device_flow_disabled":
                return "Device flow is not enabled for this OAuth app. In GitHub open Settings, Developer settings, "
                        + "OAuth Apps, your app, and tick 'Enable Device Flow'.";
            case "incorrect_client_credentials":
                return "GitHub does not recognise this client ID. Check the configured GitHub client ID.";
            case "expired_token":
                return "The code expired. Start sign-in again.";
            case "access_denied":
                return "Authorization was cancelled on GitHub.";
            default:
                return "GitHub could not complete sign-in (" + error + ").";
        }
    }
}
