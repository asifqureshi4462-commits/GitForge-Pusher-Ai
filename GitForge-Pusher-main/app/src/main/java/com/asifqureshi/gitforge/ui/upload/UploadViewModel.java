package com.asifqureshi.gitforge.ui.upload;

import android.app.Application;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.asifqureshi.gitforge.AppContainer;
import com.asifqureshi.gitforge.GitForgeApp;
import com.asifqureshi.gitforge.R;
import com.asifqureshi.gitforge.data.local.db.OperationEntity;
import com.asifqureshi.gitforge.data.model.Repo;
import com.asifqureshi.gitforge.data.model.Selection;
import com.asifqureshi.gitforge.data.repository.GitHubRepository;
import com.asifqureshi.gitforge.domain.DiffEngine;
import com.asifqureshi.gitforge.domain.PathSanitizer;
import com.asifqureshi.gitforge.domain.Validators;
import com.asifqureshi.gitforge.domain.github.ApiException;
import com.asifqureshi.gitforge.domain.project.FileStatus;
import com.asifqureshi.gitforge.domain.project.ProjectFile;
import com.asifqureshi.gitforge.domain.project.SkippedEntry;
import com.asifqureshi.gitforge.domain.project.StagingArea;
import com.asifqureshi.gitforge.domain.project.ZipImporter;
import com.asifqureshi.gitforge.domain.upload.UploadEngine;
import com.asifqureshi.gitforge.ui.common.ErrorText;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the whole upload flow (import, compare, commit). It is scoped to the activity so the staged project survives
 * moving between the intake, preview and progress screens.
 */
public class UploadViewModel extends AndroidViewModel {

    public enum Phase { IDLE, IMPORTING, STAGED, ANALYZING, PREVIEW, UPLOADING, SUCCESS, NO_CHANGES, CANCELLED, FAILED }

    public static final class Progress {
        public final UploadEngine.Stage stage;
        public final int done;
        public final int total;
        public final long doneBytes;
        public final long totalBytes;

        Progress(UploadEngine.Stage stage, int done, int total, long doneBytes, long totalBytes) {
            this.stage = stage;
            this.done = done;
            this.total = total;
            this.doneBytes = doneBytes;
            this.totalBytes = totalBytes;
        }
    }

    public static final class Outcome {
        public final Phase phase;
        public final String message;
        @Nullable
        public final String commitUrl;

        Outcome(Phase phase, String message, @Nullable String commitUrl) {
            this.phase = phase;
            this.message = message;
            this.commitUrl = commitUrl;
        }
    }

    /** What an upload would do, for the confirmation dialog. */
    public static final class Plan {
        public int added;
        public int replaced;
        public int unknown;
        public int unchangedSkipped;
        public int secretsIncluded;
        public long bytes;

        public int toUpload() {
            return added + replaced + unknown;
        }
    }

    private interface ImportJob {
        void run(StagingArea area) throws Exception;
    }

    private static final long MAX_ZIP_BYTES = 100L * 1024 * 1024;
    private static final int MAX_FOLDER_DEPTH = 32;

    private final AppContainer container;
    private final MutableLiveData<Phase> phase = new MutableLiveData<>(Phase.IDLE);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Integer> version = new MutableLiveData<>(0);
    private final MutableLiveData<Progress> progress = new MutableLiveData<>();
    private final MutableLiveData<Outcome> outcome = new MutableLiveData<>();
    private final MutableLiveData<Boolean> openPreview = new MutableLiveData<>(false);

    private volatile StagingArea staging;
    private volatile GitHubRepository.RemoteSnapshot snapshot;
    private volatile DiffEngine.Summary diff;
    private volatile Repo targetRepo;
    private volatile String targetBranch;
    private volatile String pathPrefix = "";
    private volatile UploadEngine.CancelToken cancelToken;
    private int versionCounter;

    public UploadViewModel(@NonNull Application application) {
        super(application);
        container = GitForgeApp.container(application);
    }

    // ------------------------------------------------------------------ observable state

    public LiveData<Phase> getPhase() {
        return phase;
    }

    public LiveData<String> getError() {
        return error;
    }

    /** Bumps whenever staged files, statuses or selections change. */
    public LiveData<Integer> getVersion() {
        return version;
    }

    public LiveData<Progress> getProgress() {
        return progress;
    }

    public LiveData<Outcome> getOutcome() {
        return outcome;
    }

    public LiveData<Boolean> getOpenPreview() {
        return openPreview;
    }

    public void consumeOpenPreview() {
        openPreview.setValue(false);
    }

    public void clearError() {
        error.setValue(null);
    }

    public LiveData<Selection> getSelection() {
        return container.getSelection();
    }

    // ------------------------------------------------------------------ staged data access

    @NonNull
    public List<ProjectFile> getFiles() {
        StagingArea s = staging;
        return s != null ? s.getFiles() : Collections.<ProjectFile>emptyList();
    }

    @NonNull
    public List<SkippedEntry> getSkipped() {
        StagingArea s = staging;
        return s != null ? s.getSkipped() : Collections.<SkippedEntry>emptyList();
    }

    public long getTotalBytes() {
        StagingArea s = staging;
        return s != null ? s.getTotalBytes() : 0;
    }

    @Nullable
    public String getCommonRoot() {
        StagingArea s = staging;
        return s != null ? s.commonRoot() : null;
    }

    public boolean isRootStripped() {
        StagingArea s = staging;
        return s != null && s.isRootStripped();
    }

    @Nullable
    public DiffEngine.Summary getDiff() {
        return diff;
    }

    public boolean isRemoteListingTruncated() {
        GitHubRepository.RemoteSnapshot snap = snapshot;
        return snap != null && snap.tree.isTruncated();
    }

    @Nullable
    public Repo getTargetRepo() {
        return targetRepo;
    }

    @Nullable
    public String getTargetBranch() {
        return targetBranch;
    }

    @NonNull
    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setStripCommonRoot(boolean strip) {
        StagingArea s = staging;
        if (s != null && phase.getValue() == Phase.STAGED) {
            s.setStripCommonRoot(strip);
            bump();
        }
    }

    /** Called after a checkbox changes. */
    public void notifySelectionChanged() {
        bump();
    }

    public void selectAll(boolean selected) {
        for (ProjectFile f : getFiles()) {
            f.setSelected(selected);
        }
        bump();
    }

    @NonNull
    public Plan getPlan() {
        Plan plan = new Plan();
        for (ProjectFile f : getFiles()) {
            if (!f.isSelected()) {
                continue;
            }
            switch (f.getStatus()) {
                case NEW:
                    plan.added++;
                    break;
                case MODIFIED:
                    plan.replaced++;
                    break;
                case UNCHANGED:
                    plan.unchangedSkipped++;
                    continue;
                default:
                    plan.unknown++;
                    break;
            }
            plan.bytes += f.getSize();
            if (f.hasSecretFindings()) {
                plan.secretsIncluded++;
            }
        }
        return plan;
    }

    // ------------------------------------------------------------------ import

    public void importZip(@NonNull Uri uri) {
        startImport(true, area -> {
            long size = querySize(uri);
            if (size > MAX_ZIP_BYTES) {
                throw new ImportRejected(getApplication().getString(R.string.upload_err_zip_too_large));
            }
            try (InputStream in = openStream(uri)) {
                ZipImporter.importZip(new BufferedInputStream(in), area);
            }
        });
    }

    public void importFiles(@NonNull List<Uri> uris) {
        startImport(false, area -> {
            for (Uri uri : uris) {
                String name = queryName(uri);
                try (InputStream in = getApplication().getContentResolver().openInputStream(uri)) {
                    if (in == null || name == null) {
                        area.addUnreadable(name != null ? name : uri.toString());
                    } else {
                        area.add(name, in);
                    }
                }
            }
        });
    }

    public void importFolder(@NonNull Uri treeUri) {
        startImport(false, area -> {
            DocumentFile root = DocumentFile.fromTreeUri(getApplication(), treeUri);
            if (root == null || !root.isDirectory()) {
                throw new ImportRejected(getApplication().getString(R.string.upload_err_folder));
            }
            walk(root, "", 0, area);
        });
    }

    private void walk(DocumentFile dir, String prefix, int depth, StagingArea area) throws Exception {
        if (depth > MAX_FOLDER_DEPTH) {
            return;
        }
        for (DocumentFile child : dir.listFiles()) {
            String name = child.getName();
            if (name == null) {
                continue;
            }
            String path = prefix + name;
            if (child.isDirectory()) {
                if (com.asifqureshi.gitforge.domain.IgnoreRules.isIgnored(path + "/x")) {
                    area.add(path + "/x", new ByteArrayInputStream(new byte[0])); // recorded as ignored
                } else {
                    walk(child, path + "/", depth + 1, area);
                }
            } else {
                try (InputStream in = getApplication().getContentResolver().openInputStream(child.getUri())) {
                    if (in == null) {
                        area.addUnreadable(path);
                    } else {
                        area.add(path, in);
                    }
                }
            }
        }
    }

    private void startImport(boolean stripRootByDefault, ImportJob job) {
        Phase p = phase.getValue();
        if (p == Phase.IMPORTING || p == Phase.ANALYZING || p == Phase.UPLOADING) {
            return;
        }
        phase.setValue(Phase.IMPORTING);
        error.setValue(null);
        container.getIo().execute(() -> {
            StagingArea area = null;
            try {
                discardStaging();
                area = new StagingArea(new File(container.getStagingRoot(), "s" + System.nanoTime()));
                job.run(area);
                area.finish();
                if (area.getFiles().isEmpty()) {
                    area.deleteAll();
                    fail(getApplication().getString(R.string.upload_err_nothing));
                    return;
                }
                area.setStripCommonRoot(stripRootByDefault && area.commonRoot() != null);
                staging = area;
                snapshot = null;
                diff = null;
                bump();
                phase.postValue(Phase.STAGED);
            } catch (ImportRejected | ZipImporter.InvalidArchiveException | StagingArea.StagingLimitException e) {
                cleanup(area);
                fail(e.getMessage());
            } catch (SecurityException e) {
                cleanup(area);
                fail(getApplication().getString(R.string.upload_err_denied));
            } catch (IOException e) {
                cleanup(area);
                fail(getApplication().getString(R.string.upload_err_read));
            } catch (Exception e) {
                cleanup(area);
                fail(getApplication().getString(R.string.upload_err_unexpected));
            }
        });
    }

    // ------------------------------------------------------------------ compare with GitHub

    public void analyze(@Nullable String targetFolderRaw) {
        if (staging == null || phase.getValue() == Phase.ANALYZING) {
            return;
        }
        final Selection sel = container.getSelection().getValue();
        if (sel == null || sel.getBranch() == null) {
            error.setValue(getApplication().getString(R.string.upload_err_no_target));
            return;
        }
        if (!sel.getRepo().canPush()) {
            error.setValue(getApplication().getString(R.string.repo_read_only_note));
            return;
        }
        final String prefix;
        try {
            prefix = PathSanitizer.sanitizeFolderOrEmpty(targetFolderRaw);
        } catch (PathSanitizer.UnsafePathException e) {
            error.setValue(getApplication().getString(R.string.upload_err_folder_invalid));
            return;
        }
        phase.setValue(Phase.ANALYZING);
        error.setValue(null);
        container.getIo().execute(() -> {
            try {
                GitHubRepository.RemoteSnapshot snap = container.getGitHub().loadSnapshot(sel.getRepo(), sel.getBranch());
                snapshot = snap;
                pathPrefix = prefix;
                targetRepo = sel.getRepo();
                targetBranch = sel.getBranch();
                diff = DiffEngine.apply(staging.getFiles(), prefix, snap.tree);
                bump();
                phase.postValue(Phase.PREVIEW);
                openPreview.postValue(true);
            } catch (ApiException e) {
                error.postValue(ErrorText.describe(getApplication(), e));
                phase.postValue(Phase.STAGED);
            }
        });
    }

    // ------------------------------------------------------------------ commit

    public void startUpload(@Nullable String commitMessage) {
        String problem = Validators.validateCommitMessage(commitMessage);
        if (problem != null) {
            error.setValue(problem);
            return;
        }
        final GitHubRepository.RemoteSnapshot snap = snapshot;
        final Repo repo = targetRepo;
        final String branch = targetBranch;
        if (phase.getValue() != Phase.PREVIEW || snap == null || repo == null || branch == null || staging == null) {
            return;
        }
        final Plan plan = getPlan();
        if (plan.toUpload() == 0) {
            error.setValue(getApplication().getString(R.string.upload_err_nothing_to_upload));
            return;
        }
        final String message = commitMessage.trim();
        final UploadEngine.Request request = new UploadEngine.Request(repo.getOwner(), repo.getName(), branch,
                snap.headSha, message, pathPrefix, new ArrayList<>(staging.getFiles()), snap.tree);
        final UploadEngine.CancelToken token = new UploadEngine.CancelToken();
        cancelToken = token;
        progress.setValue(new Progress(UploadEngine.Stage.CHECKING_BRANCH, 0, plan.toUpload(), 0, plan.bytes));
        outcome.setValue(null);
        error.setValue(null);
        phase.setValue(Phase.UPLOADING);

        final OperationEntity op = new OperationEntity();
        op.repoFullName = repo.getFullName();
        op.branch = branch;
        op.operationType = OperationEntity.TYPE_UPLOAD;
        op.commitMessage = message;
        op.fileCount = plan.toUpload();
        op.totalBytes = plan.bytes;

        container.getIo().execute(() -> {
            final int[] done = {0};
            final long[] doneBytes = {0};
            UploadEngine.Listener listener = new UploadEngine.Listener() {
                @Override
                public void onStage(UploadEngine.Stage stage) {
                    progress.postValue(new Progress(stage, done[0], plan.toUpload(), doneBytes[0], plan.bytes));
                }

                @Override
                public void onProgress(int doneFiles, int totalFiles, long bytesDone, long totalBytes) {
                    done[0] = doneFiles;
                    doneBytes[0] = bytesDone;
                    progress.postValue(new Progress(UploadEngine.Stage.UPLOADING_FILES, doneFiles, totalFiles,
                            bytesDone, totalBytes));
                }
            };
            try {
                UploadEngine.Result r = container.getGitHub().newUploadEngine().run(request, listener, token);
                switch (r.outcome) {
                    case COMMITTED:
                        op.result = OperationEntity.RESULT_SUCCESS;
                        op.commitSha = r.commitSha;
                        op.stage = "DONE";
                        outcome.postValue(new Outcome(Phase.SUCCESS,
                                getApplication().getString(R.string.upload_done_message, r.filesCommitted,
                                        repo.getFullName(), branch),
                                repo.getHtmlUrl() + "/commit/" + r.commitSha));
                        phase.postValue(Phase.SUCCESS);
                        break;
                    case NO_CHANGES:
                        op.result = OperationEntity.RESULT_NO_CHANGES;
                        outcome.postValue(new Outcome(Phase.NO_CHANGES,
                                getApplication().getString(R.string.upload_no_changes_message), null));
                        phase.postValue(Phase.NO_CHANGES);
                        break;
                    default:
                        op.result = OperationEntity.RESULT_CANCELLED;
                        outcome.postValue(new Outcome(Phase.CANCELLED,
                                getApplication().getString(R.string.upload_cancelled_message), null));
                        phase.postValue(Phase.CANCELLED);
                        break;
                }
            } catch (UploadEngine.UploadFailedException e) {
                op.result = OperationEntity.RESULT_FAILED;
                op.stage = e.getStage().name();
                op.errorSummary = e.getMessage();
                op.branchMayHaveChanged = e.branchMayHaveChanged();
                String text = ErrorText.describe(getApplication(), e.getMessage(), e.getKind(),
                        e.getRetryAtEpochSeconds());
                outcome.postValue(new Outcome(Phase.FAILED, text, null));
                phase.postValue(Phase.FAILED);
            } finally {
                container.getHistory().record(op);
            }
        });
    }

    public void cancelUpload() {
        UploadEngine.CancelToken t = cancelToken;
        if (t != null) {
            t.cancel();
        }
    }

    /** Returns to the intake screen so the project can be compared again with the latest remote state. */
    public void backToIntake() {
        if (staging != null && phase.getValue() != Phase.UPLOADING) {
            snapshot = null;
            diff = null;
            for (ProjectFile f : getFiles()) {
                f.setStatus(FileStatus.UNKNOWN);
            }
            outcome.setValue(null);
            phase.setValue(Phase.STAGED);
            bump();
        }
    }

    /** Forgets the staged project and returns to the idle state. Ignored while an upload is running. */
    public void reset() {
        if (phase.getValue() == Phase.UPLOADING) {
            return;
        }
        container.getIo().execute(this::discardStaging);
        snapshot = null;
        diff = null;
        pathPrefix = "";
        outcome.setValue(null);
        error.setValue(null);
        progress.setValue(null);
        phase.setValue(Phase.IDLE);
        bump();
    }

    // ------------------------------------------------------------------ helpers

    private void discardStaging() {
        StagingArea s = staging;
        staging = null;
        if (s != null) {
            s.deleteAll();
        }
        container.clearStaging();
    }

    private void cleanup(@Nullable StagingArea area) {
        if (area != null) {
            area.deleteAll();
        }
    }

    private void fail(String message) {
        error.postValue(message);
        phase.postValue(Phase.IDLE);
    }

    private void bump() {
        versionCounter++;
        version.postValue(versionCounter);
    }

    private InputStream openStream(Uri uri) throws IOException {
        InputStream in = getApplication().getContentResolver().openInputStream(uri);
        if (in == null) {
            throw new IOException("Cannot open " + uri);
        }
        return in;
    }

    private long querySize(Uri uri) {
        ContentResolver cr = getApplication().getContentResolver();
        try (Cursor c = cr.query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) {
                return c.getLong(0);
            }
        } catch (RuntimeException ignored) {
            // Size unknown: the staging limits still apply while reading.
        }
        return -1;
    }

    @Nullable
    private String queryName(Uri uri) {
        ContentResolver cr = getApplication().getContentResolver();
        try (Cursor c = cr.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) {
                return c.getString(0);
            }
        } catch (RuntimeException ignored) {
            // Fall through to the last path segment.
        }
        return uri.getLastPathSegment();
    }

    /** A user-facing reason to stop an import (not an unexpected failure). */
    private static final class ImportRejected extends Exception {
        ImportRejected(String message) {
            super(message);
        }
    }
}
