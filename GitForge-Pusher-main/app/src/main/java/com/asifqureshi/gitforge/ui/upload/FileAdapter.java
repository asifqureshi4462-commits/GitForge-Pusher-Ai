package com.asifqureshi.gitforge.ui.upload;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asifqureshi.gitforge.R;
import com.asifqureshi.gitforge.databinding.ItemFileBinding;
import com.asifqureshi.gitforge.domain.project.ProjectFile;
import com.asifqureshi.gitforge.ui.common.Formatters;

import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.Holder> {

    public interface OnSelectionChanged {
        void onChanged();
    }

    private final List<ProjectFile> items = new ArrayList<>();
    private final OnSelectionChanged listener;

    public FileAdapter(@NonNull OnSelectionChanged listener) {
        this.listener = listener;
    }

    public void setItems(@NonNull List<ProjectFile> files) {
        items.clear();
        items.addAll(files);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemFileBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ProjectFile f = items.get(position);
        ItemFileBinding b = holder.binding;
        b.filePath.setText(f.getPath());
        int statusRes;
        switch (f.getStatus()) {
            case NEW:
                statusRes = R.string.file_status_new;
                break;
            case MODIFIED:
                statusRes = R.string.file_status_modified;
                break;
            case UNCHANGED:
                statusRes = R.string.file_status_unchanged;
                break;
            default:
                statusRes = R.string.file_status_unknown;
                break;
        }
        b.fileMeta.setText(b.getRoot().getContext().getString(statusRes) + " · " + Formatters.bytes(f.getSize()));
        if (f.hasSecretFindings()) {
            b.fileWarning.setText(b.getRoot().getContext().getString(R.string.file_secret_warning,
                    String.join(", ", f.getSecretFindings())));
            b.fileWarning.setVisibility(View.VISIBLE);
        } else {
            b.fileWarning.setVisibility(View.GONE);
        }
        b.fileCheck.setOnCheckedChangeListener(null);
        b.fileCheck.setChecked(f.isSelected());
        b.fileCheck.setOnCheckedChangeListener((button, checked) -> {
            f.setSelected(checked);
            listener.onChanged();
        });
        b.getRoot().setOnClickListener(v -> b.fileCheck.toggle());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemFileBinding binding;

        Holder(ItemFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
