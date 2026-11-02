package com.asifqureshi.gitforge.ui.auth;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.asifqureshi.gitforge.R;
import com.asifqureshi.gitforge.databinding.FragmentLoginBinding;
import com.asifqureshi.gitforge.ui.common.Browser;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        boolean configured = viewModel.isOAuthConfigured();
        binding.btnOauth.setEnabled(configured);
        binding.oauthConfigHint.setVisibility(configured ? View.GONE : View.VISIBLE);

        binding.btnOauth.setOnClickListener(v ->
                viewModel.startOAuth(binding.radioPrivate.isChecked()));
        binding.btnCancelOauth.setOnClickListener(v -> viewModel.cancelOAuth());
        binding.btnToken.setOnClickListener(v -> {
            CharSequence text = binding.tokenInput.getText();
            viewModel.signInWithToken(text != null ? text.toString() : "");
        });
        binding.btnCopyCode.setOnClickListener(v -> {
            LoginViewModel.DeviceInfo info = viewModel.getDevice().getValue();
            if (info != null) {
                ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("GitHub device code", info.userCode));
            }
        });
        binding.btnOpenGithub.setOnClickListener(v -> {
            LoginViewModel.DeviceInfo info = viewModel.getDevice().getValue();
            if (info != null) {
                Browser.open(requireContext(), info.verificationUri);
            }
        });

        viewModel.getBusy().observe(getViewLifecycleOwner(), busy -> {
            boolean b = Boolean.TRUE.equals(busy);
            boolean deviceShown = viewModel.getDevice().getValue() != null;
            binding.progress.setVisibility(b && !deviceShown ? View.VISIBLE : View.GONE);
            binding.btnToken.setEnabled(!b);
            binding.btnOauth.setEnabled(!b && viewModel.isOAuthConfigured());
        });
        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            binding.errorText.setText(message);
            binding.errorText.setVisibility(message == null || message.isEmpty() ? View.GONE : View.VISIBLE);
        });
        viewModel.getDevice().observe(getViewLifecycleOwner(), info -> {
            binding.devicePanel.setVisibility(info == null ? View.GONE : View.VISIBLE);
            if (info != null) {
                binding.userCode.setText(info.userCode);
                binding.progress.setVisibility(View.GONE);
            }
        });
        viewModel.getSessionNotice().observe(getViewLifecycleOwner(), notice -> {
            binding.noticeText.setText(notice);
            binding.noticeText.setVisibility(notice == null || notice.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
