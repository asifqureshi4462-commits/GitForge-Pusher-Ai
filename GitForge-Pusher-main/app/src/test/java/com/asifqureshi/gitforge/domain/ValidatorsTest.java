package com.asifqureshi.gitforge.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ValidatorsTest {

    @Test
    public void tokenValidation() {
        assertNotNull(Validators.validateToken(""));
        assertNotNull(Validators.validateToken("short"));
        assertNotNull(Validators.validateToken("abcdefghij klmnopqrstuvwxyz"));
        assertNull(Validators.validateToken("ghp_abcdefghijklmnopqrstuvwxyz0123456789"));
        assertEquals("abc", Validators.normalizeToken("  abc\n"));
    }

    @Test
    public void commitMessageValidation() {
        assertNotNull(Validators.validateCommitMessage("   "));
        assertNotNull(Validators.validateCommitMessage(null));
        assertNull(Validators.validateCommitMessage("Update project files"));
    }

    @Test
    public void branchNameValidation() {
        assertNull(Validators.validateBranchName("feature/new-ui"));
        assertNull(Validators.validateBranchName("release-1.2"));
        assertNotNull(Validators.validateBranchName(""));
        assertNotNull(Validators.validateBranchName("has space"));
        assertNotNull(Validators.validateBranchName("a..b"));
        assertNotNull(Validators.validateBranchName("/lead"));
        assertNotNull(Validators.validateBranchName("trail/"));
        assertNotNull(Validators.validateBranchName("x.lock"));
        assertNotNull(Validators.validateBranchName("a/.hidden"));
        assertNotNull(Validators.validateBranchName("bad~name"));
        assertNotNull(Validators.validateBranchName("-dash"));
        assertNotNull(Validators.validateBranchName("@"));
    }
}
