package com.asifqureshi.gitforge.ui.upload;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.asifqureshi.gitforge.R;
import com.asifqureshi.gitforge.databinding.FragmentUploadPreviewBinding;
import com.asifqureshi.gitforge.domain.DiffEngine;
import com.asifqureshi.gitforge.ui.common.Formatters;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Step 2: show exactly what would change on GitHub, then ask for explicit confirmation. */
public class UploadPreviewFragment extends Fragment {

    private FragmentUploadPreviewBinding binding;
    private UploadViewModel viewModel;
    private FileAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUploadPreviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(UploadViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        adapter = new FileAdapter(() -> viewModel.notifySelectionChanged());
        binding.fileList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.fileList.setAdapter(adapter);
        adapter.setItems(viewModel.getFiles());

        binding.btnSelectAll.setOnClickListener(v -> {
            viewModel.selectAll(true);
            adapter.setItems(viewModel.getFiles());
        });
        binding.btnSelectNone.setOnClickListener(v -> {
            viewModel.selectAll(false);
            adapter.setItems(viewModel.getFiles());
        });
        binding.btnReview.setOnClickListener(v -> review());

        viewModel.getVersion().observe(getViewLifecycleOwner(), v -> renderSummary());
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            binding.errorText.setText(message);
            binding.errorText.setVisibility(message == null || message.isEmpty() ? View.GONE : View.VISIBLE);
        });
        viewModel.getPhase().observe(getViewLifecycleOwner(), phase -> {
            if (phase == UploadViewModel.Phase.UPLOADING) {
                NavHostFragment.findNavController(this).navigate(R.id.uploadProgressFragment);
            }
        });
    }

    private void renderSummary() {
        if (binding == null) {
            return;
        }
        UploadViewModel.Plan plan = viewModel.getPlan();
        DiffEngine.Summary diff = viewModel.getDiff();
        binding.targetText.setText(getString(R.string.upload_target,
                viewModel.getTargetRepo() != null ? viewModel.getTargetRepo().getFullName() : "",
                viewModel.getTargetBranch() != null ? viewModel.getTargetBranch() : ""));
        binding.summaryText.setText(getString(R.string.preview_summary, plan.added, plan.replaced,
                diff != null ? diff.unchanged : 0, viewModel.getSkipped().size()));

        StringBuilder warnings = new StringBuilder();
        if (plan.replaced > 0) {
            warnings.append(getString(R.string.preview_warn_replace, plan.replaced)).append('\n');
        }
        if (viewModel.isRemoteListingTruncated()) {
            warnings.append(getString(R.string.preview_warn_truncated)).append('\n');
        }
        if (plan.secretsIncluded > 0) {
            warnings.append(getString(R.string.preview_warn_secrets, plan.secretsIncluded)).append('\n');
        }
        String text = warnings.toString().trim();
        binding.warningText.setText(text);
        binding.warningText.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
        binding.btnReview.setEnabled(plan.toUpload() > 0);
        binding.btnReview.setText(plan.toUpload() > 0
                ? getString(R.string.preview_review_button, plan.toUpload())
                : getString(R.string.preview_nothing_to_upload));
    }

    private void review() {
        viewModel.clearError();
        CharSequence raw = binding.messageInput.getText();
        String message = raw != null ? raw.toString() : "";
        String problem = com.asifqureshi.gitforge.domain.Validators.validateCommitMessage(message);
        if (problem != null) {
            binding.messageLayout.setError(problem);
            return;
        }
        binding.messageLayout.setError(null);

        UploadViewModel.Plan plan = viewModel.getPlan();
        StringBuilder body = new StringBuilder();
        body.append(getString(R.string.confirm_target, viewModel.getTargetRepo().getFullName(),
                viewModel.getTargetBranch())).append("\n\n");
        body.append(getString(R.string.confirm_new, plan.added)).append('\n');
        if (plan.replaced > 0) {
            body.append(getString(R.string.confirm_replace, plan.replaced)).append('\n');
        }
        if (plan.unknown > 0) {
            body.append(getString(R.string.confirm_unknown, plan.unknown)).append('\n');
        }
        body.append(getString(R.string.confirm_size, Formatters.bytes(plan.bytes))).append('\n');
        if (plan.secretsIncluded > 0) {
            body.append('\n').append(getString(R.string.confirm_secrets, plan.secretsIncluded)).append('\n');
        }
        body.append("\n").append(getString(R.string.confirm_no_force));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_title)
                .setMessage(body.toString())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.confirm_positive, (d, w) -> viewModel.startUpload(message))
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
