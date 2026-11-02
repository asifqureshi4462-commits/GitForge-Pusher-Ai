package com.asifqureshi.gitforge.ui.repos;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.asifqureshi.gitforge.R;
import com.asifqureshi.gitforge.databinding.FragmentRepositoriesBinding;
import com.asifqureshi.gitforge.ui.common.RepoAdapter;

public class RepositoriesFragment extends Fragment {

    private FragmentRepositoriesBinding binding;
    private RepoListViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRepositoriesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(RepoListViewModel.class);

        RepoAdapter adapter = new RepoAdapter(repo -> {
            viewModel.select(repo);
            NavHostFragment.findNavController(this).navigate(R.id.repoDetailFragment);
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.list.setLayoutManager(layoutManager);
        binding.list.setAdapter(adapter);
        binding.list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (layoutManager.findLastVisibleItemPosition() >= adapter.getItemCount() - 5) {
                    viewModel.loadMoreIfNeeded();
                }
            }
        });

        binding.swipe.setOnRefreshListener(() -> viewModel.refresh());
        binding.btnRetry.setOnClickListener(v -> viewModel.retry());
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        binding.filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chip_public) {
                viewModel.setFilter(RepoListViewModel.Filter.PUBLIC);
            } else if (id == R.id.chip_private) {
                viewModel.setFilter(RepoListViewModel.Filter.PRIVATE);
            } else {
                viewModel.setFilter(RepoListViewModel.Filter.ALL);
            }
        });

        viewModel.getState().observe(getViewLifecycleOwner(), s -> {
            if (s == null) {
                return;
            }
            adapter.setItems(s.visible);
            if (!s.loading) {
                binding.swipe.setRefreshing(false);
            }
            binding.loadingFirst.setVisibility(s.loading && s.visible.isEmpty() ? View.VISIBLE : View.GONE);
            binding.errorPanel.setVisibility(s.error != null ? View.VISIBLE : View.GONE);
            binding.errorText.setText(s.error);
            boolean empty = !s.loading && s.error == null && s.visible.isEmpty();
            binding.emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.emptyText.setText(s.loadedAnything && binding.searchInput.length() == 0
                    && binding.filterGroup.getCheckedChipId() == R.id.chip_all
                    ? R.string.repos_empty_none : R.string.repos_empty_no_match);
            binding.loadingMore.setVisibility(s.loading && !s.visible.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
