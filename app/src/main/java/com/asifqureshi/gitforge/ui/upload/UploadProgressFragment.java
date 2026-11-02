package com.asifqureshi.gitforge.ui.upload;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.asifqureshi.gitforge.R;
import com.asifqureshi.gitforge.databinding.FragmentUploadProgressBinding;
import com.asifqureshi.gitforge.ui.common.Browser;
import com.asifqureshi.gitforge.ui.common.Formatters;

/** Step 3: real progress (files uploaded so far), cancellation, and an honest result. */
public class UploadProgressFragment extends Fragment {

    private FragmentUploadProgressBinding binding;
    private UploadViewModel viewModel;
    private boolean cancelling;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUploadProgressBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(UploadViewModel.class);
        final NavController nav = NavHostFragment.findNavController(this);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (viewModel.getPhase().getValue() == UploadViewModel.Phase.UPLOADING) {
                            Toast.makeText(requireContext(), R.string.upload_use_cancel, Toast.LENGTH_SHORT).show();
                        } else {
                            finish(nav);
                        }
                    }
                });

        binding.btnCancel.setOnClickListener(v -> {
            cancelling = true;
            binding.btnCancel.setEnabled(false);
            binding.stageText.setText(R.string.upload_stage_cancelling);
            viewModel.cancelUpload();
        });
        binding.btnDone.setOnClickListener(v -> finish(nav));
        binding.btnRetry.setOnClickListener(v -> {
            viewModel.backToIntake();
            nav.popBackStack(R.id.uploadFragment, false);
        });
        binding.btnViewCommit.setOnClickListener(v -> {
            UploadViewModel.Outcome o = viewModel.getOutcome().getValue();
            if (o != null && o.commitUrl != null) {
                Browser.open(requireContext(), o.commitUrl);
            }
        });

        viewModel.getProgress().observe(getViewLifecycleOwner(), p -> {
            if (p == null) {
                return;
            }
            binding.progressBar.setMax(Math.max(1, p.total));
            binding.progressBar.setProgress(p.done);
            binding.progressText.setText(getString(R.string.upload_progress_text, p.done, p.total,
                    Formatters.bytes(p.doneBytes), Formatters.bytes(p.totalBytes)));
            if (viewModel.getPhase().getValue() == UploadViewModel.Phase.UPLOADING) {
                if (!cancelling) {
                    binding.stageText.setText(stageLabel(p.stage));
                }
                // The branch is only touched by the final call; after that point cancelling is no longer possible.
                binding.btnCancel.setEnabled(!cancelling
                        && p.stage != com.asifqureshi.gitforge.domain.upload.UploadEngine.Stage.UPDATING_BRANCH);
            }
        });
        viewModel.getPhase().observe(getViewLifecycleOwner(), phase -> render());
        viewModel.getOutcome().observe(getViewLifecycleOwner(), o -> render());
    }

    private int stageLabel(com.asifqureshi.gitforge.domain.upload.UploadEngine.Stage stage) {
        switch (stage) {
            case CHECKING_BRANCH:
                return R.string.upload_stage_checking;
            case UPLOADING_FILES:
                return R.string.upload_stage_files;
            case CREATING_TREE:
                return R.string.upload_stage_tree;
            case CREATING_COMMIT:
                return R.string.upload_stage_commit;
            default:
                return R.string.upload_stage_branch;
        }
    }

    private void render() {
        if (binding == null) {
            return;
        }
        UploadViewModel.Phase phase = viewModel.getPhase().getValue();
        UploadViewModel.Outcome outcome = viewModel.getOutcome().getValue();
        boolean running = phase == UploadViewModel.Phase.UPLOADING;
        binding.runningGroup.setVisibility(running ? View.VISIBLE : View.GONE);
        binding.resultGroup.setVisibility(!running && outcome != null ? View.VISIBLE : View.GONE);
        if (running || outcome == null) {
            return;
        }
        int title;
        switch (outcome.phase) {
            case SUCCESS:
                title = R.string.upload_done_title;
                break;
            case NO_CHANGES:
                title = R.string.upload_no_changes_title;
                break;
            case CANCELLED:
                title = R.string.upload_cancelled_title;
                break;
            default:
                title = R.string.upload_failed_title;
                break;
        }
        binding.resultTitle.setText(title);
        binding.resultMessage.setText(outcome.message);
        binding.btnViewCommit.setVisibility(outcome.commitUrl != null ? View.VISIBLE : View.GONE);
        binding.btnRetry.setVisibility(outcome.phase == UploadViewModel.Phase.FAILED
                || outcome.phase == UploadViewModel.Phase.CANCELLED ? View.VISIBLE : View.GONE);
    }

    private void finish(NavController nav) {
        viewModel.reset();
        if (!nav.popBackStack(R.id.homeFragment, false)) {
            nav.navigate(R.id.homeFragment);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
