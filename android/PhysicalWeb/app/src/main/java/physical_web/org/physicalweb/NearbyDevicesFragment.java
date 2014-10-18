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
import android.app.ListFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.uribeacon.beacon.UriBeacon;
import org.uribeacon.scan.compat.BluetoothLeScannerCompat;
import org.uribeacon.scan.compat.BluetoothLeScannerCompatProvider;
import org.uribeacon.scan.compat.ScanCallback;
import org.uribeacon.scan.compat.ScanFilter;
import org.uribeacon.scan.compat.ScanRecord;
import org.uribeacon.scan.compat.ScanResult;
import org.uribeacon.scan.compat.ScanSettings;
import org.uribeacon.widget.ScanResultAdapter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class shows the ui list for all
 * detected nearby beacons.
 * It also listens for tap events
 * on items within the list.
 * Tapped list items then launch
 * the browser and point that browser
 * to the given list items url.
 */
public class NearbyDevicesFragment extends ListFragment implements MetadataResolver.MetadataResolverCallback {

  private static final String TAG = "NearbyDevicesFragment";
  private LayoutInflater mLayoutInflater;
  private BluetoothAdapter mBluetoothAdapter;
  private static final int REQUEST_ENABLE_BT = 1;
  private HashMap<String, MetadataResolver.UrlMetadata> mUrlToUrlMetadata;
  private AnimationDrawable mScanningAnimationDrawable;
  private ImageView mScanningImageView;
  private boolean mIsDemoMode;
  private static int BEACON_EXPIRATION_DURATION = 5;

  public static NearbyDevicesFragment newInstance(boolean isDemoMode) {
    NearbyDevicesFragment nearbyDevicesFragment = new NearbyDevicesFragment();
    Bundle bundle = new Bundle();
    bundle.putBoolean("isDemoMode", isDemoMode);
    nearbyDevicesFragment.setArguments(bundle);
    return nearbyDevicesFragment;
  }

  public NearbyDevicesFragment() {
  }

  private void initialize(View rootView) {
    setHasOptionsMenu(true);
    mUrlToUrlMetadata = new HashMap<>();
    getActivity().getActionBar().setTitle(R.string.title_nearby_beacons);
    setListAdapter(new NearbyDevicesAdapter());
    initializeScanningAnimation(rootView);
    mIsDemoMode = getArguments().getBoolean("isDemoMode");
    // Only scan for beacons when not in demo mode
    if (mIsDemoMode) {
      MetadataResolver.findDemoUrlMetadata(getActivity(), NearbyDevicesFragment.this);
    } else {
      initializeBluetooth();
    }
  }

  private void initializeBluetooth() {
    // Initializes a Bluetooth adapter. For API version 18 and above,
    // get a reference to BluetoothAdapter through BluetoothManager.
    final BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = bluetoothManager.getAdapter();
  }

  private void initializeScanningAnimation(View rootView) {
    mScanningImageView = (ImageView) rootView.findViewById(R.id.imageView_nearbyDevicesScanning);
    mScanningImageView.setBackgroundResource(R.drawable.scanning_animation);
    mScanningAnimationDrawable = (AnimationDrawable) mScanningImageView.getBackground();
    mScanningAnimationDrawable.start();
  }


  /////////////////////////////////
  // accessors
  /////////////////////////////////

  private BluetoothLeScannerCompat getLeScanner() {
    return BluetoothLeScannerCompatProvider.getBluetoothLeScannerCompat(getActivity());
  }

  private NearbyDevicesAdapter getNearbyDevicesAdapter() {
    return (NearbyDevicesAdapter)getListAdapter();
  }


  /////////////////////////////////
  // callbacks
  /////////////////////////////////

  public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
    mLayoutInflater = layoutInflater;
    View rootView = layoutInflater.inflate(R.layout.fragment_nearby_devices, container, false);
    initialize(rootView);
    return rootView;
  }

  @Override
  public void onResume() {
    super.onResume();
    if (!mIsDemoMode) {
      getActivity().getActionBar().setTitle(R.string.title_nearby_beacons);
      startScanning();
    } else {
      getActivity().getActionBar().setTitle(R.string.title_nearby_beacons_demo);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (!mIsDemoMode) {
      stopScanning();
    }
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    if (mIsDemoMode) {
      menu.findItem(R.id.action_config).setVisible(false);
      menu.findItem(R.id.action_about).setVisible(false);
      menu.findItem(R.id.action_demo).setVisible(false);
    }
    else {
      menu.findItem(R.id.action_config).setVisible(true);
      menu.findItem(R.id.action_about).setVisible(true);
      menu.findItem(R.id.action_demo).setVisible(true);
    }
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

  @Override
  public void onListItemClick (ListView l, View v, int position, long id) {
    // Get the url for the given item
    String url = getUrlFromDeviceSighting(getNearbyDevicesAdapter().getItem(position));
    String siteUrl = mUrlToUrlMetadata.get(url).siteUrl;
    if (siteUrl != null) {
      // Open the url in the browser
      openUrlInBrowser(siteUrl);
    } else {
      Toast.makeText(getActivity(), "No URL found.", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onUrlMetadataReceived(String id, MetadataResolver.UrlMetadata urlMetadata) {
    mUrlToUrlMetadata.put(id, urlMetadata);
    // If we don't want to wait for another sighting
    getNearbyDevicesAdapter().notifyDataSetChanged();
  }

  @Override
  public void onDemoUrlMetadataReceived(String id, MetadataResolver.UrlMetadata urlMetadata) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(4);
    byteBuffer.putInt(id.hashCode());
    byte[] byteBufferArray = byteBuffer.array();
    String idHash = "http://" + Base64.encodeToString(byteBufferArray, 0, 4, Base64.DEFAULT);
    mUrlToUrlMetadata.put(idHash, urlMetadata);

    byte[] advertisingPacket = new byte[0];
    try {
      advertisingPacket = BeaconHelper.createAdvertisingPacket(idHash);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }

    // Create dummy bluetooth address
    BluetoothDevice bluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(generateMockBluetoothAddress(id.hashCode()));
    // Build the scan record from the advertising packet
    ScanRecord scanRecord = ScanRecord.parseFromBytes(advertisingPacket);
    // Build the scan result from the scan record
    ScanResult scanResult = new ScanResult(bluetoothDevice, scanRecord, -20, 0);
    // Add the demo beacon with a very long timeout
    getNearbyDevicesAdapter().add(scanResult, 20, Integer.MAX_VALUE);
    // If we don't want to wait for another sighting
    getNearbyDevicesAdapter().notifyDataSetChanged();
  }

  private static String generateMockBluetoothAddress(int hashCode) {
    String mockAddress = "00:11";
    for (int i = 0; i < 4; i++) {
      mockAddress += String.format(":%02X", hashCode & 0xFF);
      hashCode = hashCode >> 8;
    }
    return mockAddress;
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
              new byte[]{},
              new byte[]{});
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
        getNearbyDevicesAdapter().clear();
      }
    });
  }

  private void handleScanMatch(final ScanResult scanResult) {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        UriBeacon uriBeacon = UriBeacon.parseFromBytes(scanResult.getScanRecord().getBytes());
        if (uriBeacon != null) {
          int txPowerLevel = uriBeacon.getTxPowerLevel();
          String url = uriBeacon.getUriString();
          if (!mUrlToUrlMetadata.containsKey(url)) {
            mUrlToUrlMetadata.put(url, null);
            MetadataResolver.findUrlMetadata(getActivity(), NearbyDevicesFragment.this, url);
          } else if (mUrlToUrlMetadata.get(url) != null) {
            getNearbyDevicesAdapter().add(scanResult, txPowerLevel, BEACON_EXPIRATION_DURATION);
          }
        }
        updateScanningAnimation();
      }
    });
  }

  private void updateScanningAnimation() {
    if (getNearbyDevicesAdapter().getCount() > 0) {
      if (mScanningAnimationDrawable.isRunning()) {
        mScanningAnimationDrawable.stop();
      }
    } else {
      if (!mScanningAnimationDrawable.isRunning()) {
        mScanningAnimationDrawable.start();
      }
    }
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
  private class NearbyDevicesAdapter extends ScanResultAdapter {

    NearbyDevicesAdapter() {
      super(mLayoutInflater);
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
      // Get the list view item for the given position
      if (view == null) {
        view = getActivity().getLayoutInflater().inflate(R.layout.list_item_nearby_device, viewGroup, false);
      }
      // Get the url for the given position
      String url = getUrlFromDeviceSighting(getItem(i));

      // Get the metadata for this url
      MetadataResolver.UrlMetadata urlMetadata = mUrlToUrlMetadata.get(url);

      // If the metadata exists
      if (urlMetadata != null) {
        // Set the title text
        TextView infoView = (TextView) view.findViewById(R.id.title);
        infoView.setText(urlMetadata.title);

        // Set the site url text
        infoView = (TextView) view.findViewById(R.id.url);
        infoView.setText(urlMetadata.siteUrl);

        // Set the description text
        infoView = (TextView) view.findViewById(R.id.description);
        infoView.setText(urlMetadata.description);

        // Set the favicon image
        ImageView iconView = (ImageView) view.findViewById(R.id.icon);
        iconView.setImageBitmap(urlMetadata.icon);

        // If the metadata does not exist
      } else {
        Log.i(TAG, String.format("Beacon with URL %s has no metadata.", url));
      }
      return view;
    }
  }
}

