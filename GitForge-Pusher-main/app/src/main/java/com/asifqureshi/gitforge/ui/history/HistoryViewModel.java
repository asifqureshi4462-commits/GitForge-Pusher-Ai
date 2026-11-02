package com.asifqureshi.gitforge.ui.history;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.asifqureshi.gitforge.GitForgeApp;
import com.asifqureshi.gitforge.data.local.db.OperationEntity;
import com.asifqureshi.gitforge.data.repository.HistoryRepository;

import java.util.List;

public class HistoryViewModel extends AndroidViewModel {

    private final HistoryRepository repository;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        repository = GitForgeApp.container(application).getHistory();
    }

    public LiveData<List<OperationEntity>> getOperations() {
        return repository.observeAll();
    }

    public void clear() {
        repository.clear();
    }
}
