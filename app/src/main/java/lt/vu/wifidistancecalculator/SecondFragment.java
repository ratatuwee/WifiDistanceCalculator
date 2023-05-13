package lt.vu.wifidistancecalculator;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.ParsedRequestListener;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import lt.vu.wifidistancecalculator.databinding.FragmentSecondBinding;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Collectors;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;
    private WifiManager wifiManager;
    private BroadcastReceiver wifiScanReceiver;

    @Override
    public View onCreateView(
            @NotNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        wifiManager = (WifiManager) requireActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            showSnackbar("WiFi is disabled ...", BaseTransientBottomBar.LENGTH_LONG);
        }
        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                boolean success = intent.getBooleanExtra(
                        WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    refreshPosition();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        requireActivity().getApplicationContext().registerReceiver(wifiScanReceiver, intentFilter);

        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            });

    private void checkForLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void refreshPosition() {
        checkForLocationPermission();
        String apPrefix = "TEST_AP_";

        Map<String, String> stringResults = wifiManager.getScanResults().stream()
                .filter(scanResult -> StringUtils.isNotEmpty(scanResult.SSID))
                .filter(scanResult -> scanResult.SSID.contains(apPrefix))
                .collect(Collectors.toMap(scanResult -> scanResult.SSID.substring(apPrefix.length()), scanResult -> String.valueOf(scanResult.level)));

        if (stringResults.size() != 3) {
            return;
        }

        String path = "http://192.168.0.195:8000/" + resolvePathString() + "/{firstRssi}/{secondRssi}/{thirdRssi}";
        AndroidNetworking.get(path)
                .addPathParameter("firstRssi", stringResults.get("1"))
                .addPathParameter("secondRssi", stringResults.get("2"))
                .addPathParameter("thirdRssi", stringResults.get("3"))
                .setPriority(Priority.LOW)
                .build()
                .getAsObject(Cartesian2D.class, new ParsedRequestListener<Cartesian2D>() {
                    @Override
                    public void onResponse(Cartesian2D response) {
                        Drawable butoPlanas = getResources().getDrawable(R.drawable.buto_planas_proper);
                        CircleDrawable circle = new CircleDrawable(response.getX(), response.getY(), butoPlanas);

                        binding.imageView.setBackground(butoPlanas); // TODO paziureti kodel kvadratas pasidaro
                        binding.imageView.setImageDrawable(circle);
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.e("Error", "Error");
                        anError.printStackTrace();
                    }
                });
    }

    private String resolvePathString() {
        if (binding.radioTrilateration.isChecked()) {
            return "trilateration";
        } else if (binding.radioNeural.isChecked()) {
            return "neural";
        }

        throw new IllegalStateException("Something went wrong");
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonSecond.setOnClickListener(view1 -> NavHostFragment.findNavController(SecondFragment.this)
                .navigate(R.id.action_SecondFragment_to_FirstFragment));
        binding.refreshBtn.setOnClickListener(v -> wifiManager.startScan());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().getApplicationContext().unregisterReceiver(wifiScanReceiver);
        binding = null;
    }

    private void showSnackbar(String text, int length) {
        Snackbar.make(requireActivity().findViewById(android.R.id.content), text, length)
                .setAction("Action", null).show();
    }

}