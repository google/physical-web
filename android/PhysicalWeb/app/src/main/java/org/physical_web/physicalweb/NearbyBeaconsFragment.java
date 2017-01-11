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

import org.physical_web.collection.PhysicalWebCollection;
import org.physical_web.collection.PwPair;
import org.physical_web.collection.PwsResult;
import org.physical_web.collection.UrlDevice;

import android.annotation.SuppressLint;
import android.app.ListFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimationDrawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
                                   implements UrlDeviceDiscoveryService.UrlDeviceDiscoveryListener,
                                              SwipeRefreshWidget.OnRefreshListener {

  private static final String TAG = NearbyBeaconsFragment.class.getSimpleName();
  private static final long FIRST_SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(2);
  private static final long SECOND_SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(5);
  private static final long THIRD_SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(10);
  private List<String> mGroupIdQueue;
  private PhysicalWebCollection mPwCollection = null;
  private TextView mScanningAnimationTextView;
  private AnimationDrawable mScanningAnimationDrawable;
  private Handler mHandler;
  private NearbyBeaconsAdapter mNearbyDeviceAdapter;
  private SwipeRefreshWidget mSwipeRefreshWidget;
  private boolean mSecondScanComplete;
  private boolean mFirstTime;
  private DiscoveryServiceConnection mDiscoveryServiceConnection;
  private boolean mMissedEmptyGroupIdQueue = false;
  private SwipeDismissListViewTouchListener mTouchListener;
  private WifiDirectConnect mWifiDirectConnect;
  private BluetoothSite mBluetoothSite;

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
      if (!mGroupIdQueue.isEmpty()) {
        emptyGroupIdQueue();
        setRefreshWidgetInvisible();
      }
    }
  };

  // Run when the SECOND_SCAN_MILLIS has elapsed.
  private Runnable mSecondScanTimeout = new Runnable() {
    @Override
    public void run() {
      Log.d(TAG, "running second scan timeout");
      emptyGroupIdQueue();
      mSecondScanComplete = true;
      setRefreshWidgetInvisible();
      if (mNearbyDeviceAdapter.getCount() == 0) {
        int tintColor = ContextCompat.getColor(getActivity(), R.color.physical_web_logo);
        mScanningAnimationDrawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
        mScanningAnimationTextView.setText(R.string.empty_nearby_beacons_list_text_no_results);
      }
    }
  };

  // Run when the THIRD_SCAN_MILLIS has elapsed.
  private Runnable mThirdScanTimeout = new Runnable() {
    @Override
    public void run() {
      Log.d(TAG, "running third scan timeout");
      mDiscoveryServiceConnection.disconnect();
    }
  };

  /**
   * The connection to the service that discovers urls.
   */
  private class DiscoveryServiceConnection implements ServiceConnection {
    private UrlDeviceDiscoveryService mDiscoveryService;
    private boolean mRequestCachedUrlDevices;

    @Override
    public synchronized void onServiceConnected(ComponentName className, IBinder service) {
      // Get the service
      UrlDeviceDiscoveryService.LocalBinder localBinder =
          (UrlDeviceDiscoveryService.LocalBinder) service;
      mDiscoveryService = localBinder.getServiceInstance();

      // Start the scanning display
      mDiscoveryService.addCallback(NearbyBeaconsFragment.this);
      if (!mRequestCachedUrlDevices) {
        mDiscoveryService.restartScan();
      }
      mPwCollection = mDiscoveryService.getPwCollection();
      // Make sure cached results get placed in the mGroupIdQueue.
      onUrlDeviceDiscoveryUpdate();
      startScanningDisplay(mDiscoveryService.getScanStartTime(), mDiscoveryService.hasResults());
    }

    @Override
    public synchronized void onServiceDisconnected(ComponentName className) {
      // onServiceDisconnected gets called when the connection is unintentionally disconnected,
      // which should never happen for us since this is a local service
      mDiscoveryService = null;
    }

    public synchronized void connect(boolean requestCachedUrlDevices) {
      if (mDiscoveryService != null) {
        return;
      }

      mRequestCachedUrlDevices = requestCachedUrlDevices;
      Intent intent = new Intent(getActivity(), UrlDeviceDiscoveryService.class);
      getActivity().startService(intent);
      getActivity().bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    public synchronized void disconnect() {
      if (mDiscoveryService == null) {
        return;
      }

      mDiscoveryService.removeCallback(NearbyBeaconsFragment.this);
      mDiscoveryService = null;
      getActivity().unbindService(this);
      stopScanningDisplay();
    }
  }

  private void initialize(View rootView) {
    setHasOptionsMenu(true);
    mGroupIdQueue = new ArrayList<>();
    mHandler = new Handler();

    mSwipeRefreshWidget = (SwipeRefreshWidget) rootView.findViewById(R.id.swipe_refresh_widget);
    mSwipeRefreshWidget.setColorSchemeResources(R.color.swipe_refresh_widget_first_color,
                                                R.color.swipe_refresh_widget_second_color);
    mSwipeRefreshWidget.setOnRefreshListener(this);

    getActivity().getActionBar().setTitle(R.string.title_nearby_beacons);
    mNearbyDeviceAdapter = new NearbyBeaconsAdapter();
    setListAdapter(mNearbyDeviceAdapter);
    //Get the top drawable
    mScanningAnimationTextView = (TextView) rootView.findViewById(android.R.id.empty);
    mScanningAnimationDrawable =
        (AnimationDrawable) mScanningAnimationTextView.getCompoundDrawables()[1];
    ListView listView = (ListView) rootView.findViewById(android.R.id.list);
    mDiscoveryServiceConnection = new DiscoveryServiceConnection();
    mWifiDirectConnect = new WifiDirectConnect(getActivity());
    mBluetoothSite = new BluetoothSite(getActivity());
    mTouchListener =
      new SwipeDismissListViewTouchListener(
              listView,
              new SwipeDismissListViewTouchListener.DismissCallbacks() {
                  @Override
                  public boolean canDismiss(int position) {
                      return true;
                  }

                  @Override
                  public void onDismiss(ListView listView, int position) {
                    Utils.addBlocked(mNearbyDeviceAdapter.getItem(position));
                    Utils.saveBlocked(getActivity());
                    if (mMissedEmptyGroupIdQueue) {
                      mMissedEmptyGroupIdQueue = false;
                      emptyGroupIdQueue();
                    }
                  }
              });
    listView.setOnTouchListener(mTouchListener);

    // Setting this scroll listener is required to ensure that during ListView scrolling,
    // we don't look for swipes.
    listView.setOnScrollListener(mTouchListener.makeScrollListener());
    Utils.restoreFavorites(getActivity());
    Utils.restoreBlocked(getActivity());
  }

  @Override
  public View onCreateView(LayoutInflater layoutInflater, ViewGroup container,
                           Bundle savedInstanceState) {
    mFirstTime = true;
    View rootView = layoutInflater.inflate(R.layout.fragment_nearby_beacons, container, false);
    initialize(rootView);
    return rootView;
  }

  @Override
  public void onResume() {
    super.onResume();
    getActivity().getActionBar().setTitle(R.string.title_nearby_beacons);
    getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
    if (mFirstTime && !PermissionCheck.getInstance().isCheckingPermissions()) {
      restartScan();
    }
    mFirstTime = false;
  }

  public void restartScan() {
    if (mDiscoveryServiceConnection != null) {
      mDiscoveryServiceConnection.connect(true);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    mDiscoveryServiceConnection.disconnect();
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(R.id.action_about).setVisible(true);
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    PwPair item = mNearbyDeviceAdapter.getItem(position);
    // If we are scanning or user clicked on folder
    if (mScanningAnimationDrawable.isRunning() || isFolderItem(item)) {
      // Don't respond to touch events
      return;
    }
    // Get the url for the given item
    PwsResult pwsResult = item.getPwsResult();
    if (Utils.isWifiDirectDevice(item.getUrlDevice())) {
      // Initiate WifiDirect Connection request to device
      mWifiDirectConnect.connect(item.getUrlDevice(), pwsResult.getTitle());
    } else if (Utils.isFatBeaconDevice(item.getUrlDevice())) {
      if (!mBluetoothSite.isRunning()) {
        mBluetoothSite.connect(pwsResult.getSiteUrl(), pwsResult.getTitle());
      }
    } else {
      Intent intent = Utils.createNavigateToUrlIntent(pwsResult);
      startActivity(intent);
    }
  }

  @Override
  public void onUrlDeviceDiscoveryUpdate() {
    for (PwPair pwPair : mPwCollection.getGroupedPwPairsSortedByRank(
        new Utils.PwPairRelevanceComparator())) {
      String groupId = Utils.getGroupId(pwPair.getPwsResult());
      Log.d(TAG, "groupid to add " + groupId);
      if (mNearbyDeviceAdapter.containsGroupId(groupId)) {
        mNearbyDeviceAdapter.updateItem(pwPair);
      } else if (!mGroupIdQueue.contains(groupId)
          && !Utils.isBlocked(pwPair)) {
        mGroupIdQueue.add(groupId);
      }
    }

    if(mGroupIdQueue.isEmpty() || !mSecondScanComplete) {
      return;
    }
    // Since this callback is given on a background thread and we want
    // to update the list adapter (which can only be done on the UI thread)
    // we have to interact with the adapter on the UI thread.
    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
       emptyGroupIdQueue();
      }
    });
  }

  private void stopScanningDisplay() {
    // Cancel the scan timeout callback if still active or else it may fire later.
    mHandler.removeCallbacks(mFirstScanTimeout);
    mHandler.removeCallbacks(mSecondScanTimeout);
    mHandler.removeCallbacks(mThirdScanTimeout);

    // Change the display appropriately
    setRefreshWidgetInvisible();
  }

  private void startScanningDisplay(long scanStartTime, boolean hasResults) {
    // Start the scanning animation only if we don't haven't already been scanning
    // for long enough
    Log.d(TAG, "startScanningDisplay " + scanStartTime + " " + hasResults);
    long elapsedMillis = new Date().getTime() - scanStartTime;
    if (elapsedMillis < FIRST_SCAN_TIME_MILLIS
        || (elapsedMillis < SECOND_SCAN_TIME_MILLIS && !hasResults)) {
      mNearbyDeviceAdapter.clear();
      mScanningAnimationDrawable.setColorFilter(null);
      mScanningAnimationTextView.setText(R.string.empty_nearby_beacons_list_text);
      mScanningAnimationDrawable.start();
    } else {
      setRefreshWidgetInvisible();
    }

    // Schedule the timeouts
    mSecondScanComplete = false;
    long firstDelay = Math.max(FIRST_SCAN_TIME_MILLIS - elapsedMillis, 0);
    long secondDelay = Math.max(SECOND_SCAN_TIME_MILLIS - elapsedMillis, 0);
    long thirdDelay = Math.max(THIRD_SCAN_TIME_MILLIS - elapsedMillis, 0);
    mHandler.postDelayed(mFirstScanTimeout, firstDelay);
    mHandler.postDelayed(mSecondScanTimeout, secondDelay);
    mHandler.postDelayed(mThirdScanTimeout, thirdDelay);
  }

  @Override
  public void onRefresh() {
    // Clear any stored url data
    mGroupIdQueue.clear();
    mNearbyDeviceAdapter.clear();

    // Reconnect to the service
    mDiscoveryServiceConnection.disconnect();
    mSwipeRefreshWidget.setRefreshing(true);
    mDiscoveryServiceConnection.connect(false);
  }

  private void emptyGroupIdQueue() {
    if (SwipeDismissListViewTouchListener.isLocked()) {
      mMissedEmptyGroupIdQueue = true;
      return;
    }

    List<PwPair> pwPairs = new ArrayList<>();

    for (String groupId : mGroupIdQueue) {
      Log.d(TAG, "groupid " + groupId);
      pwPairs.add(Utils.getTopRankedPwPairByGroupId(mPwCollection, groupId));
    }
    Collections.sort(pwPairs, new Utils.PwPairRelevanceComparator());
    for (PwPair pwPair : pwPairs) {
      mNearbyDeviceAdapter.addItem(pwPair);
    }
    mGroupIdQueue.clear();
    mNearbyDeviceAdapter.notifyDataSetChanged();
  }

  private static boolean isFolderItem(PwPair item) {
    return item.getUrlDevice() == null && item.getPwsResult().getSiteUrl() == null;
  }

  private void setRefreshWidgetInvisible() {
    mSwipeRefreshWidget.setRefreshing(false);
    mScanningAnimationDrawable.stop();
    mScanningAnimationTextView.setVisibility(View.INVISIBLE);
  }

  // Adapter for holding beacons found through scanning.
  private class NearbyBeaconsAdapter extends BaseAdapter {
    private List<PwPair> mPwPairs;
    private int mNumberOfHideableResults;

    NearbyBeaconsAdapter() {
      mPwPairs = new ArrayList<>();
      mNumberOfHideableResults = 0;
    }

    public void addItem(PwPair pwPair) {
      // If isResolvableDevice place in the folder at the bottom
      // of the list (making the folder if it didn't already exist)
      // Otherwise place in the bottom of the non-folder list
      if (Utils.isResolvableDevice(pwPair.getUrlDevice())) {
        mPwPairs.add(mPwPairs.size() - mNumberOfHideableResults, pwPair);
        return;
      }
      if (mNumberOfHideableResults == 0) {
        mPwPairs.add(new PwPair(null, new PwsResult(null, null)));
        mNumberOfHideableResults++;
      }
      mNumberOfHideableResults++;
      mPwPairs.add(pwPair);
    }

    public void updateItem(PwPair pwPair) {
      String groupId = Utils.getGroupId(pwPair.getPwsResult());
      for (int i = 0; i < mPwPairs.size(); ++i) {
        if (isFolderItem(mPwPairs.get(i))) {
          continue;
        }
        if (Utils.getGroupId(mPwPairs.get(i).getPwsResult()).equals(groupId)) {
          mPwPairs.set(i, pwPair);
          return;
        }
      }
      throw new RuntimeException("Cannot find PwPair with group " + groupId);
    }

    public boolean containsGroupId(String groupId) {
      for (PwPair pwPair : mPwPairs) {
        if (isFolderItem(pwPair)) {
          continue;
        }
        if (Utils.getGroupId(pwPair.getPwsResult()).equals(groupId)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public int getCount() {
      return mPwPairs.size();
    }

    @Override
    public PwPair getItem(int i) {
      return mPwPairs.get(i);
    }

    @Override
    public long getItemId(int i) {
      return i;
    }

    private void setText(View view, int textViewId, String text) {
      ((TextView) view.findViewById(textViewId)).setText(text);
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
      // Display the pwsResult.
      PwPair pwPair = getItem(i);
      PwsResult pwsResult = pwPair.getPwsResult();
      if (isFolderItem(pwPair)) {
        view = getActivity().getLayoutInflater().inflate(R.layout.folder_item_nearby_beacon,
            viewGroup, false);
        WifiManager wifiManager = (WifiManager) getActivity().
            getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID().trim();
        if (ssid.charAt(0) == '"' && ssid.charAt(ssid.length() - 1) == '"') {
          setText(view, R.id.title, ssid.substring(1, ssid.length() - 1));
        } else {
          setText(view, R.id.title, "Wireless Network");
        }
        return view;
      }
      view = getActivity().getLayoutInflater().inflate(R.layout.list_item_nearby_beacon,
          viewGroup, false);
      setText(view, R.id.title, pwsResult.getTitle());
      if (Utils.isFatBeaconDevice(pwPair.getUrlDevice())) {
        setText(view, R.id.url, getString(R.string.FatBeacon_URL) + pwsResult.getSiteUrl());
      } else {
        setText(view, R.id.url, pwsResult.getSiteUrl());
      }
      if (Utils.isResolvableDevice(pwPair.getUrlDevice())) {
        ((ImageView) view.findViewById(R.id.icon)).setImageBitmap(
            Utils.getBitmapIcon(mPwCollection, pwsResult));
      } else {
        ((ImageView) view.findViewById(R.id.icon))
            .setImageResource(R.drawable.unresolved_result_icon);
      }
      setText(view, R.id.description, pwsResult.getDescription());
      final String siteUrl = pwsResult.getSiteUrl();

      if (Utils.isFavorite(siteUrl)) {
        ((Button) view.findViewById(R.id.star)).setBackgroundResource(
            R.drawable.ic_star_black_24dp);
        ((Button) view.findViewById(R.id.star)).setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            Utils.toggleFavorite(siteUrl);
            Utils.saveFavorites(getActivity());
            ((Button) v).setBackgroundResource(R.drawable.ic_star_border_black_24dp);
            notifyDataSetChanged();
          }
        });
      } else {
        ((Button) view.findViewById(R.id.star)).setBackgroundResource(
            R.drawable.ic_star_border_black_24dp);
        ((Button) view.findViewById(R.id.star)).setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            Utils.toggleFavorite(siteUrl);
            Utils.saveFavorites(getActivity());
            ((Button) v).setBackgroundResource(R.drawable.ic_star_black_24dp);
            notifyDataSetChanged();
          }
        });
      }

      if (Utils.isDebugViewEnabled(getActivity())) {
        // If we should show the ranging data
        updateDebugView(pwPair, view);
        view.findViewById(R.id.ranging_debug_container).setVisibility(View.VISIBLE);
        view.findViewById(R.id.metadata_debug_container).setVisibility(View.VISIBLE);
      } else {
        view.findViewById(R.id.ranging_debug_container).setVisibility(View.GONE);
        view.findViewById(R.id.metadata_debug_container).setVisibility(View.GONE);
      }
      return view;
    }

    private void updateDebugView(PwPair pwPair, View view) {
      // Ranging debug line
      UrlDevice urlDevice = pwPair.getUrlDevice();
      if (Utils.isBleUrlDevice(urlDevice)) {
        setText(view, R.id.ranging_debug_tx_power,
            getString(R.string.ranging_debug_tx_power_prefix) + Utils.getTxPower(urlDevice));
        setText(view, R.id.ranging_debug_rssi,
            getString(R.string.ranging_debug_rssi_prefix) + Utils.getSmoothedRssi(urlDevice));
        setText(view, R.id.ranging_debug_distance,
            getString(R.string.ranging_debug_distance_prefix)
            + new DecimalFormat("##.##").format(Utils.getDistance(urlDevice)));
        setText(view, R.id.ranging_debug_region,
            getString(R.string.ranging_debug_region_prefix) + Utils.getRegionString(urlDevice));
      } else {
        setText(view, R.id.ranging_debug_tx_power, "");
        setText(view, R.id.ranging_debug_rssi, "");
        setText(view, R.id.ranging_debug_distance, "");
        setText(view, R.id.ranging_debug_region, "");
      }

      // Metadata debug line
      setText(view, R.id.metadata_debug_scan_time,
          getString(R.string.metadata_debug_scan_time_prefix)
          + new DecimalFormat("##.##s").format(Utils.getScanTimeMillis(urlDevice) / 1000.0));

      PwsResult pwsResult = pwPair.getPwsResult();
      setText(view, R.id.metadata_debug_rank,
          getString(R.string.metadata_debug_rank_prefix)
          + new DecimalFormat("##.##").format(0));  // We currently do not use rank.
      if (Utils.isResolvableDevice(urlDevice)) {
        setText(view, R.id.metadata_debug_pws_trip_time,
            getString(R.string.metadata_debug_pws_trip_time_prefix)
                + new DecimalFormat("##.##s")
                .format(Utils.getPwsTripTimeMillis(pwsResult) / 1000.0));
      }
      setText(view, R.id.metadata_debug_groupid,
          getString(R.string.metadata_debug_groupid_prefix) + Utils.getGroupId(pwsResult));
    }

    public void clear() {
      mPwPairs.clear();
      mNumberOfHideableResults = 0;
      notifyDataSetChanged();
    }
  }

}
