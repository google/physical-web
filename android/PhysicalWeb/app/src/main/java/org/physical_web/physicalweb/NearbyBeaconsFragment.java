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

package org.physical_web.physicalweb;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.uribeacon.beacon.UriBeacon;
import org.uribeacon.scan.compat.ScanRecord;
import org.uribeacon.scan.compat.ScanResult;
import org.uribeacon.scan.util.RegionResolver;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
public class NearbyBeaconsFragment extends ListFragment implements MetadataResolver.MetadataResolverCallback, SwipeRefreshWidget.OnRefreshListener, MdnsUrlDiscoverer.MdnsUrlDiscovererCallback {

  private static final String TAG = "NearbyBeaconsFragment";
  private static final long SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(3);
  private final BluetoothAdapter.LeScanCallback mLeScanCallback = new LeScanCallback();
  private BluetoothAdapter mBluetoothAdapter;
  private HashMap<String, MetadataResolver.UrlMetadata> mUrlToUrlMetadata;
  private AnimationDrawable mScanningAnimationDrawable;
  private boolean mIsDemoMode;
  private boolean mIsScanRunning;
  private Handler mHandler;
  private NearbyBeaconsAdapter mNearbyDeviceAdapter;
  private Parcelable[] mScanFilterUuids;
  private SwipeRefreshWidget mSwipeRefreshWidget;
  private MdnsUrlDiscoverer mMdnsUrlDiscoverer;
  // Run when the SCAN_TIME_MILLIS has elapsed.
  private Runnable mScanTimeout = new Runnable() {
    @Override
    public void run() {
      mScanningAnimationDrawable.stop();
      scanLeDevice(false);
      mMdnsUrlDiscoverer.stopScanning();
      mNearbyDeviceAdapter.notifyDataSetChanged();
    }
  };

  public static NearbyBeaconsFragment newInstance(boolean isDemoMode) {
    NearbyBeaconsFragment nearbyBeaconsFragment = new NearbyBeaconsFragment();
    Bundle bundle = new Bundle();
    bundle.putBoolean("isDemoMode", isDemoMode);
    nearbyBeaconsFragment.setArguments(bundle);
    return nearbyBeaconsFragment;
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
    mHandler = new Handler();
    mScanFilterUuids = new ParcelUuid[]{UriBeacon.URI_SERVICE_UUID};

    mSwipeRefreshWidget = (SwipeRefreshWidget) rootView.findViewById(R.id.swipe_refresh_widget);
    mSwipeRefreshWidget.setColorSchemeResources(R.color.swipe_refresh_widget_first_color, R.color.swipe_refresh_widget_second_color);
    mSwipeRefreshWidget.setOnRefreshListener(this);

    mMdnsUrlDiscoverer = new MdnsUrlDiscoverer(getActivity(), NearbyBeaconsFragment.this);

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
      mScanningAnimationDrawable.start();
      scanLeDevice(true);
      mMdnsUrlDiscoverer.startScanning();
    } else {
      getActivity().getActionBar().setTitle(R.string.title_nearby_beacons_demo);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (!mIsDemoMode) {
      if (mIsScanRunning) {
        scanLeDevice(false);
        mMdnsUrlDiscoverer.stopScanning();
      }
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
    // If we are scanning
    if (mIsScanRunning) {
      // Don't respond to touch events
      return;
    }
    // Get the url for the given item
    String url = mNearbyDeviceAdapter.getUrlForListItem(position);
    String siteUrl = mUrlToUrlMetadata.get(url).siteUrl;
    if (siteUrl != null) {
      // Open the url in the browser
      openUrlInBrowser(siteUrl);
    } else {
      Toast.makeText(getActivity(), "No URL found.", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onUrlMetadataReceived(String url, MetadataResolver.UrlMetadata urlMetadata) {
    mUrlToUrlMetadata.put(url, urlMetadata);
  }

  @Override
  public void onUrlMetadataIconReceived() {
  }

  @Override
  public void onDemoUrlMetadataReceived(String url, MetadataResolver.UrlMetadata urlMetadata) {
    // Update the hash table
    mUrlToUrlMetadata.put(url, urlMetadata);
    // Fabricate the adapter values so that we can show these demo beacons
    String mockAddress = generateMockBluetoothAddress(url.hashCode());
    int mockRssi = 0;
    int mockTxPower = 0;
    mNearbyDeviceAdapter.update(url, mockAddress, mockRssi, mockTxPower);
    // Inform the list adapter of the new data
    mNearbyDeviceAdapter.notifyDataSetChanged();
    // Stop the refresh animation
    mSwipeRefreshWidget.setRefreshing(false);
  }

  @SuppressWarnings("deprecation")
  private void scanLeDevice(final boolean enable) {
    if (mIsScanRunning != enable) {
      mIsScanRunning = enable;
      // If we should start scanning
      if (enable) {
        // Stops scanning after the predefined scan time has elapsed.
        mHandler.postDelayed(mScanTimeout, SCAN_TIME_MILLIS);
        // Clear any stored url data
        mUrlToUrlMetadata.clear();
        mNearbyDeviceAdapter.clear();
        // Start the scan
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        // If we should stop scanning
      } else {
        // Cancel the scan timeout callback if still active or else it may fire later.
        mHandler.removeCallbacks(mScanTimeout);
        // Stop the scan
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mSwipeRefreshWidget.setRefreshing(false);
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

  private void openUrlInBrowser(String url) {
    // Ensure an http prefix exists in the url
    if (!URLUtil.isNetworkUrl(url)) {
      url = "http://" + url;
    }
    // Route through the proxy server go link
    url = createUrlProxyGoLink(url);
    // Open the browser and point it to the given url
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse(url));
    startActivity(intent);
  }

  private String createUrlProxyGoLink(String url) {
    try {
      url = getString(R.string.proxy_go_link_base_url) + URLEncoder.encode(url, "UTF-8");
      Log.d(TAG, "url: " + url);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return url;
  }

  @Override
  public void onRefresh() {
    mSwipeRefreshWidget.setRefreshing(true);
    if (!mIsDemoMode) {
      mScanningAnimationDrawable.start();
      scanLeDevice(true);
      mMdnsUrlDiscoverer.startScanning();
    } else {
      mNearbyDeviceAdapter.clear();
      MetadataResolver.findDemoUrlMetadata(getActivity(), NearbyBeaconsFragment.this);
    }
  }

  @Override
  public void onMdnsUrlFound(String url) {
    if (!mUrlToUrlMetadata.containsKey(url)) {
      mUrlToUrlMetadata.put(url, null);
      MetadataResolver.findUrlMetadata(getActivity(), NearbyBeaconsFragment.this, url);
      // Fabricate the adapter values so that we can show these ersatz beacons
      String mockAddress = generateMockBluetoothAddress(url.hashCode());
      int mockRssi = 0;
      int mockTxPower = 0;
      // Update the ranging info
      mNearbyDeviceAdapter.update(url, mockAddress, mockRssi, mockTxPower);
      // Force the device to be added to the listview (since it has no metadata)
      mNearbyDeviceAdapter.add(url, mockAddress);
    }
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
              // If we haven't yet seen this url
              if (!mUrlToUrlMetadata.containsKey(url)) {
                mUrlToUrlMetadata.put(url, null);
                // Fetch the metadata for this url
                MetadataResolver.findUrlMetadata(getActivity(), NearbyBeaconsFragment.this, url);
              }
              // Tell the adapter to update stored data for this url
              mNearbyDeviceAdapter.update(url, scanResult.getDevice().getAddress(), scanResult.getRssi(), txPowerLevel);
            }
          }
        });
      }
    }
  }

  // Adapter for holding beacons found through scanning.
  private class NearbyBeaconsAdapter extends BaseAdapter {

    public final RegionResolver mRegionResolver;
    private final HashMap<String, String> mUrlToDeviceAddress;
    private List<String> mSortedDevices;
    private Comparator<String> mComparator = new Comparator<String>() {
      @Override
      public int compare(String address, String otherAddress) {
        // Sort by the stabilized region of the device, unless
        // they are the same, in which case sort by distance.
        final String nearest = mRegionResolver.getNearestAddress();
        if (address.equals(nearest)) {
          return -1;
        }
        if (otherAddress.equals(nearest)) {
          return 1;
        }
        int r1 = mRegionResolver.getRegion(address);
        int r2 = mRegionResolver.getRegion(otherAddress);
        if (r1 != r2) {
          return ((Integer) r1).compareTo(r2);
        }
        // The two devices are in the same region, sort by device address.
        return address.compareTo(otherAddress);
      }
    };

    NearbyBeaconsAdapter() {
      mUrlToDeviceAddress = new HashMap<>();
      mRegionResolver = new RegionResolver();
      mSortedDevices = null;
    }

    public void update(String url, String address, int rssi, int txPower) {
      // Always update the ranging data for the given device
      mRegionResolver.onUpdate(address, rssi, txPower);
      // If we haven't yet stored this url and device AND
      // if the metadata for this url has already been fetched
      if ((mUrlToDeviceAddress.get(url) == null) && (mUrlToUrlMetadata.get(url) != null)) {
        // Update the url/device hash table
        add(url, address);
      }
    }

    public void add(String url, String address) {
      mUrlToDeviceAddress.put(url, address);
    }

    @Override
    public int getCount() {
      return mUrlToDeviceAddress.size();
    }

    @Override
    public String getItem(int i) {
      if (mSortedDevices == null) {
        mSortedDevices = new ArrayList<>(mUrlToDeviceAddress.values());
        Collections.sort(mSortedDevices, mComparator);
      }
      return mSortedDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
      return i;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
      // Get the list view item for the given position
      if (view == null) {
        view = getActivity().getLayoutInflater().inflate(R.layout.list_item_nearby_beacon, viewGroup, false);
      }

      // Get the url for the given position
      String url = getUrlForListItem(i);

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
      }

      return view;
    }

    public String getUrlForListItem(int i) {
      String address = getItem(i);
      for (String url : mUrlToDeviceAddress.keySet()) {
        if (mUrlToDeviceAddress.get(url).equals(address)) {
          return url;
        }
      }
      return null;
    }

    @Override
    public void notifyDataSetChanged() {
      mSortedDevices = null;
      super.notifyDataSetChanged();
    }

    public void clear() {
      mUrlToDeviceAddress.clear();
      notifyDataSetChanged();
    }
  }
}

