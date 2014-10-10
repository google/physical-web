package physical_web.org.physicalweb;

/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.uribeacon.beacon.UriBeacon;
import org.uribeacon.scan.compat.BluetoothLeScannerCompat;
import org.uribeacon.scan.compat.BluetoothLeScannerCompatProvider;
import org.uribeacon.scan.compat.ScanCallback;
import org.uribeacon.scan.compat.ScanFilter;
import org.uribeacon.scan.compat.ScanResult;
import org.uribeacon.scan.compat.ScanSettings;
import org.uribeacon.scan.util.AdvertisingData;
import org.uribeacon.scan.util.RangingUtils;
import org.uribeacon.widget.ScanResultAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * This class shows the ui list for all
 * detected nearby devices that are beacons.
 * It also listens for tap events
 * on items within the list.
 * Tapped list items then launch
 * the browser and point that browser
 * to the given list items url.
 */

public class NearbyDevicesFragment extends Fragment implements MetadataResolver.MetadataResolverCallback {

  private static String TAG = "NearbyDevicesFragment";
  private ListView mNearbyDevicesListView;
  private LayoutInflater mLayoutInflater;
  private NearbyDeviceAdapter mNearbyDeviceAdapter;
  private BluetoothAdapter mBluetoothAdapter;
  private static final int REQUEST_ENABLE_BT = 1;
  private HashMap<String, MetadataResolver.DeviceMetadata> mUrlToDeviceMetadata;

  public static NearbyDevicesFragment newInstance() {
    return new NearbyDevicesFragment();
  }

  public NearbyDevicesFragment() {
  }

  private void initialize(View rootView) {
    mUrlToDeviceMetadata = new HashMap<>();

    initializeNearbyDevicesListView(rootView);
    createNearbyDevicesAdapter();
    initializeBluetooth();
  }

  private void initializeNearbyDevicesListView(View rootView) {
    mNearbyDevicesListView = (ListView) rootView.findViewById(R.id.list_view_nearby_devices);
    mNearbyDevicesListView.setEmptyView(rootView.findViewById(R.id.linearLayout_nearbyDevicesEmptyListView));
    mNearbyDevicesListView.setOnItemClickListener(onItemClick_nearbyDevicesListViewItem);
  }

  private void createNearbyDevicesAdapter() {
    mNearbyDeviceAdapter = new NearbyDeviceAdapter();
    mNearbyDevicesListView.setAdapter(mNearbyDeviceAdapter);
  }

  private void initializeBluetooth() {
    // Initializes a Bluetooth adapter. For API version 18 and above,
    // get a reference to BluetoothAdapter through BluetoothManager.
    final BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = bluetoothManager.getAdapter();

    // Checks if Bluetooth is supported on the device.
    //if (mBluetoothAdapter == null) {
    //  Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show();
    //  finish();
    //}
  }


  /////////////////////////////////
  // accessors
  /////////////////////////////////

  private BluetoothLeScannerCompat getLeScanner() {
    return BluetoothLeScannerCompatProvider.getBluetoothLeScannerCompat(getActivity());
  }


  /////////////////////////////////
  // callbacks
  /////////////////////////////////

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
    mLayoutInflater = layoutInflater;
    View rootView = layoutInflater.inflate(R.layout.fragment_nearby_devices, container, false);
    initialize(rootView);
    return rootView;
  }

  @Override
  public void onResume() {
    super.onResume();
    startScanning();
  }

  @Override
  public void onPause() {
    super.onPause();
    stopScanning();
  }

  private final ScanCallback mScanCallback = new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
      handleScanMatch(result);
    }

    @Override
    public void onScanFailed(int errorCode) {
      handleScanFailed(errorCode);
    }
  };

  /**
   * Called when an item in the list view is clicked.
   */
  private AdapterView.OnItemClickListener onItemClick_nearbyDevicesListViewItem = new AdapterView.OnItemClickListener() {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      // Get the device for the given item
      String url = getUrlFromDeviceSighting(mNearbyDeviceAdapter.getItem(position));
      if (url != null) {
        // Open the url in the browser
        openUrlInBrowser(url);
      } else {
        Toast.makeText(getActivity(), "No URL found.", Toast.LENGTH_SHORT).show();
      }
    }
  };

  @Override
  public void onDeviceMetadataReceived(String url, MetadataResolver.DeviceMetadata deviceMetadata) {
    mUrlToDeviceMetadata.put(url, deviceMetadata);
    // If we don't want to wait for another sighting
    mNearbyDeviceAdapter.notifyDataSetChanged();
  }


  /////////////////////////////////
  // utilities
  /////////////////////////////////

  private void startScanning() {
    Log.v(TAG, "startScanning() called");
    // Ensures Bluetooth is enabled on the device. If Bluetooth is not currently enabled,
    // fire an intent to display a dialog asking the user to grant permission to enable it.
    if (!mBluetoothAdapter.isEnabled()) {
      Log.v(TAG, "Bluetooth not enabled; asking user to start it");
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    } else {
      List<ScanFilter> filters = new ArrayList<>();
      ScanFilter.Builder builder = new ScanFilter.Builder()
          .setServiceData(UriBeacon.URI_SERVICE_UUID,
              new byte[] {},
              new byte[] {});
      filters.add(builder.build());

      ScanSettings settings = new ScanSettings.Builder()
          .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
          .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
          .build();
      getLeScanner().startScan(filters, settings, mScanCallback);
    }
  }

  private void stopScanning() {
    Log.v(TAG, "stopScanning() called");
    getLeScanner().stopScan(mScanCallback);
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mNearbyDeviceAdapter.clear();
      }
    });
  }










  private void handleScanMatch(final ScanResult scanResult) {
    //Log.v(TAG, "Scan Match: " + scanResult.getDevice() + " Rssi:" + scanResult.getRssi() + " sr:"
        //+ AdvertisingData.toHexString(scanResult.getScanRecord().getBytes()));
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        int txPowerLevel = getTxPowerLevel(scanResult);

        UriBeacon uriBeacon = UriBeacon.parseFromBytes(scanResult.getScanRecord().getBytes());
        if (uriBeacon != null) {
          String url = uriBeacon.getUriString();
          if (!mUrlToDeviceMetadata.containsKey(url)) {
            mUrlToDeviceMetadata.put(url, null);
            MetadataResolver.findDeviceMetadata(getActivity(), NearbyDevicesFragment.this, url);
          }
          else
            if (mUrlToDeviceMetadata.get(url) != null) {
              mNearbyDeviceAdapter.add(scanResult, txPowerLevel, 5);
          }
        }
      }
    });
  }

  private int getTxPowerLevel(ScanResult scanResult) {
    // Note: Config Service has no TX Power.
    return RangingUtils.DEFAULT_TX_POWER_LEVEL;
  }

  private void handleScanFailed(int err) {
    Log.e(TAG, "onScanFailed: " + err);
  }




  private void openUrlInBrowser(String url) {
    // Ensure we have an http prefix
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
      url = "http://" + url;
    }
    // Open the browser and point it to the given url
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(url));
    startActivity(intent);
  }

  private static String getUrlFromDeviceSighting(ScanResultAdapter.DeviceSighting deviceSighting) {
    UriBeacon uriBeacon = UriBeacon.parseFromBytes(deviceSighting.scanResult.getScanRecord().getBytes());
    return uriBeacon.getUriString();
  }


  // Adapter for holding devices found through scanning.
  private class NearbyDeviceAdapter extends ScanResultAdapter {

    NearbyDeviceAdapter() {
      super(mLayoutInflater);
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
      // Get the list view item for the given position
      if (view == null) {
        view = getActivity().getLayoutInflater().inflate(R.layout.list_item_nearby_device, viewGroup, false);
      }
      // Get the device for the given position
      String url = getUrlFromDeviceSighting(getItem(i));

      // Get the metadata for this device
      MetadataResolver.DeviceMetadata deviceMetadata = mUrlToDeviceMetadata.get(url);

      // If the metadata exists
      if (deviceMetadata != null) {
        // Set the title text
        TextView infoView = (TextView) view.findViewById(R.id.title);
        infoView.setText(deviceMetadata.title);

        // Set the site url text
        infoView = (TextView) view.findViewById(R.id.url);
        infoView.setText(deviceMetadata.siteUrl);

        // Set the description text
        infoView = (TextView) view.findViewById(R.id.description);
        infoView.setText(deviceMetadata.description);

        // Set the favicon image
        ImageView iconView = (ImageView) view.findViewById(R.id.icon);
        iconView.setImageBitmap(deviceMetadata.icon);

        // If the metadata does not exist
      } else {
        Log.i(TAG, String.format("Beacon with URL %s has no metadata.", url));
      }
      return view;

    }

  }

}


