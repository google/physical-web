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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import org.uribeacon.beacon.UriBeacon;
import org.uribeacon.scan.compat.BluetoothLeScannerCompat;
import org.uribeacon.scan.compat.BluetoothLeScannerCompatProvider;
import org.uribeacon.scan.compat.ScanCallback;
import org.uribeacon.scan.compat.ScanFilter;
import org.uribeacon.scan.compat.ScanResult;
import org.uribeacon.scan.compat.ScanSettings;
import java.util.ArrayList;
import java.util.List;

/**
 * This class show the ui list for all
 * detected nearby devices that are beacons.
 * It also listens for tap events
 * on items within the list.
 * Tapped list items then launch
 * the browser and point that browser
 * to the given list items url.
 */

public class NearbyDevicesFragment extends Fragment implements MetadataResolver.MetadataResolverCallback {

  private static String TAG = "NearbyDevicesFragment";
  private NearbyDevicesAdapter mNearbyDevicesAdapter;
  private ListView mNearbyDevicesListView;

  public static NearbyDevicesFragment newInstance() {
    return new NearbyDevicesFragment();
  }

  public NearbyDevicesFragment() {
  }

  private void initialize(View rootView) {
    initializeNearbyDevicesListView(rootView);
    createNearbyDevicesAdapter();
  }

  private void initializeNearbyDevicesListView(View rootView) {
    mNearbyDevicesListView = (ListView) rootView.findViewById(R.id.list_view_nearby_devices);
    mNearbyDevicesListView.setOnItemClickListener(onItemClick_nearbyDevicesListViewItem);
  }

  private void createNearbyDevicesAdapter() {
    mNearbyDevicesAdapter = new NearbyDevicesAdapter(getActivity());
    mNearbyDevicesListView.setAdapter(mNearbyDevicesAdapter);
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

  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_nearby_devices, container, false);
    initialize(rootView);
    return rootView;
  }

  @Override
  public void onResume() {
    super.onResume();
    startSearchingForDevices();
  }

  @Override
  public void onPause() {
    super.onPause();
    stopSearchingForDevices();
  }

  private final ScanCallback mScanCallback = new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, ScanResult scanResult) {
      switch (callbackType) {
        case ScanSettings.CALLBACK_TYPE_ALL_MATCHES:
          handleFoundDevice(scanResult);
          break;
        case ScanSettings.CALLBACK_TYPE_FIRST_MATCH:
          handleFoundDevice(scanResult);
          break;
        case ScanSettings.CALLBACK_TYPE_MATCH_LOST:
          handleLostDevice(scanResult);
          break;
        default:
          Log.e(TAG, "Unrecognized callback type constant received: " + callbackType);
      }
    }

    @Override
    public void onScanFailed(int errorCode) {
      Log.d(TAG, "onScanFailed  " + "errorCode: " + errorCode);
    }
  };

  /**
   * Called when an item in the list view is clicked.
   */
  private AdapterView.OnItemClickListener onItemClick_nearbyDevicesListViewItem = new AdapterView.OnItemClickListener() {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      // Get the device for the given item
      Device device = mNearbyDevicesAdapter.getItem(position);
      // Get the url for this device
      String url = device.getLongUrl();
      if (url != null) {
        // Open the url in the browser
        openUrlInBrowser(url);
      } else {
        Toast.makeText(getActivity(), "No URL found.", Toast.LENGTH_SHORT).show();
      }
    }
  };

  @Override
  public void onDeviceMetadataReceived(Device device, MetadataResolver.DeviceMetadata deviceMetadata) {
    mNearbyDevicesAdapter.notifyDataSetChanged();
  }


  /////////////////////////////////
  // utilities
  /////////////////////////////////

  private void startSearchingForDevices() {
    Log.v(TAG, "startSearchingForDevices");

    int scanMode = ScanSettings.SCAN_MODE_LOW_LATENCY;

    ScanSettings settings = new ScanSettings.Builder()
        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        .setScanMode(scanMode)
        .build();

    List<ScanFilter> filters = new ArrayList<>();

    ScanFilter.Builder builder = new ScanFilter.Builder()
        .setServiceData(UriBeacon.URI_SERVICE_UUID,
            new byte[] {},
            new byte[] {});
    filters.add(builder.build());

    boolean started = getLeScanner().startScan(filters, settings, mScanCallback);
    Log.v(TAG, started ? "... scan started" : "... scan NOT started");
  }

  private void stopSearchingForDevices() {
    Log.v(TAG, "stopSearchingForDevices");
    getLeScanner().stopScan(mScanCallback);
  }

  private void handleFoundDevice(ScanResult scanResult) {
    Device device = mNearbyDevicesAdapter.handleFoundDevice(scanResult);
    if (device != null) {
      findMetadataForDevice(device);
    }
  }

  private void handleLostDevice(ScanResult scanResult) {
    mNearbyDevicesAdapter.handleLostDevice(scanResult);
  }

  private void findMetadataForDevice(Device device) {
    MetadataResolver.findDeviceMetadata(getActivity(), this, device);
  }

  private void openUrlInBrowser(String url) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(url));
    startActivity(intent);
  }

}


