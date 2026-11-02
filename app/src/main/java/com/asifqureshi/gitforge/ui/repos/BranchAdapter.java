package com.asifqureshi.gitforge.ui.repos;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.asifqureshi.gitforge.R;
import com.asifqureshi.gitforge.data.model.Branch;
import com.asifqureshi.gitforge.databinding.ItemBranchBinding;

import java.util.ArrayList;
import java.util.List;

public class BranchAdapter extends RecyclerView.Adapter<BranchAdapter.Holder> {

    public interface OnBranchClick {
        void onBranchClick(@NonNull Branch branch);
    }

    private final List<Branch> items = new ArrayList<>();
    private final OnBranchClick listener;
    @Nullable
    private String selected;
    @Nullable
    private String defaultBranch;

    public BranchAdapter(@NonNull OnBranchClick listener) {
        this.listener = listener;
    }

    public void setItems(@NonNull List<Branch> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void setSelected(@Nullable String selected, @Nullable String defaultBranch) {
        this.selected = selected;
        this.defaultBranch = defaultBranch;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemBranchBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Branch branch = items.get(position);
        ItemBranchBinding b = holder.binding;
        b.branchName.setText(branch.getName());
        StringBuilder tags = new StringBuilder();
        if (branch.getName().equals(defaultBranch)) {
            tags.append(b.getRoot().getContext().getString(R.string.branch_tag_default));
        }
        if (branch.isProtected()) {
            if (tags.length() > 0) {
                tags.append(" · ");
            }
            tags.append(b.getRoot().getContext().getString(R.string.branch_tag_protected));
        }
        b.branchTags.setText(tags);
        b.branchTags.setVisibility(tags.length() == 0 ? android.view.View.GONE : android.view.View.VISIBLE);
        b.getRoot().setChecked(branch.getName().equals(selected));
        b.getRoot().setOnClickListener(v -> listener.onBranchClick(branch));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemBranchBinding binding;

        Holder(ItemBranchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
