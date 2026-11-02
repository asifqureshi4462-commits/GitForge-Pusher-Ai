package com.asifqureshi.gitforge.data.local.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface OperationDao {

    @Insert
    long insert(OperationEntity entity);

    @Query("SELECT * FROM operation_history ORDER BY timestampMillis DESC")
    LiveData<List<OperationEntity>> observeAll();

    @Query("SELECT * FROM operation_history WHERE id = :id")
    LiveData<OperationEntity> observeById(long id);

    @Query("SELECT COUNT(*) FROM operation_history WHERE operationType = 'UPLOAD' AND result = 'SUCCESS' "
            + "AND timestampMillis >= :sinceMillis")
    LiveData<Integer> observeSuccessfulUploadsSince(long sinceMillis);

    @Query("SELECT COUNT(*) FROM operation_history WHERE operationType = 'UPLOAD' AND result = 'SUCCESS'")
    LiveData<Integer> observeSuccessfulUploads();

    @Query("SELECT COUNT(*) FROM operation_history WHERE operationType = 'UPLOAD' AND result = 'FAILED'")
    LiveData<Integer> observeFailedUploads();

    @Query("DELETE FROM operation_history")
    void deleteAll();
}
