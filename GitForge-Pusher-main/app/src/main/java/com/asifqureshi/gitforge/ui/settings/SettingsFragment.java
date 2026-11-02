package com.asifqureshi.gitforge.ui.settings;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.asifqureshi.gitforge.BuildConfig;
import com.asifqureshi.gitforge.GitForgeApp;
import com.asifqureshi.gitforge.R;
import com.asifqureshi.gitforge.data.local.SettingsStore;
import com.asifqureshi.gitforge.databinding.FragmentSettingsBinding;
import com.asifqureshi.gitforge.ui.common.Formatters;
import com.asifqureshi.gitforge.ui.theme.Accent;
import com.asifqureshi.gitforge.ui.theme.ThemeManager;
import com.asifqureshi.gitforge.ui.upload.UploadViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;
    private SettingsStore settings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        settings = GitForgeApp.container(requireContext()).getSettings();

        bindAccount();
        bindTheme();
        bindAccent();

        binding.btnAiSettings.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.aiSettingsFragment));
        binding.btnSignOut.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_sign_out_title)
                .setMessage(R.string.settings_sign_out_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.settings_sign_out, (d, w) -> viewModel.signOut())
                .show());
        binding.btnClearCache.setOnClickListener(v -> {
            UploadViewModel upload = new ViewModelProvider(requireActivity()).get(UploadViewModel.class);
            if (upload.getPhase().getValue() != UploadViewModel.Phase.IDLE) {
                Toast.makeText(requireContext(), R.string.settings_cache_busy, Toast.LENGTH_LONG).show();
                return;
            }
            viewModel.clearCache();
        });
        binding.versionText.setText(getString(R.string.settings_version, BuildConfig.VERSION_NAME));

        viewModel.getCacheBytes().observe(getViewLifecycleOwner(), bytes ->
                binding.btnClearCache.setText(getString(R.string.settings_clear_cache,
                        Formatters.bytes(bytes == null ? 0 : bytes))));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            binding.aiStatus.setText(GitForgeApp.container(requireContext()).getAi().isConfigured()
                    ? R.string.settings_ai_configured : R.string.settings_ai_not_configured);
            viewModel.refreshCacheSize();
        }
    }

    private void bindAccount() {
        String login = settings.getAccountLogin();
        String method = SettingsStore.AUTH_OAUTH.equals(settings.getAuthMethod())
                ? getString(R.string.settings_method_oauth) : getString(R.string.settings_method_pat);
        String scopes = settings.getTokenScopes();
        StringBuilder sb = new StringBuilder(getString(R.string.settings_signed_in_as, login != null ? login : "-"));
        sb.append('\n').append(method);
        if (scopes != null && !scopes.isEmpty()) {
            sb.append('\n').append(getString(R.string.settings_scopes, scopes));
        } else if (scopes != null) {
            sb.append('\n').append(getString(R.string.settings_scopes_none));
        } else {
            sb.append('\n').append(getString(R.string.settings_scopes_fine_grained));
        }
        binding.accountText.setText(sb.toString());
    }

    private void bindTheme() {
        int mode = settings.getThemeMode();
        binding.radioThemeSystem.setChecked(mode == SettingsStore.THEME_SYSTEM);
        binding.radioThemeLight.setChecked(mode == SettingsStore.THEME_LIGHT);
        binding.radioThemeDark.setChecked(mode == SettingsStore.THEME_DARK);
        binding.themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int newMode;
            if (checkedId == R.id.radio_theme_light) {
                newMode = SettingsStore.THEME_LIGHT;
            } else if (checkedId == R.id.radio_theme_dark) {
                newMode = SettingsStore.THEME_DARK;
            } else {
                newMode = SettingsStore.THEME_SYSTEM;
            }
            if (newMode != settings.getThemeMode()) {
                settings.setThemeMode(newMode);
                ThemeManager.applyNightMode(newMode);
            }
        });
    }

    private void bindAccent() {
        final Accent current = Accent.fromId(settings.getAccentId());
        for (Accent accent : Accent.values()) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setText(accent.getLabelRes());
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            chip.setChipBackgroundColor(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), accent.getColorRes())));
            chip.setTextColor(ContextCompat.getColor(requireContext(), accent.getOnColorRes()));
            chip.setCheckedIconTint(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), accent.getOnColorRes())));
            chip.setTag(accent);
            binding.accentGroup.addView(chip);
            if (accent == current) {
                chip.setChecked(true);
            }
        }
        binding.accentGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) {
                return;
            }
            View chip = group.findViewById(ids.get(0));
            if (chip != null && chip.getTag() instanceof Accent) {
                Accent chosen = (Accent) chip.getTag();
                if (chosen != Accent.fromId(settings.getAccentId())) {
                    settings.setAccentId(chosen.getId());
                    requireActivity().recreate();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
