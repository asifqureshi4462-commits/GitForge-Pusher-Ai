package com.asifqureshi.gitforge.ui.ai;

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
import com.asifqureshi.gitforge.data.remote.ai.AiProviderType;
import com.asifqureshi.gitforge.databinding.FragmentAiSettingsBinding;
import com.asifqureshi.gitforge.domain.ai.AiValidators;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AiSettingsFragment extends Fragment {

    private FragmentAiSettingsBinding binding;
    private AiSettingsViewModel viewModel;
    private AiProviderType current = AiProviderType.OPENAI_COMPATIBLE;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AiSettingsViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        AiProviderType saved = viewModel.getSavedProvider();
        if (saved != null) {
            current = saved;
        }
        check(current);
        if (saved != null && viewModel.getSavedBaseUrl() != null) {
            binding.baseUrlInput.setText(viewModel.getSavedBaseUrl());
        } else {
            binding.baseUrlInput.setText(current.getDefaultBaseUrl());
        }
        if (saved != null && viewModel.getSavedModel() != null) {
            binding.modelInput.setText(viewModel.getSavedModel());
        }
        updateKeyHint();

        binding.providerGroup.setOnCheckedChangeListener((group, checkedId) -> {
            AiProviderType picked = checkedId == R.id.radio_anthropic ? AiProviderType.ANTHROPIC
                    : checkedId == R.id.radio_gemini ? AiProviderType.GEMINI : AiProviderType.OPENAI_COMPATIBLE;
            if (picked != current) {
                CharSequence url = binding.baseUrlInput.getText();
                if (url == null || url.length() == 0 || url.toString().equals(current.getDefaultBaseUrl())) {
                    binding.baseUrlInput.setText(picked.getDefaultBaseUrl());
                }
                current = picked;
                updateKeyHint();
            }
        });

        binding.btnSave.setOnClickListener(v -> {
            binding.baseUrlLayout.setError(null);
            binding.modelLayout.setError(null);
            binding.keyLayout.setError(null);
            viewModel.save(current, text(binding.baseUrlInput), text(binding.modelInput), text(binding.keyInput));
        });
        binding.btnRemove.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.ai_remove_title)
                .setMessage(R.string.ai_remove_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.ai_remove_confirm, (d, w) -> {
                    viewModel.remove();
                    binding.keyInput.setText("");
                    binding.modelInput.setText("");
                    Toast.makeText(requireContext(), R.string.ai_removed, Toast.LENGTH_SHORT).show();
                })
                .show());

        viewModel.getSaveResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) {
                return;
            }
            viewModel.consumeSaveResult();
            if (result.error == null) {
                binding.keyInput.setText("");
                updateKeyHint();
                Toast.makeText(requireContext(), R.string.ai_saved, Toast.LENGTH_SHORT).show();
            } else {
                showError(result.error);
            }
        });
    }

    private void showError(String message) {
        // Route the validator message to the field it belongs to.
        if (AiValidators.validateBaseUrl(text(binding.baseUrlInput)) != null) {
            binding.baseUrlLayout.setError(message);
        } else if (AiValidators.validateModel(text(binding.modelInput)) != null) {
            binding.modelLayout.setError(message);
        } else {
            binding.keyLayout.setError(message);
        }
    }

    private void check(AiProviderType type) {
        int id = type == AiProviderType.ANTHROPIC ? R.id.radio_anthropic
                : type == AiProviderType.GEMINI ? R.id.radio_gemini : R.id.radio_openai;
        binding.providerGroup.check(id);
    }

    private void updateKeyHint() {
        binding.keyLayout.setHint(getString(viewModel.hasKey(current)
                ? R.string.ai_key_hint_stored : R.string.ai_key_hint));
    }

    @NonNull
    private static String text(com.google.android.material.textfield.TextInputEditText input) {
        return input.getText() != null ? input.getText().toString() : "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
