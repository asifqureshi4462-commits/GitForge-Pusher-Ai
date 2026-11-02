package com.asifqureshi.gitforge.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.asifqureshi.gitforge.data.local.db.OperationDao;
import com.asifqureshi.gitforge.data.local.db.OperationEntity;
import com.asifqureshi.gitforge.domain.SecretRedactor;

import java.util.List;
import java.util.concurrent.ExecutorService;

/** Local operation history (Room). Error text is redacted before it is written. */
public final class HistoryRepository {

    private final OperationDao dao;
    private final ExecutorService io;

    public HistoryRepository(@NonNull OperationDao dao, @NonNull ExecutorService io) {
        this.dao = dao;
        this.io = io;
    }

    public LiveData<List<OperationEntity>> observeAll() {
        return dao.observeAll();
    }

    public LiveData<OperationEntity> observeById(long id) {
        return dao.observeById(id);
    }

    public LiveData<Integer> observeSuccessfulUploadsSince(long sinceMillis) {
        return dao.observeSuccessfulUploadsSince(sinceMillis);
    }

    public LiveData<Integer> observeSuccessfulUploads() {
        return dao.observeSuccessfulUploads();
    }

    public LiveData<Integer> observeFailedUploads() {
        return dao.observeFailedUploads();
    }

    public void record(@NonNull OperationEntity entity) {
        if (entity.errorSummary != null) {
            entity.errorSummary = SecretRedactor.redact(entity.errorSummary);
        }
        if (entity.timestampMillis == 0) {
            entity.timestampMillis = System.currentTimeMillis();
        }
        io.execute(() -> dao.insert(entity));
    }

    public void clear() {
        io.execute(dao::deleteAll);
    }
}
