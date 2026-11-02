package com.asifqureshi.gitforge.ui.repos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.asifqureshi.gitforge.R;
import com.asifqureshi.gitforge.data.model.Repo;
import com.asifqureshi.gitforge.data.model.Selection;
import com.asifqureshi.gitforge.databinding.DialogCreateBranchBinding;
import com.asifqureshi.gitforge.databinding.FragmentRepoDetailBinding;
import com.asifqureshi.gitforge.domain.Validators;
import com.asifqureshi.gitforge.ui.common.Browser;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class RepoDetailFragment extends Fragment {

    private FragmentRepoDetailBinding binding;
    private RepoDetailViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRepoDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(RepoDetailViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        BranchAdapter adapter = new BranchAdapter(branch -> viewModel.selectBranch(branch.getName()));
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        binding.branchList.setLayoutManager(lm);
        binding.branchList.setAdapter(adapter);
        binding.branchList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (lm.findLastVisibleItemPosition() >= adapter.getItemCount() - 5) {
                    viewModel.loadMoreIfNeeded();
                }
            }
        });

        binding.btnRetry.setOnClickListener(v -> viewModel.retry());
        binding.btnCreateBranch.setOnClickListener(v -> showCreateBranchDialog());
        binding.btnUpload.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.uploadFragment));
        binding.btnOpenGithub.setOnClickListener(v -> {
            Selection s = viewModel.getSelection().getValue();
            if (s != null) {
                Browser.open(requireContext(), s.getRepo().getHtmlUrl());
            }
        });

        viewModel.getSelection().observe(getViewLifecycleOwner(), s -> {
            if (s == null) {
                NavHostFragment.findNavController(this).navigateUp();
                return;
            }
            Repo repo = s.getRepo();
            binding.toolbar.setTitle(repo.getFullName());
            binding.repoInfo.setText(buildInfo(repo));
            adapter.setSelected(s.getBranch(), repo.getDefaultBranch());
            boolean canPush = repo.canPush();
            binding.readOnlyNote.setVisibility(canPush ? View.GONE : View.VISIBLE);
            binding.btnCreateBranch.setEnabled(canPush && s.getBranch() != null);
            binding.btnUpload.setEnabled(canPush && s.getBranch() != null);
            binding.btnUpload.setText(s.getBranch() != null
                    ? getString(R.string.repo_upload_to, s.getBranch()) : getString(R.string.repo_select_branch));
        });

        viewModel.getState().observe(getViewLifecycleOwner(), st -> {
            if (st == null) {
                return;
            }
            adapter.setItems(st.branches);
            binding.branchProgress.setVisibility(st.loading ? View.VISIBLE : View.GONE);
            binding.errorPanel.setVisibility(st.error != null ? View.VISIBLE : View.GONE);
            binding.errorText.setText(st.error);
            boolean emptyRepo = !st.loading && st.error == null && st.branches.isEmpty();
            binding.emptyText.setVisibility(emptyRepo ? View.VISIBLE : View.GONE);
        });

        viewModel.getCreating().observe(getViewLifecycleOwner(), c -> {
            if (Boolean.TRUE.equals(c)) {
                binding.btnCreateBranch.setEnabled(false);
            }
        });
        viewModel.getCreateResult().observe(getViewLifecycleOwner(), r -> {
            if (r == null) {
                return;
            }
            Toast.makeText(requireContext(),
                    r.success ? getString(R.string.branch_created, r.message) : r.message,
                    Toast.LENGTH_LONG).show();
        });
    }

    private String buildInfo(Repo repo) {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(repo.isPrivate() ? R.string.visibility_private : R.string.visibility_public));
        if (repo.getDefaultBranch() != null) {
            sb.append(" · ").append(getString(R.string.repo_default_branch, repo.getDefaultBranch()));
        }
        if (repo.isArchived()) {
            sb.append(" · ").append(getString(R.string.repo_archived));
        }
        if (repo.getDescription() != null && !repo.getDescription().isEmpty()) {
            sb.append("\n").append(repo.getDescription());
        }
        return sb.toString();
    }

    private void showCreateBranchDialog() {
        Selection s = viewModel.getSelection().getValue();
        if (s == null || s.getBranch() == null) {
            return;
        }
        DialogCreateBranchBinding d = DialogCreateBranchBinding.inflate(getLayoutInflater());
        d.fromText.setText(getString(R.string.branch_create_from, s.getBranch()));
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.branch_create_title)
                .setView(d.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_create, null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = d.nameInput.getText() != null ? d.nameInput.getText().toString().trim() : "";
            String problem = Validators.validateBranchName(name);
            if (problem != null) {
                d.nameLayout.setError(problem);
                return;
            }
            d.nameLayout.setError(null);
            viewModel.createBranch(name);
            dialog.dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
