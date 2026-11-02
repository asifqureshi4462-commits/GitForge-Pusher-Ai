package com.asifqureshi.gitforge.data.local.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Local history database. Version 1. When the schema changes, bump the version and add an explicit Migration;
 * destructive fallback is intentionally NOT enabled so history is never silently wiped.
 */
@Database(entities = {OperationEntity.class}, version = 1, exportSchema = false)
public abstract class GitForgeDatabase extends RoomDatabase {

    public abstract OperationDao operationDao();

    @NonNull
    public static GitForgeDatabase create(@NonNull Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), GitForgeDatabase.class, "gitforge.db").build();
    }
}
