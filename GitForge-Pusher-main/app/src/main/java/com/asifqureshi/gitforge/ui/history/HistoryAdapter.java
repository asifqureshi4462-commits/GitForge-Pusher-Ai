package com.asifqureshi.gitforge.ui.history;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asifqureshi.gitforge.R;
import com.asifqureshi.gitforge.data.local.db.OperationEntity;
import com.asifqureshi.gitforge.databinding.ItemHistoryBinding;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {

    public interface OnItemClick {
        void onItemClick(@NonNull OperationEntity entity);
    }

    private final List<OperationEntity> items = new ArrayList<>();
    private final OnItemClick listener;

    public HistoryAdapter(@NonNull OnItemClick listener) {
        this.listener = listener;
    }

    public void setItems(@NonNull List<OperationEntity> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        OperationEntity e = items.get(position);
        ItemHistoryBinding b = holder.binding;
        View root = b.getRoot();
        b.historyTitle.setText(root.getContext().getString(R.string.history_title_format, e.repoFullName, e.branch));
        if (e.commitMessage == null || e.commitMessage.isEmpty()) {
            b.historyMessage.setVisibility(View.GONE);
        } else {
            b.historyMessage.setText(e.commitMessage);
            b.historyMessage.setVisibility(View.VISIBLE);
        }
        CharSequence when = DateUtils.getRelativeTimeSpanString(e.timestampMillis, System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS);
        b.historyMeta.setText(root.getContext().getString(R.string.history_meta_format,
                typeLabel(root, e.operationType), when));
        b.historyResult.setText(resultLabel(e.result));
        int colorAttr;
        if (OperationEntity.RESULT_SUCCESS.equals(e.result)) {
            colorAttr = R.attr.gfColorSuccess;
        } else if (OperationEntity.RESULT_FAILED.equals(e.result)) {
            colorAttr = androidx.appcompat.R.attr.colorError;
        } else {
            colorAttr = R.attr.gfColorTextMuted;
        }
        b.historyResult.setTextColor(MaterialColors.getColor(root, colorAttr));
        root.setOnClickListener(v -> listener.onItemClick(e));
    }

    public static int resultLabel(String result) {
        if (OperationEntity.RESULT_SUCCESS.equals(result)) {
            return R.string.history_result_success;
        }
        if (OperationEntity.RESULT_FAILED.equals(result)) {
            return R.string.history_result_failed;
        }
        if (OperationEntity.RESULT_CANCELLED.equals(result)) {
            return R.string.history_result_cancelled;
        }
        return R.string.history_result_no_changes;
    }

    private static String typeLabel(View root, String type) {
        return root.getContext().getString(OperationEntity.TYPE_CREATE_BRANCH.equals(type)
                ? R.string.history_type_branch : R.string.history_type_upload);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemHistoryBinding binding;

        Holder(ItemHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
