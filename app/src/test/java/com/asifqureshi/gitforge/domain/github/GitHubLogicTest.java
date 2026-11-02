package com.asifqureshi.gitforge.domain.github;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GitHubLogicTest {

    @Test
    public void unauthorizedMapsToAuthProblem() {
        ApiException e = ApiErrorMapper.map(401, "{\"message\":\"Bad credentials\"}", null, null, null);
        assertEquals(ApiException.Kind.UNAUTHORIZED, e.getKind());
        assertTrue(e.isAuthProblem());
    }

    @Test
    public void rateLimitIsDistinguishedFromPermissionDenied() {
        ApiException limited = ApiErrorMapper.map(403, "{\"message\":\"API rate limit exceeded\"}", "0", "1900000000", null);
        assertEquals(ApiException.Kind.RATE_LIMITED, limited.getKind());
        assertEquals(1900000000L, limited.getRetryAtEpochSeconds());
        ApiException secondary = ApiErrorMapper.map(403, "{\"message\":\"You have exceeded a secondary rate limit\"}", "50", null, "30");
        assertEquals(ApiException.Kind.RATE_LIMITED, secondary.getKind());
        ApiException denied = ApiErrorMapper.map(403, "{\"message\":\"Resource not accessible by personal access token\"}", "4000", null, null);
        assertEquals(ApiException.Kind.FORBIDDEN, denied.getKind());
    }

    @Test
    public void emptyRepositoryAndFastForwardAreRecognised() {
        assertEquals(ApiException.Kind.EMPTY_REPOSITORY,
                ApiErrorMapper.map(409, "{\"message\":\"Git Repository is empty.\"}", null, null, null).getKind());
        ApiException ff = ApiErrorMapper.map(422, "{\"message\":\"Update is not a fast forward\"}", null, null, null);
        assertEquals(ApiException.Kind.CONFLICT, ff.getKind());
        assertEquals(ApiException.Kind.VALIDATION,
                ApiErrorMapper.map(422, "{\"message\":\"Reference already exists\"}", null, null, null).getKind());
        assertEquals(ApiException.Kind.NOT_FOUND, ApiErrorMapper.map(404, "", null, null, null).getKind());
        assertEquals(ApiException.Kind.SERVER, ApiErrorMapper.map(502, "", null, null, null).getKind());
    }

    @Test
    public void errorMessagesNeverEchoTokens() {
        String token = "ghp_" + "a1B2c3D4e5F6g7H8i9J0k1L2m3N4o5P6q7R8";
        ApiException e = ApiErrorMapper.map(403, "{\"message\":\"nope " + token + "\"}", "10", null, null);
        assertFalse(e.getMessage().contains(token));
    }

    @Test
    public void linkHeaderNext() {
        assertTrue(LinkHeader.hasNext("<https://api.github.com/x?page=2>; rel=\"next\", <https://api.github.com/x?page=5>; rel=\"last\""));
        assertFalse(LinkHeader.hasNext("<https://api.github.com/x?page=1>; rel=\"prev\""));
        assertFalse(LinkHeader.hasNext(null));
    }

    @Test
    public void deviceFlowClassificationAndInterval() {
        assertEquals(DeviceFlowLogic.Action.SUCCESS, DeviceFlowLogic.classify(null, "tok"));
        assertEquals(DeviceFlowLogic.Action.KEEP_POLLING, DeviceFlowLogic.classify("authorization_pending", null));
        assertEquals(DeviceFlowLogic.Action.SLOW_DOWN, DeviceFlowLogic.classify("slow_down", null));
        assertEquals(DeviceFlowLogic.Action.EXPIRED, DeviceFlowLogic.classify("expired_token", null));
        assertEquals(DeviceFlowLogic.Action.DENIED, DeviceFlowLogic.classify("access_denied", null));
        assertEquals(DeviceFlowLogic.Action.FAILED, DeviceFlowLogic.classify("device_flow_disabled", null));
        assertEquals(10, DeviceFlowLogic.nextIntervalSeconds(5, DeviceFlowLogic.Action.SLOW_DOWN));
        assertEquals(5, DeviceFlowLogic.nextIntervalSeconds(5, DeviceFlowLogic.Action.KEEP_POLLING));
        assertTrue(DeviceFlowLogic.messageFor("device_flow_disabled").contains("Enable Device Flow"));
    }
}
