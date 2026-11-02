package com.asifqureshi.gitforge.ui.home;

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

import com.asifqureshi.gitforge.GitForgeApp;
import com.asifqureshi.gitforge.R;
import com.asifqureshi.gitforge.core.Resource;
import com.asifqureshi.gitforge.data.remote.ai.AiProviderType;
import com.asifqureshi.gitforge.data.repository.AiRepository;
import com.asifqureshi.gitforge.databinding.FragmentHomeBinding;
import com.asifqureshi.gitforge.ui.common.RepoAdapter;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        RepoAdapter adapter = new RepoAdapter(repo -> {
            viewModel.select(repo);
            NavHostFragment.findNavController(this).navigate(R.id.repoDetailFragment);
        });
        binding.recentList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recentList.setNestedScrollingEnabled(false);
        binding.recentList.setAdapter(adapter);

        binding.btnUpload.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.uploadFragment));
        binding.btnRetry.setOnClickListener(v -> viewModel.load());

        viewModel.getProfile().observe(getViewLifecycleOwner(), res -> {
            if (res == null) {
                return;
            }
            if (res.getStatus() == Resource.Status.SUCCESS && res.getData() != null) {
                binding.greeting.setText(getString(R.string.home_greeting, res.getData().getDisplayName()));
                binding.statRepos.setText(String.valueOf(res.getData().getRepoCount()));
                binding.statReposLabel.setText(res.getData().includesPrivateCount()
                        ? R.string.stat_repositories : R.string.stat_repositories_public);
            } else if (res.getStatus() == Resource.Status.ERROR) {
                binding.statRepos.setText(R.string.stat_unavailable);
            }
        });
        viewModel.getRecent().observe(getViewLifecycleOwner(), res -> {
            if (res == null) {
                return;
            }
            boolean loading = res.getStatus() == Resource.Status.LOADING;
            boolean error = res.getStatus() == Resource.Status.ERROR;
            binding.recentProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.errorPanel.setVisibility(error ? View.VISIBLE : View.GONE);
            if (error) {
                binding.errorText.setText(res.getErrorMessage());
            }
            if (res.getStatus() == Resource.Status.SUCCESS && res.getData() != null) {
                adapter.setItems(res.getData());
                binding.recentEmpty.setVisibility(res.getData().isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
        viewModel.getPushesToday().observe(getViewLifecycleOwner(),
                n -> binding.statPushes.setText(String.valueOf(n == null ? 0 : n)));
        viewModel.getSuccessRate().observe(getViewLifecycleOwner(),
                rate -> binding.statSuccess.setText(rate != null ? rate : getString(R.string.stat_none_yet)));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            AiRepository ai = GitForgeApp.container(requireContext()).getAi();
            AiProviderType type = ai.getProvider();
            binding.statAi.setText(ai.isConfigured() && type != null ? ai.getModel() : getString(R.string.stat_ai_not_configured));
            binding.statAiLabel.setText(ai.isConfigured() && type != null ? type.getLabel() : getString(R.string.stat_ai_provider));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
