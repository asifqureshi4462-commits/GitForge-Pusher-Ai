package com.asifqureshi.gitforge.ui.ai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.asifqureshi.gitforge.R;
import com.asifqureshi.gitforge.core.Resource;
import com.asifqureshi.gitforge.databinding.FragmentAiBinding;
import com.asifqureshi.gitforge.domain.ai.AiPrompts;
import com.asifqureshi.gitforge.ui.upload.UploadViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/** README generation, code review and error explanation through the user's own AI provider. */
public class AiFragment extends Fragment {

    private FragmentAiBinding binding;
    private AiViewModel viewModel;
    private UploadViewModel upload;
    private AiPrompts.Task task = AiPrompts.Task.README;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AiViewModel.class);
        upload = new ViewModelProvider(requireActivity()).get(UploadViewModel.class);

        binding.btnConfigure.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.aiSettingsFragment));
        binding.taskGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.btn_task_review) {
                task = AiPrompts.Task.REVIEW;
            } else if (checkedId == R.id.btn_task_error) {
                task = AiPrompts.Task.EXPLAIN_ERROR;
            } else {
                task = AiPrompts.Task.README;
            }
            updateTaskUi();
        });
        binding.btnUseProject.setOnClickListener(v -> {
            List<com.asifqureshi.gitforge.domain.project.ProjectFile> files = upload.getFiles();
            if (files.isEmpty()) {
                Toast.makeText(requireContext(), R.string.ai_no_staged_project, Toast.LENGTH_LONG).show();
            } else {
                viewModel.buildDigest(files);
            }
        });
        binding.btnRun.setOnClickListener(v -> confirmAndRun());
        binding.btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("AI output", binding.outputText.getText()));
            Toast.makeText(requireContext(), R.string.ai_copied, Toast.LENGTH_SHORT).show();
        });

        viewModel.getDigest().observe(getViewLifecycleOwner(), text -> {
            if (text != null) {
                binding.inputText.setText(text);
            }
        });
        viewModel.getDigestError().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
        viewModel.getResult().observe(getViewLifecycleOwner(), res -> {
            if (res == null) {
                return;
            }
            boolean loading = res.getStatus() == Resource.Status.LOADING;
            binding.aiProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnRun.setEnabled(!loading);
            if (res.getStatus() == Resource.Status.SUCCESS) {
                binding.outputError.setVisibility(View.GONE);
                binding.outputText.setText(res.getData());
                binding.outputGroup.setVisibility(View.VISIBLE);
            } else if (res.getStatus() == Resource.Status.ERROR) {
                binding.outputGroup.setVisibility(View.GONE);
                binding.outputError.setText(res.getErrorMessage());
                binding.outputError.setVisibility(View.VISIBLE);
            }
        });
        updateTaskUi();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            boolean configured = viewModel.isConfigured();
            binding.notConfiguredCard.setVisibility(configured ? View.GONE : View.VISIBLE);
            binding.workGroup.setVisibility(configured ? View.VISIBLE : View.GONE);
        }
    }

    private void updateTaskUi() {
        switch (task) {
            case README:
                binding.inputLayout.setHint(getString(R.string.ai_hint_readme));
                break;
            case REVIEW:
                binding.inputLayout.setHint(getString(R.string.ai_hint_review));
                break;
            default:
                binding.inputLayout.setHint(getString(R.string.ai_hint_error));
                break;
        }
        binding.btnUseProject.setVisibility(task == AiPrompts.Task.EXPLAIN_ERROR ? View.GONE : View.VISIBLE);
    }

    private void confirmAndRun() {
        CharSequence raw = binding.inputText.getText();
        final String content = raw != null ? raw.toString().trim() : "";
        if (content.isEmpty()) {
            binding.inputLayout.setError(getString(R.string.ai_err_empty));
            return;
        }
        binding.inputLayout.setError(null);

        List<String> secrets = AiPrompts.findSecrets(content);
        if (!secrets.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.ai_secrets_title)
                    .setMessage(getString(R.string.ai_secrets_message, String.join(", ", secrets)))
                    .setPositiveButton(R.string.action_close, null)
                    .show();
            return;
        }
        final int sent = Math.min(content.length(), AiPrompts.MAX_INPUT_CHARS);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.ai_confirm_title)
                .setMessage(getString(R.string.ai_confirm_message, sent, viewModel.getProviderHost()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.ai_confirm_send, (d, w) -> viewModel.run(task, content))
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
