package com.asifqureshi.gitforge;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavGraph;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.asifqureshi.gitforge.data.repository.AuthRepository;
import com.asifqureshi.gitforge.databinding.ActivityMainBinding;
import com.asifqureshi.gitforge.ui.theme.ThemeManager;
import com.asifqureshi.gitforge.ui.upload.UploadViewModel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Single activity hosting the navigation graph and the bottom navigation bar.
 * Routes to the sign-in screen whenever the user is signed out (including when a token is rejected) and back to
 * the dashboard after signing in. Bottom navigation is shown only on top-level destinations.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        AppContainer container = GitForgeApp.container(this);
        ThemeManager.applyAccent(this, container.getSettings());
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Fragment hostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (!(hostFragment instanceof NavHostFragment)) {
            throw new IllegalStateException("NavHostFragment not found in activity_main");
        }
        final NavController navController = ((NavHostFragment) hostFragment).getNavController();
        final AuthRepository auth = container.getAuth();

        NavGraph graph = navController.getNavInflater().inflate(R.navigation.nav_graph);
        graph.setStartDestination(auth.isSignedIn() ? R.id.homeFragment : R.id.loginFragment);
        navController.setGraph(graph);
        NavigationUI.setupWithNavController(binding.bottomNav, navController);

        final Set<Integer> topLevelDestinations = new HashSet<>(Arrays.asList(
                R.id.homeFragment,
                R.id.repositoriesFragment,
                R.id.aiFragment,
                R.id.historyFragment,
                R.id.settingsFragment));

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean topLevel = topLevelDestinations.contains(destination.getId());
            binding.bottomNav.setVisibility(topLevel ? View.VISIBLE : View.GONE);
            ViewCompat.requestApplyInsets(binding.getRoot());
        });

        final NavOptions clearAll = new NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build();
        auth.getSignedIn().observe(this, signedIn -> {
            NavDestination current = navController.getCurrentDestination();
            int currentId = current != null ? current.getId() : 0;
            if (Boolean.TRUE.equals(signedIn)) {
                if (currentId == R.id.loginFragment) {
                    navController.navigate(R.id.homeFragment, null, clearAll);
                }
            } else if (currentId != R.id.loginFragment) {
                new ViewModelProvider(this).get(UploadViewModel.class).reset();
                navController.navigate(R.id.loginFragment, null, clearAll);
            }
        });

        // Edge-to-edge: the root pads for the status bar / cutout / side bars. The bottom navigation
        // bar pads itself for the gesture/navigation bar; when it is hidden the root pads the bottom.
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            boolean navVisible = binding.bottomNav.getVisibility() == View.VISIBLE;
            view.setPadding(bars.left, bars.top, bars.right, navVisible ? 0 : bars.bottom);
            return windowInsets;
        });
    }
}
