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
//import org.apache.commons.math3.fitting.leastsquares.*;
//import org.apache.commons.math3.linear.Array2DRowRealMatrix;
//import org.apache.commons.math3.linear.ArrayRealVector;
//import org.apache.commons.math3.linear.RealMatrix;
//import org.apache.commons.math3.linear.RealVector;
//import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private WifiManager wifiManager;
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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, stringResults);
        binding.wifiList.setAdapter(adapter);
        saveToFile();
        scanWifi();
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

    //    private void calculate() {
//        final double radius = 70.0;
//        final Cartesian2D[] observedPoints = new Cartesian2D[]{
//                new Cartesian2D(30.0, 68.0),
//                new Cartesian2D(50.0, -6.0),
//                new Cartesian2D(110.0, -20.0),
//                new Cartesian2D(35.0, 15.0),
//                new Cartesian2D(45.0, 97.0)
//        };
//
//        // the model function components are the distances to current estimated center,
//        // they should be as close as possible to the specified radius
//        MultivariateJacobianFunction distancesToCurrentCenter = point -> {
//            Cartesian2D center = new Cartesian2D(point.getEntry(0), point.getEntry(1));
//
//            RealVector value = new ArrayRealVector(observedPoints.length);
//            RealMatrix jacobian = new Array2DRowRealMatrix(observedPoints.length, 2);
//
//            for (int i = 0; i < observedPoints.length; ++i) {
//                Cartesian2D o = observedPoints[i];
//                double modelI = Cartesian2D.distance(o, center);
//                value.setEntry(i, modelI);
//                // derivative with respect to p0 = x center
//                jacobian.setEntry(i, 0, (center.getX() - o.getX()) / modelI);
//                // derivative with respect to p1 = y center
//                jacobian.setEntry(i, 1, (center.getX() - o.getX()) / modelI);
//            }
//
//            return new Pair<>(value, jacobian);
//
//        };
//
//        // the target is to have all points at the specified radius from the center
//        double[] prescribedDistances = new double[observedPoints.length];
//        Arrays.fill(prescribedDistances, radius);
//
//        // least squares problem to solve : modeled radius should be close to target radius
//        LeastSquaresProblem problem = new LeastSquaresBuilder()
//                .start(new double[]{100.0, 50.0})
//                .model(distancesToCurrentCenter)
//                .target(prescribedDistances)
//                .lazyEvaluation(false)
//                .maxEvaluations(1000)
//                .maxIterations(1000)
//                .build();
//        LeastSquaresOptimizer.Optimum optimum = new LevenbergMarquardtOptimizer().optimize(problem);
//        Cartesian2D fittedCenter = new Cartesian2D(optimum.getPoint().getEntry(0), optimum.getPoint().getEntry(1));
//        System.out.println("fitted center: " + fittedCenter.getX() + " " + fittedCenter.getY());
//        System.out.println("RMS: " + optimum.getRMS());
//        System.out.println("evaluations: " + optimum.getEvaluations());
//        System.out.println("iterations: " + optimum.getIterations());
//    }

}