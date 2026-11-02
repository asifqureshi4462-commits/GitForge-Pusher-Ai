package com.asifqureshi.gitforge.ui.history;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.asifqureshi.gitforge.R;
import com.asifqureshi.gitforge.data.local.db.OperationEntity;
import com.asifqureshi.gitforge.databinding.FragmentHistoryBinding;
import com.asifqureshi.gitforge.ui.common.Browser;
import com.asifqureshi.gitforge.ui.common.Formatters;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Date;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private HistoryViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        HistoryAdapter adapter = new HistoryAdapter(this::showDetails);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);

        binding.btnClear.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.history_clear_title)
                .setMessage(R.string.history_clear_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.history_clear_confirm, (d, w) -> viewModel.clear())
                .show());

        viewModel.getOperations().observe(getViewLifecycleOwner(), list -> {
            adapter.setItems(list);
            boolean empty = list.isEmpty();
            binding.emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.btnClear.setEnabled(!empty);
        });
    }

    private void showDetails(@NonNull OperationEntity e) {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.history_detail_repo, e.repoFullName, e.branch)).append('\n');
        sb.append(getString(R.string.history_detail_result, getString(HistoryAdapter.resultLabel(e.result)))).append('\n');
        sb.append(getString(R.string.history_detail_when,
                DateFormat.getMediumDateFormat(requireContext()).format(new Date(e.timestampMillis)) + " "
                        + DateFormat.getTimeFormat(requireContext()).format(new Date(e.timestampMillis)))).append('\n');
        if (e.commitMessage != null && !e.commitMessage.isEmpty()) {
            sb.append(getString(R.string.history_detail_message, e.commitMessage)).append('\n');
        }
        if (OperationEntity.TYPE_UPLOAD.equals(e.operationType)) {
            sb.append(getString(R.string.history_detail_files, e.fileCount, Formatters.bytes(e.totalBytes))).append('\n');
        }
        if (e.stage != null && OperationEntity.RESULT_FAILED.equals(e.result)) {
            sb.append(getString(R.string.history_detail_stage, e.stage)).append('\n');
        }
        if (e.errorSummary != null && !e.errorSummary.isEmpty()) {
            sb.append(getString(R.string.history_detail_error, e.errorSummary)).append('\n');
        }
        if (e.branchMayHaveChanged) {
            sb.append('\n').append(getString(R.string.history_detail_uncertain)).append('\n');
        }
        if (e.commitSha != null) {
            sb.append(getString(R.string.history_detail_sha, e.commitSha)).append('\n');
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(OperationEntity.TYPE_CREATE_BRANCH.equals(e.operationType)
                        ? R.string.history_type_branch : R.string.history_type_upload)
                .setMessage(sb.toString().trim())
                .setPositiveButton(R.string.action_close, null);
        if (e.commitSha != null && e.repoFullName != null) {
            final String url = "https://github.com/" + e.repoFullName + "/commit/" + e.commitSha;
            builder.setNeutralButton(R.string.history_open_commit, (d, w) -> Browser.open(requireContext(), url));
        }
        builder.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
