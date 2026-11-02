package com.asifqureshi.gitforge.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asifqureshi.gitforge.R;
import com.asifqureshi.gitforge.data.model.Repo;
import com.asifqureshi.gitforge.databinding.ItemRepoBinding;

import java.util.ArrayList;
import java.util.List;

/** Shared by the dashboard (recent repositories) and the repository browser. */
public class RepoAdapter extends RecyclerView.Adapter<RepoAdapter.Holder> {

    public interface OnRepoClick {
        void onRepoClick(@NonNull Repo repo);
    }

    private final List<Repo> items = new ArrayList<>();
    private final OnRepoClick listener;

    public RepoAdapter(@NonNull OnRepoClick listener) {
        this.listener = listener;
    }

    public void setItems(@NonNull List<Repo> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemRepoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Repo repo = items.get(position);
        ItemRepoBinding b = holder.binding;
        b.repoName.setText(repo.getFullName());
        b.repoVisibility.setText(repo.isPrivate() ? R.string.visibility_private : R.string.visibility_public);
        String description = repo.getDescription();
        b.repoDescription.setText(description);
        b.repoDescription.setVisibility(description == null || description.isEmpty() ? View.GONE : View.VISIBLE);
        String meta = repo.getDefaultBranch() != null
                ? b.getRoot().getContext().getString(R.string.repo_default_branch, repo.getDefaultBranch())
                : b.getRoot().getContext().getString(R.string.repo_no_default_branch);
        if (repo.isArchived()) {
            meta += " · " + b.getRoot().getContext().getString(R.string.repo_archived);
        }
        if (!repo.canPush()) {
            meta += " · " + b.getRoot().getContext().getString(R.string.repo_read_only);
        }
        b.repoMeta.setText(meta);
        b.getRoot().setOnClickListener(v -> listener.onRepoClick(repo));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemRepoBinding binding;

        Holder(ItemRepoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
