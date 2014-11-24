package org.physical_web.physicalweb;

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
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.uribeacon.beacon.UriBeacon;
import org.uribeacon.scan.compat.ScanRecord;
import org.uribeacon.scan.compat.ScanResult;
import org.uribeacon.widget.ScanResultAdapter;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class shows the ui list for all
 * detected nearby beacons.
 * It also listens for tap events
 * on items within the list.
 * Tapped list items then launch
 * the browser and point that browser
 * to the given list items url.
 */
public class NearbyBeaconsFragment extends ListFragment implements MetadataResolver.MetadataResolverCallback {

  private static final String TAG = "NearbyBeaconsFragment";
  private static final int BEACON_EXPIRATION_DURATION = Integer.MAX_VALUE;
  private static final long SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(10);
  private final BluetoothAdapter.LeScanCallback mLeScanCallback = new LeScanCallback();
  private LayoutInflater mLayoutInflater;
  private BluetoothAdapter mBluetoothAdapter;
  private HashMap<String, MetadataResolver.UrlMetadata> mUrlToUrlMetadata;
  private AnimationDrawable mScanningAnimationDrawable;
  private boolean mIsDemoMode;
  private boolean mIsScanRunning;
  private Handler mHandler;
  private NearbyBeaconsAdapter mNearbyDeviceAdapter;
  private Parcelable[] mScanFilterUuids;
  private HashMap<String, ScanInfo> mUrlToScanInfo;

  // Run when the SCAN_TIME_MILLIS has elapsed.
  private Runnable mScanTimeout = new Runnable() {
    @Override
    public void run() {
      scanLeDevice(false);
    }
  };

  public NearbyBeaconsFragment() {
  }

  public static NearbyBeaconsFragment newInstance(boolean isDemoMode) {
    NearbyBeaconsFragment nearbyBeaconsFragment = new NearbyBeaconsFragment();
    Bundle bundle = new Bundle();
    bundle.putBoolean("isDemoMode", isDemoMode);
    nearbyBeaconsFragment.setArguments(bundle);
    return nearbyBeaconsFragment;
  }

  private static String getUrlFromDeviceSighting(ScanResultAdapter.DeviceSighting deviceSighting) {
    UriBeacon uriBeacon = UriBeacon.parseFromBytes(deviceSighting.scanResult.getScanRecord().getBytes());
    return uriBeacon.getUriString();
  }

  private static String generateMockBluetoothAddress(int hashCode) {
    String mockAddress = "00:11";
    for (int i = 0; i < 4; i++) {
      mockAddress += String.format(":%02X", hashCode & 0xFF);
      hashCode = hashCode >> 8;
    }
    return mockAddress;
  }

  private void initialize(View rootView) {
    setHasOptionsMenu(true);
    mUrlToUrlMetadata = new HashMap<>();
    mUrlToScanInfo = new HashMap<>();
    mHandler = new Handler();
    mScanFilterUuids = new ParcelUuid[]{UriBeacon.URI_SERVICE_UUID};
    getActivity().getActionBar().setTitle(R.string.title_nearby_beacons);
    mNearbyDeviceAdapter = new NearbyBeaconsAdapter();
    setListAdapter(mNearbyDeviceAdapter);
    initializeScanningAnimation(rootView);
    mIsDemoMode = getArguments().getBoolean("isDemoMode");
    // Only scan for beacons when not in demo mode
    if (mIsDemoMode) {
      getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
      MetadataResolver.findDemoUrlMetadata(getActivity(), NearbyBeaconsFragment.this);
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
    TextView tv = (TextView) rootView.findViewById(android.R.id.empty);
    //Get the top drawable
    mScanningAnimationDrawable = (AnimationDrawable) tv.getCompoundDrawables()[1];
    mScanningAnimationDrawable.start();
  }

  public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
    mLayoutInflater = layoutInflater;
    View rootView = layoutInflater.inflate(R.layout.fragment_nearby_beacons, container, false);
    initialize(rootView);
    return rootView;
  }

  @Override
  public void onResume() {
    super.onResume();
    if (!mIsDemoMode) {
      getActivity().getActionBar().setTitle(R.string.title_nearby_beacons);
      getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
      scanLeDevice(true);
    } else {
      getActivity().getActionBar().setTitle(R.string.title_nearby_beacons_demo);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (!mIsDemoMode) {
      scanLeDevice(false);
    }
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    if (mIsDemoMode) {
      menu.findItem(R.id.action_config).setVisible(false);
      menu.findItem(R.id.action_about).setVisible(false);
      menu.findItem(R.id.action_demo).setVisible(false);
    } else {
      menu.findItem(R.id.action_config).setVisible(true);
      menu.findItem(R.id.action_about).setVisible(true);
      menu.findItem(R.id.action_demo).setVisible(true);
    }
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    // Get the url for the given item
    String url = getUrlFromDeviceSighting(mNearbyDeviceAdapter.getItem(position));
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
    ScanInfo scanInfo = mUrlToScanInfo.get(id);
    mNearbyDeviceAdapter.add(scanInfo.scanResult, scanInfo.txPowerLevel, BEACON_EXPIRATION_DURATION);
  }

  @Override
  public void onUrlMetadataIconReceived() {
    mNearbyDeviceAdapter.notifyDataSetChanged();
  }


  @Override
  public void onDemoUrlMetadataReceived(String id, MetadataResolver.UrlMetadata urlMetadata) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(4);
    byteBuffer.putInt(id.hashCode());
    byte[] byteBufferArray = byteBuffer.array();

    String idHash = "https://" +
        Base64.encodeToString(byteBufferArray, 0, 4, Base64.NO_PADDING).replace("\n", "");
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
    mNearbyDeviceAdapter.add(scanResult, 20, Integer.MAX_VALUE);
    // If we don't want to wait for another sighting
    mNearbyDeviceAdapter.notifyDataSetChanged();
  }

  @SuppressWarnings("deprecation")
  private void scanLeDevice(final boolean enable) {
    if (mIsScanRunning != enable) {
      mIsScanRunning = enable;
      if (enable) {
        // Stops scanning after the predefined scan time has elapsed.
        mHandler.postDelayed(mScanTimeout, SCAN_TIME_MILLIS);
        mNearbyDeviceAdapter.clear();
        mBluetoothAdapter.startLeScan(mLeScanCallback);
      } else {
        // Cancel the scan timeout callback if still active or else it may fire later.
        mHandler.removeCallbacks(mScanTimeout);
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
      }
    }
  }

  private boolean leScanMatches(ScanRecord scanRecord) {
    if (mScanFilterUuids == null) {
      return true;
    }
    List services = scanRecord.getServiceUuids();
    if (services != null) {
      for (Parcelable uuid : mScanFilterUuids) {
        if (services.contains(uuid)) {
          return true;
        }
      }
    }
    return false;
  }

  private void updateScanningAnimation() {
    if (mNearbyDeviceAdapter.getCount() > 0) {
      if (mScanningAnimationDrawable.isRunning()) {
        mScanningAnimationDrawable.stop();
      }
    } else {
      if (!mScanningAnimationDrawable.isRunning()) {
        mScanningAnimationDrawable.start();
      }
    }
  }

  private void openUrlInBrowser(String url) {
    // Ensure an http prefix exists in the url
    if (!URLUtil.isNetworkUrl(url)) {
      url = "http://" + url;
    }
    // Open the browser and point it to the given url
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(url));
    startActivity(intent);
  }

  /**
   * Callback for LE scan results.
   */
  private class LeScanCallback implements BluetoothAdapter.LeScanCallback {
    @Override
    public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanBytes) {
      ScanRecord scanRecord = ScanRecord.parseFromBytes(scanBytes);
      if (leScanMatches(scanRecord)) {
        final ScanResult scanResult = new ScanResult(device, scanRecord, rssi, SystemClock.elapsedRealtimeNanos());
        getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            UriBeacon uriBeacon = UriBeacon.parseFromBytes(scanResult.getScanRecord().getBytes());
            if (uriBeacon != null) {
              int txPowerLevel = uriBeacon.getTxPowerLevel();
              String url = uriBeacon.getUriString();
              if (!mUrlToUrlMetadata.containsKey(url)) {
                mUrlToUrlMetadata.put(url, null);
                mUrlToScanInfo.put(url, new ScanInfo(scanResult, txPowerLevel));
                MetadataResolver.findUrlMetadata(getActivity(), NearbyBeaconsFragment.this, url);
              }
            }
            updateScanningAnimation();
          }
        });
      }
    }
  }

  // Adapter for holding beacons found through scanning.
  private class NearbyBeaconsAdapter extends ScanResultAdapter {

    NearbyBeaconsAdapter() {
      super(mLayoutInflater);
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
      // Get the list view item for the given position
      if (view == null) {
        view = getActivity().getLayoutInflater().inflate(R.layout.list_item_nearby_beacon, viewGroup, false);
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

  class ScanInfo {
    ScanResult scanResult;
    int txPowerLevel;

    public ScanInfo(ScanResult scanResult, int txPowerLevel) {
      this.scanResult = scanResult;
      this.txPowerLevel = txPowerLevel;
    }

  }
}

