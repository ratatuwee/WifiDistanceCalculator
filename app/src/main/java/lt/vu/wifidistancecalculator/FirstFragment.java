package lt.vu.wifidistancecalculator;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import lt.vu.wifidistancecalculator.databinding.FragmentFirstBinding;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private WifiManager wifiManager;

    @Override
    public View onCreateView(
            @NotNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);

        View currentView = Objects.requireNonNull(getActivity()).findViewById(android.R.id.content);
        wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Snackbar.make(currentView, "WiFi is disabled ...", BaseTransientBottomBar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getActivity().getApplicationContext().registerReceiver(wifiScanReceiver, intentFilter);

        return binding.getRoot();

    }

    private void scanWifi() {
        boolean success = wifiManager.startScan();
        Snackbar.make(getActivity().findViewById(android.R.id.content), "Scanning WiFi ... :" + success, BaseTransientBottomBar.LENGTH_SHORT)
                .setAction("Action", null).show();
        if (!success) {
            // scan failure handling
            scanFailure();
        }
    }

    // Register the permissions callback, which handles the user's response to the
// system permissions dialog. Save the return value, an instance of
// ActivityResultLauncher, as an instance variable.
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (Boolean.TRUE.equals(isGranted)) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                }
            });

    BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            boolean success = intent.getBooleanExtra(
                    WifiManager.EXTRA_RESULTS_UPDATED, false);
            Snackbar.make(Objects.requireNonNull(getActivity()).findViewById(android.R.id.content), "onreceive: " + success, BaseTransientBottomBar.LENGTH_LONG)
                    .setAction("Action", null).show();
            if (success) {
                scanSuccess();
            } else {
                // scan failure handling
                scanFailure();
            }
        }
    };


    private void checkForLocationPermission() {
        if (ContextCompat.checkSelfPermission(Objects.requireNonNull(getActivity()).getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
//            ActivityCompat.requestPermissions(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION);

            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void scanSuccess() {
        checkForLocationPermission();
        List<ScanResult> results = wifiManager.getScanResults();
        Snackbar.make(Objects.requireNonNull(getActivity()).findViewById(android.R.id.content), "Result size:" + results.size(), BaseTransientBottomBar.LENGTH_LONG)
                .setAction("Action", null).show();

        List<String> stringResults = results.stream().map(scanResult -> scanResult.SSID + " " + scanResult.level).collect(Collectors.toList());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, stringResults);
        binding.wifiList.setAdapter(adapter);
    }

    private void scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        checkForLocationPermission();
        List<ScanResult> results = wifiManager.getScanResults();

    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(view1 -> NavHostFragment.findNavController(FirstFragment.this)
                .navigate(R.id.action_FirstFragment_to_SecondFragment));

        binding.scanBtn.setOnClickListener(v -> scanWifi());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}