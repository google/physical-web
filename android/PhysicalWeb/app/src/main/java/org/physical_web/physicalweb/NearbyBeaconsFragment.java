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

import org.physical_web.physicalweb.PwoMetadata.BleMetadata;
import org.physical_web.physicalweb.PwoMetadata.UrlMetadata;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.ListFragment;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.uribeacon.scan.util.RangingUtils;
import org.uribeacon.scan.util.RegionResolver;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
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
public class NearbyBeaconsFragment extends ListFragment
                                   implements PwsClient.ResolveScanCallback,
                                              PwoDiscoverer.PwoDiscoveryCallback,
                                              SwipeRefreshWidget.OnRefreshListener {

  private static final String TAG = "NearbyBeaconsFragment";
  private static final long FIRST_SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(2);
  private static final long SECOND_SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(5);
  private static final long THIRD_SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(10);
  private HashMap<String, PwoMetadata> mUrlToPwoMetadata;
  private List<PwoMetadata> mPwoMetadataQueue;
  private List<PwoDiscoverer> mPwoDiscoverers;
  private AnimationDrawable mScanningAnimationDrawable;
  private Handler mHandler;
  private NearbyBeaconsAdapter mNearbyDeviceAdapter;
  private SwipeRefreshWidget mSwipeRefreshWidget;
  private boolean mDebugViewEnabled = false;
  private boolean mSecondScanComplete;

  // The display of gathered urls happens as follows
  // 0. Begin scan
  // 1. Sort and show all urls (mFirstScanTimeout)
  // 2. Sort and show all new urls beneath the first set (mSecondScanTimeout)
  // 3. Show each new url at bottom of list as it comes in
  // 4. Stop scanning (mThirdScanTimeout)

  // Run when the FIRST_SCAN_MILLIS has elapsed.
  private Runnable mFirstScanTimeout = new Runnable() {
    @Override
    public void run() {
      Log.d(TAG, "running first scan timeout");
      if (!mPwoMetadataQueue.isEmpty()) {
        mScanningAnimationDrawable.stop();
        mSwipeRefreshWidget.setRefreshing(false);
        emptyPwoMetadataQueue();
        fadeInListView();
      }
    }
  };

  // Run when the SECOND_SCAN_MILLIS has elapsed.
  private Runnable mSecondScanTimeout = new Runnable() {
    @Override
    public void run() {
      Log.d(TAG, "running second scan timeout");
      emptyPwoMetadataQueue();
      if (mScanningAnimationDrawable.isRunning()) {
        mScanningAnimationDrawable.stop();
        mSwipeRefreshWidget.setRefreshing(false);
        fadeInListView();
      }
      mSecondScanComplete = true;
    }
  };

  // Run when the THIRD_SCAN_MILLIS has elapsed.
  private Runnable mThirdScanTimeout = new Runnable() {
    @Override
    public void run() {
      Log.d(TAG, "running third scan timeout");
      stopScanning();
    }
  };

  private AdapterView.OnItemLongClickListener mAdapterViewItemLongClickListener =
      new AdapterView.OnItemLongClickListener() {
    public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
      mDebugViewEnabled = !mDebugViewEnabled;
      mNearbyDeviceAdapter.notifyDataSetChanged();
      return true;
    }
  };

  public static NearbyBeaconsFragment newInstance() {
    return new NearbyBeaconsFragment();
  }

  private void initialize(View rootView) {
    setHasOptionsMenu(true);
    mUrlToPwoMetadata = new HashMap<>();
    mPwoMetadataQueue = new ArrayList<>();
    mPwoDiscoverers = new ArrayList<>();
    mHandler = new Handler();

    mSwipeRefreshWidget = (SwipeRefreshWidget) rootView.findViewById(R.id.swipe_refresh_widget);
    mSwipeRefreshWidget.setColorSchemeResources(R.color.swipe_refresh_widget_first_color,
                                                R.color.swipe_refresh_widget_second_color);
    mSwipeRefreshWidget.setOnRefreshListener(this);

    mPwoDiscoverers.add(new MdnsPwoDiscoverer(getActivity()));
    mPwoDiscoverers.add(new SsdpPwoDiscoverer(getActivity()));
    mPwoDiscoverers.add(new BlePwoDiscoverer(getActivity()));
    for (PwoDiscoverer pwoDiscoverer : mPwoDiscoverers) {
      pwoDiscoverer.setCallback(this);
    }

    getActivity().getActionBar().setTitle(R.string.title_nearby_beacons);
    mNearbyDeviceAdapter = new NearbyBeaconsAdapter();
    setListAdapter(mNearbyDeviceAdapter);
    initializeScanningAnimation(rootView);
    ListView listView = (ListView) rootView.findViewById(android.R.id.list);
    listView.setOnItemLongClickListener(mAdapterViewItemLongClickListener);
  }

  private void initializeScanningAnimation(View rootView) {
    TextView tv = (TextView) rootView.findViewById(android.R.id.empty);
    //Get the top drawable
    mScanningAnimationDrawable = (AnimationDrawable) tv.getCompoundDrawables()[1];
  }

  public View onCreateView(LayoutInflater layoutInflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View rootView = layoutInflater.inflate(R.layout.fragment_nearby_beacons, container, false);
    initialize(rootView);
    return rootView;
  }

  @Override
  public void onResume() {
    super.onResume();
    getActivity().getActionBar().setTitle(R.string.title_nearby_beacons);
    getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
    startScanning();
  }

  @Override
  public void onPause() {
    super.onPause();
    stopScanning();
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(R.id.action_config).setVisible(true);
    menu.findItem(R.id.action_about).setVisible(true);
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    // If we are scanning
    if (mScanningAnimationDrawable.isRunning()) {
      // Don't respond to touch events
      return;
    }
    // Get the url for the given item
    PwoMetadata pwoMetadata = mNearbyDeviceAdapter.getItem(position);
    Intent intent = pwoMetadata.createNavigateToUrlIntent(getActivity());
    startActivity(intent);
  }

  @Override
  public void onUrlMetadataReceived(PwoMetadata pwoMetadata) {
    mNearbyDeviceAdapter.notifyDataSetChanged();
  }

  @Override
  public void onUrlMetadataIconReceived(PwoMetadata pwoMetadata) {
    mNearbyDeviceAdapter.notifyDataSetChanged();
  }

  private void stopScanning() {
    // Cancel the scan timeout callback if still active or else it may fire later.
    mHandler.removeCallbacks(mFirstScanTimeout);
    mHandler.removeCallbacks(mSecondScanTimeout);
    mHandler.removeCallbacks(mThirdScanTimeout);
    // Stop the scanners
    for (PwoDiscoverer pwoDiscoverer : mPwoDiscoverers) {
      pwoDiscoverer.stopScan();
    }
    // Change the display appropriately
    mSwipeRefreshWidget.setRefreshing(false);
    mScanningAnimationDrawable.stop();
  }

  private void startScanning() {
    // Clear any stored url data
    mUrlToPwoMetadata.clear();
    mPwoMetadataQueue.clear();
    mNearbyDeviceAdapter.clear();
    // Start the scan
    for (PwoDiscoverer pwoDiscoverer : mPwoDiscoverers) {
      pwoDiscoverer.startScan();
    }
    // Stops scanning after the predefined scan time has elapsed.
    mHandler.postDelayed(mFirstScanTimeout, FIRST_SCAN_TIME_MILLIS);
    mHandler.postDelayed(mSecondScanTimeout, SECOND_SCAN_TIME_MILLIS);
    mHandler.postDelayed(mThirdScanTimeout, THIRD_SCAN_TIME_MILLIS);
    mSecondScanComplete = false;
    // Change the display appropriately
    mScanningAnimationDrawable.start();
    getListView().setVisibility(View.INVISIBLE);
  }

  @Override
  public void onRefresh() {
    stopScanning();
    mSwipeRefreshWidget.setRefreshing(true);
    startScanning();
  }

  @Override
  public void onPwoDiscovered(PwoMetadata pwoMetadata) {
    if (pwoMetadata.hasBleMetadata()) {
      BleMetadata bleMetadata = pwoMetadata.bleMetadata;
      mNearbyDeviceAdapter.updateItem(pwoMetadata.url, bleMetadata.deviceAddress, bleMetadata.rssi,
                                      bleMetadata.txPower);
    }

    if (!mUrlToPwoMetadata.containsKey(pwoMetadata.url)) {
      mUrlToPwoMetadata.put(pwoMetadata.url, pwoMetadata);
      PwsClient.getInstance(getActivity()).findUrlMetadata(pwoMetadata, this, TAG);
      mPwoMetadataQueue.add(pwoMetadata);
      if (mSecondScanComplete) {
        emptyPwoMetadataQueue();
      }
    }
  }

  private void emptyPwoMetadataQueue() {
    Collections.sort(mPwoMetadataQueue);
    for (PwoMetadata pwoMetadata : mPwoMetadataQueue) {
      mNearbyDeviceAdapter.addItem(pwoMetadata);
    }
    mPwoMetadataQueue.clear();
  }

  private void fadeInListView() {
    ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(getListView(), "alpha", 0, 1);
    alphaAnimation.setDuration(400);
    alphaAnimation.setInterpolator(new DecelerateInterpolator());
    alphaAnimation.start();
  }

  // Adapter for holding beacons found through scanning.
  private class NearbyBeaconsAdapter extends BaseAdapter {

    public final RegionResolver mRegionResolver;
    private List<PwoMetadata> mPwoMetadataList;

    NearbyBeaconsAdapter() {
      mRegionResolver = new RegionResolver();
      mPwoMetadataList = new ArrayList<>();
    }

    public void updateItem(String url, String address, int rssi, int txPower) {
      mRegionResolver.onUpdate(address, rssi, txPower);
    }

    public void addItem(PwoMetadata pwoMetadata) {
      mPwoMetadataList.add(pwoMetadata);
    }

    @Override
    public int getCount() {
      return mPwoMetadataList.size();
    }

    @Override
    public PwoMetadata getItem(int i) {
      return mPwoMetadataList.get(i);
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
        view = getActivity().getLayoutInflater().inflate(R.layout.list_item_nearby_beacon,
                                                         viewGroup, false);
      }

      // Reference the list item views
      TextView titleTextView = (TextView) view.findViewById(R.id.title);
      TextView urlTextView = (TextView) view.findViewById(R.id.url);
      TextView descriptionTextView = (TextView) view.findViewById(R.id.description);
      ImageView iconImageView = (ImageView) view.findViewById(R.id.icon);

      // Get the metadata for the given position
      PwoMetadata pwoMetadata = getItem(i);
      if (pwoMetadata.hasUrlMetadata()) {
        // If the url metadata exists
        UrlMetadata urlMetadata = pwoMetadata.urlMetadata;
        // Set the title text
        titleTextView.setText(urlMetadata.title);
        // Set the url text
        urlTextView.setText(urlMetadata.displayUrl);
        // Set the description text
        descriptionTextView.setText(urlMetadata.description);
        // Set the favicon image
        iconImageView.setImageBitmap(urlMetadata.icon);
      } else {
        // If metadata does not yet exist
        // Clear the children views content (in case this is a recycled list item view)
        titleTextView.setText("");
        iconImageView.setImageDrawable(null);
        // Set the url text to be the beacon's advertised url
        urlTextView.setText(pwoMetadata.url);
        // Set the description text to show loading status
        descriptionTextView.setText(R.string.metadata_loading);
      }

      if (mDebugViewEnabled) {
        // If we should show the ranging data
        updateDebugView(pwoMetadata, view);
        view.findViewById(R.id.ranging_debug_container).setVisibility(View.VISIBLE);
        view.findViewById(R.id.metadata_debug_container).setVisibility(View.VISIBLE);
        PwsClient.getInstance(getActivity()).useDevEndpoint();
      } else {
        // Otherwise ensure it is not shown
        view.findViewById(R.id.ranging_debug_container).setVisibility(View.GONE);
        view.findViewById(R.id.metadata_debug_container).setVisibility(View.GONE);
        PwsClient.getInstance(getActivity()).useProdEndpoint();
      }

      return view;
    }

    private void updateDebugView(PwoMetadata pwoMetadata, View view) {
      // Ranging debug line
      TextView txPowerView = (TextView) view.findViewById(R.id.ranging_debug_tx_power);
      TextView rssiView = (TextView) view.findViewById(R.id.ranging_debug_rssi);
      TextView distanceView = (TextView) view.findViewById(R.id.ranging_debug_distance);
      TextView regionView = (TextView) view.findViewById(R.id.ranging_debug_region);
      if (pwoMetadata.hasBleMetadata()) {
        BleMetadata bleMetadata = pwoMetadata.bleMetadata;

        int txPower = bleMetadata.txPower;
        String txPowerString = getString(R.string.ranging_debug_tx_power_prefix)
            + String.valueOf(txPower);
        txPowerView.setText(txPowerString);

        String deviceAddress = bleMetadata.deviceAddress;
        int rssi = mRegionResolver.getSmoothedRssi(deviceAddress);
        String rssiString = getString(R.string.ranging_debug_rssi_prefix) + String.valueOf(rssi);
        rssiView.setText(rssiString);

        double distance = mRegionResolver.getDistance(deviceAddress);
        String distanceString = getString(R.string.ranging_debug_distance_prefix)
            + new DecimalFormat("##.##").format(distance);
        distanceView.setText(distanceString);

        int region = mRegionResolver.getRegion(deviceAddress);
        String regionString = getString(R.string.ranging_debug_region_prefix)
            + RangingUtils.toString(region);
        regionView.setText(regionString);
      } else {
        txPowerView.setText("");
        rssiView.setText("");
        distanceView.setText("");
        regionView.setText("");
      }

      // Metadata debug line
      float scanTime = pwoMetadata.scanMillis / 1000.0f;
      String scanTimeString = getString(R.string.metadata_debug_scan_time_prefix)
          + new DecimalFormat("##.##s").format(scanTime);
      TextView scanTimeView = (TextView) view.findViewById(R.id.metadata_debug_scan_time);
      scanTimeView.setText(scanTimeString);

      TextView rankView = (TextView) view.findViewById(R.id.metadata_debug_rank);
      TextView pwsTripTimeView = (TextView) view.findViewById(R.id.metadata_debug_pws_trip_time);
      if (pwoMetadata.hasUrlMetadata()) {
        UrlMetadata urlMetadata = pwoMetadata.urlMetadata;
        float rank = urlMetadata.rank;
        String rankString = getString(R.string.metadata_debug_rank_prefix)
            + new DecimalFormat("##.##").format(rank);
        rankView.setText(rankString);

        float pwsTripTime = pwoMetadata.pwsTripMillis / 1000.0f;
        String pwsTripTimeString = "" + getString(R.string.metadata_debug_pws_trip_time_prefix)
            + new DecimalFormat("##.##s").format(pwsTripTime);
        pwsTripTimeView.setText(pwsTripTimeString);
      } else {
        rankView.setText("");
        pwsTripTimeView.setText("");
      }
    }

    public void clear() {
      mPwoMetadataList.clear();
      notifyDataSetChanged();
    }
  }
}

