package com.asifqureshi.gitforge.data.local.db;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * One recorded operation. Never contains tokens, keys or file contents: only repository, branch, commit message,
 * counts, and a redacted, truncated error summary.
 */
@Entity(tableName = "operation_history", indices = {@Index("timestampMillis")})
public class OperationEntity {

    public static final String TYPE_UPLOAD = "UPLOAD";
    public static final String TYPE_CREATE_BRANCH = "CREATE_BRANCH";

    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FAILED = "FAILED";
    public static final String RESULT_CANCELLED = "CANCELLED";
    public static final String RESULT_NO_CHANGES = "NO_CHANGES";

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String repoFullName;
    public String branch;
    public String operationType;
    @Nullable
    public String commitMessage;
    public long timestampMillis;
    public String result;
    /** Stage reached (for uploads), e.g. UPLOADING_FILES. */
    @Nullable
    public String stage;
    @Nullable
    public String errorSummary;
    @Nullable
    public String commitSha;
    public int fileCount;
    public long totalBytes;
    /** True when a failure happened at a point where the branch might already have been updated. */
    public boolean branchMayHaveChanged;
}
