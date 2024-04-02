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
import android.os.Environment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private WifiManager wifiManager;
    //안드로이드로 와이파이를 관리하는 라이브러리로 현재 와이파이를 읽을때 startscan이라는 라이브러리를 사용하는데 안드로이드 10이상에서는
    //해당 메소드를 사용할 수 없다. 대신하여 registerScanResultsCallback, ScanResultsCallback를 사용한다.
    private List<String> scanResults = new ArrayList<>();
    private BroadcastReceiver wifiScanReceiver;

    @Override
    public View onCreateView(
            @NotNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);

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
                    scanSuccess();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        requireActivity().getApplicationContext().registerReceiver(wifiScanReceiver, intentFilter);

        return binding.getRoot();
    }

    private void scanWifi() {
        boolean success = wifiManager.startScan();
        showSnackbar("Scan started: " + success, BaseTransientBottomBar.LENGTH_SHORT);
    }

    // Register the permissions callback, which handles the user's response to the
// system permissions dialog. Save the return value, an instance of
// ActivityResultLauncher, as an instance variable.
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            });

    private void checkForLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void scanSuccess() {
        checkForLocationPermission();
        List<ScanResult> results = wifiManager.getScanResults();
        showSnackbar("Result size:" + results.size(), BaseTransientBottomBar.LENGTH_LONG);

        List<String> stringResults = results.stream()
                .filter(scanResult -> StringUtils.isNotEmpty(scanResult.SSID))
                .sorted(Comparator.comparing(scanResult -> scanResult.level, Comparator.reverseOrder()))
                .map(scanResult -> scanResult.SSID + " RSSI:" + scanResult.level)
                .collect(Collectors.toList());
        scanResults = stringResults;
        //위 부분에서 ssid 값으로 한기대 와이파이 걸러내서 bssid값으로 보관하게 해야함

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, stringResults);
        binding.wifiList.setAdapter(adapter);
        saveToFile();
        scanWifi(); //현재 이 부분때문에 스캔이 계속 되니깐 이후에 주석처리 해서 버튼을 눌렀을 때에만 되게 가능
    }

    private void saveToFile() {
        EditText textEditor = binding.dataLabelEdit;
        Editable labelText = textEditor.getText();
        if (StringUtils.isEmpty(labelText) || textEditor.hasFocus()) {
            return;
        }
        writeFileOnInternalStorage("data.txt", labelText + ": " + String.join(";", scanResults) + "\n");
    }

    public void writeFileOnInternalStorage(String sFileName, String sBody) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "mydir");
        if (!file.exists()) {
            boolean createdDir = file.mkdir();
        }

        try {
            File gpxfile = new File(file, sFileName);
            if (!gpxfile.exists()) {
                boolean createdNewFile = gpxfile.createNewFile();
            }
            FileWriter writer = new FileWriter(gpxfile, true);
            writer.append(sBody);
            writer.flush();
            writer.close();
            showSnackbar("Sucessfully wrote to storage", BaseTransientBottomBar.LENGTH_SHORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSnackbar(String text, int length) {
        Snackbar.make(requireActivity().findViewById(android.R.id.content), text, length)
                .setAction("Action", null).show();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.scanBtn.setOnClickListener(v -> scanWifi());
        binding.dataLabelEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.dataLabelEdit.clearFocus();
            }
            return false;
        });

        binding.mapBtn.setOnClickListener(view1 -> NavHostFragment.findNavController(FirstFragment.this)
                .navigate(R.id.action_FirstFragment_to_SecondFragment));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().getApplicationContext().unregisterReceiver(wifiScanReceiver);
        binding = null;
    }

}