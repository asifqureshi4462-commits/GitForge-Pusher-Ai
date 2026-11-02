package com.asifqureshi.gitforge.ui.upload;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.asifqureshi.gitforge.R;
import com.asifqureshi.gitforge.data.model.Repo;
import com.asifqureshi.gitforge.data.model.Selection;
import com.asifqureshi.gitforge.databinding.FragmentUploadBinding;
import com.asifqureshi.gitforge.domain.project.SkippedEntry;
import com.asifqureshi.gitforge.ui.common.Formatters;

import java.util.List;

/** Step 1: choose the project (ZIP, files or folder) through the Storage Access Framework. */
public class UploadFragment extends Fragment {

    private static final String[] ZIP_MIME_TYPES = {
            "application/zip", "application/x-zip-compressed", "application/octet-stream"
    };

    private FragmentUploadBinding binding;
    private UploadViewModel viewModel;

    private final ActivityResultLauncher<String[]> zipPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null && viewModel != null) {
                    viewModel.importZip(uri);
                }
            });

    private final ActivityResultLauncher<String[]> filesPicker = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris != null && !uris.isEmpty() && viewModel != null) {
                    viewModel.importFiles(uris);
                }
            });

    private final ActivityResultLauncher<Uri> folderPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(), uri -> {
                if (uri != null && viewModel != null) {
                    viewModel.importFolder(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUploadBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(UploadViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        binding.btnPickZip.setOnClickListener(v -> zipPicker.launch(ZIP_MIME_TYPES));
        binding.btnPickFiles.setOnClickListener(v -> filesPicker.launch(new String[]{"*/*"}));
        binding.btnPickFolder.setOnClickListener(v -> folderPicker.launch(null));
        binding.btnChooseTarget.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.repositoriesFragment));
        binding.checkStripRoot.setOnCheckedChangeListener((b, checked) -> {
            if (b.isPressed()) {
                viewModel.setStripCommonRoot(checked);
            }
        });
        binding.btnCompare.setOnClickListener(v -> {
            CharSequence folder = binding.folderInput.getText();
            viewModel.analyze(folder != null ? folder.toString() : "");
        });
        binding.btnDiscard.setOnClickListener(v -> viewModel.reset());

        viewModel.getSelection().observe(getViewLifecycleOwner(), s -> renderTarget(s));
        viewModel.getPhase().observe(getViewLifecycleOwner(), phase -> render());
        viewModel.getVersion().observe(getViewLifecycleOwner(), v -> render());
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            binding.errorText.setText(message);
            binding.errorText.setVisibility(message == null || message.isEmpty() ? View.GONE : View.VISIBLE);
        });
        viewModel.getOpenPreview().observe(getViewLifecycleOwner(), open -> {
            if (Boolean.TRUE.equals(open)) {
                viewModel.consumeOpenPreview();
                NavHostFragment.findNavController(this).navigate(R.id.uploadPreviewFragment);
            }
        });
    }

    private void renderTarget(@Nullable Selection s) {
        if (s == null || s.getBranch() == null) {
            binding.targetText.setText(R.string.upload_no_target);
            binding.btnChooseTarget.setText(R.string.upload_choose_target);
            binding.targetWarning.setVisibility(View.GONE);
        } else {
            Repo repo = s.getRepo();
            binding.targetText.setText(getString(R.string.upload_target, repo.getFullName(), s.getBranch()));
            binding.btnChooseTarget.setText(R.string.upload_change_target);
            binding.targetWarning.setVisibility(repo.canPush() ? View.GONE : View.VISIBLE);
        }
        render();
    }

    private void render() {
        if (binding == null) {
            return;
        }
        Selection s = viewModel.getSelection().getValue();
        boolean targetOk = s != null && s.getBranch() != null && s.getRepo().canPush();
        UploadViewModel.Phase phase = viewModel.getPhase().getValue();
        boolean busy = phase == UploadViewModel.Phase.IMPORTING || phase == UploadViewModel.Phase.ANALYZING;
        boolean staged = phase == UploadViewModel.Phase.STAGED || phase == UploadViewModel.Phase.ANALYZING
                || phase == UploadViewModel.Phase.PREVIEW;

        binding.pickGroup.setVisibility(staged ? View.GONE : View.VISIBLE);
        binding.btnPickZip.setEnabled(targetOk && !busy);
        binding.btnPickFiles.setEnabled(targetOk && !busy);
        binding.btnPickFolder.setEnabled(targetOk && !busy);
        binding.busyPanel.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.busyText.setText(phase == UploadViewModel.Phase.ANALYZING
                ? R.string.upload_comparing : R.string.upload_importing);
        binding.stagedCard.setVisibility(staged ? View.VISIBLE : View.GONE);
        if (!staged) {
            return;
        }

        int count = viewModel.getFiles().size();
        binding.stagedSummary.setText(getString(R.string.upload_staged_summary, count,
                Formatters.bytes(viewModel.getTotalBytes())));

        List<SkippedEntry> skipped = viewModel.getSkipped();
        if (skipped.isEmpty()) {
            binding.skippedText.setVisibility(View.GONE);
        } else {
            StringBuilder sb = new StringBuilder(getString(R.string.upload_skipped_header, skipped.size()));
            for (int i = 0; i < Math.min(5, skipped.size()); i++) {
                sb.append("\n• ").append(skipped.get(i).getPath()).append(" — ")
                        .append(skipped.get(i).getReason().getDescription());
            }
            if (skipped.size() > 5) {
                sb.append("\n• …");
            }
            binding.skippedText.setText(sb.toString());
            binding.skippedText.setVisibility(View.VISIBLE);
        }

        String root = viewModel.getCommonRoot();
        binding.checkStripRoot.setVisibility(root != null ? View.VISIBLE : View.GONE);
        if (root != null) {
            binding.checkStripRoot.setText(getString(R.string.upload_strip_root, root));
            binding.checkStripRoot.setChecked(viewModel.isRootStripped());
        }
        binding.btnCompare.setEnabled(targetOk && phase == UploadViewModel.Phase.STAGED);
        binding.btnDiscard.setEnabled(phase == UploadViewModel.Phase.STAGED);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
