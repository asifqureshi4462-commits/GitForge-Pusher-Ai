package com.asifqureshi.gitforge.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

import com.asifqureshi.gitforge.R;
import com.asifqureshi.gitforge.databinding.FragmentPendingFeatureBinding;

/**
 * Honest placeholder for a destination whose feature is not built yet. It shows the
 * destination's label and the phase in which the feature becomes functional (nav argument
 * "phase"). It contains no mock data and no fake behaviour, and is replaced destination by
 * destination as the real screens land.
 */
public class PendingFeatureFragment extends Fragment {

    public static final String ARG_PHASE = "phase";

    @Nullable
    private FragmentPendingFeatureBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPendingFeatureBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (binding == null) {
            return;
        }

        NavDestination destination = NavHostFragment.findNavController(this).getCurrentDestination();
        CharSequence label = destination != null ? destination.getLabel() : null;
        binding.title.setText(label != null ? label : getString(R.string.app_name));

        Bundle args = getArguments();
        int phase = args != null ? args.getInt(ARG_PHASE, 0) : 0;
        if (phase > 0) {
            binding.status.setText(getString(R.string.pending_feature_status, phase));
        } else {
            binding.status.setText(R.string.pending_feature_status_generic);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
