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

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.uribeacon.beacon.ConfigUriBeacon;
import org.uribeacon.config.ProtocolV1;
import org.uribeacon.config.ProtocolV2;
import org.uribeacon.config.UriBeaconConfig;
import org.uribeacon.scan.compat.ScanRecord;
import org.uribeacon.scan.compat.ScanResult;
import org.uribeacon.scan.util.RegionResolver;

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This fragment is the ui that the user sees when
 * they have entered the app's beacon configuration mode.
 * This ui is where the user can view the current configuration
 * of a beacon (including it's address and url)
 * and also allows the user to enter a new url for that beacon.
 */

public class BeaconConfigFragment extends Fragment implements TextView.OnEditorActionListener {

  private static final String TAG = "BeaconConfigFragment";
  private static final byte TX_POWER_DEFAULT = -22;
  private static final long SCAN_TIME_MILLIS = TimeUnit.SECONDS.toMillis(15);
  private static final ParcelUuid[] mScanFilterUuids =
      new ParcelUuid[]{ProtocolV2.CONFIG_SERVICE_UUID, ProtocolV1.CONFIG_SERVICE_UUID};
  private final BluetoothAdapter.LeScanCallback mLeScanCallback = new LeScanCallback();
  private BluetoothDevice mNearestDevice;
  private RegionResolver mRegionResolver;
  private EditText mEditCardUrl;
  private TextView mScanningStatus;
  private TextView mEditCardAddress;
  private LinearLayout mEditCard;
  private AnimationDrawable mScanningAnimation;
  private UriBeaconConfig mUriBeaconConfig;
  private boolean mIsScanRunning;
  private BluetoothAdapter mBluetoothAdapter;
  private Handler mHandler;
  private UrlShortener.LengthenShortUrlTask mLengthenShortUrlTask;

  // Run when the SCAN_TIME_MILLIS has elapsed.
  private Runnable mScanTimeout = new Runnable() {
    @Override
    public void run() {
      scanLeDevice(false);
      mScanningAnimation.stop();
      mScanningStatus.setCompoundDrawables(null, null, null, null);
      mScanningStatus.setText(R.string.config_no_beacons_found);
    }
  };

  public BeaconConfigFragment() {
  }

  public static BeaconConfigFragment newInstance() {
    return new BeaconConfigFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mRegionResolver = new RegionResolver();
    mHandler = new Handler();
    initializeBluetooth();
    setHasOptionsMenu(true);
    getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
  }

  private void initializeBluetooth() {
    // Initializes a Bluetooth adapter. For API version 18 and above,
    // get a reference to BluetoothAdapter through BluetoothManager.
    final BluetoothManager bluetoothManager =
        (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = bluetoothManager.getAdapter();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_beacon_config, container, false);

    mEditCard = (LinearLayout) view.findViewById(R.id.edit_card);

    // Get handles to Status and Address views
    mEditCardAddress = (TextView) view.findViewById(R.id.edit_card_address);

    // Setup the URL Edit Text handler
    mEditCardUrl = (EditText) view.findViewById(R.id.edit_card_url);
    mEditCardUrl.setOnEditorActionListener(this);

    // Setup the animation
    mScanningStatus = (TextView) view.findViewById(R.id.textView_scanningStatus);
    mScanningAnimation = (AnimationDrawable) ResourcesCompat.getDrawable(
        getResources(), R.drawable.scanning_animation, null);
    mScanningStatus.setCompoundDrawablesWithIntrinsicBounds(null, mScanningAnimation, null, null);

    Button button = (Button) view.findViewById(R.id.edit_card_save);
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        saveEditCardUrlToBeacon();
      }
    });

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    getActivity().getActionBar().setTitle(R.string.title_edit_urls);
    mEditCardAddress.setText("");
    mEditCardUrl.setText("");
    mEditCard.setVisibility(View.INVISIBLE);
    mScanningStatus.setText(R.string.config_searching_for_beacons_text);
    mScanningAnimation.start();
    scanLeDevice(true);
  }

  @Override
  public void onPause() {
    super.onPause();
    mScanningAnimation.stop();
    scanLeDevice(false);
    PwsClient.getInstance(getActivity()).cancelAllRequests(TAG);
    if (mLengthenShortUrlTask != null) {
      mLengthenShortUrlTask.cancel(true);
    }
    if (mUriBeaconConfig != null) {
      mUriBeaconConfig.closeUriBeacon();
    }
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(R.id.action_config).setVisible(false);
    menu.findItem(R.id.action_about).setVisible(false);
  }

  /**
   * This is the class that listens for specific text entry events
   * (e.g. the DONE key)
   * on the edit text field that the user uses
   * to enter a new url for the configurable beacon
   */
  @Override
  public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
    // If the keyboard "DONE" button was pressed
    if (actionId == EditorInfo.IME_ACTION_DONE) {
      // Hide the software keyboard
      hideSoftKeyboard();
      saveEditCardUrlToBeacon();
      return true;
    }
    return false;
  }

  public void onBeaconConfigReadUrlComplete(ConfigUriBeacon uriBeacon, int status) {
    if (status != BluetoothGatt.GATT_SUCCESS) {
      Log.e(TAG, "onUriBeaconRead - error " + status);
    } else {
      // Create the callback object to set the url
      UrlShortener.ModifiedUrlCallback urlSetter = new UrlShortener.ModifiedUrlCallback() {
        public void setUrl(String urlToSet) {
          // Update the url edit text field with the given url
          mEditCardUrl.setText(urlToSet);
          // Show the beacon configuration card
          showConfigurableBeaconCard();
        }
        @Override
        public void onNewUrl(String newUrl){
          setUrl(newUrl);
        }
        @Override
        public void onError(String oldUrl) {
          setUrl(oldUrl);
        }
      };

      String url = "";
      if (uriBeacon != null) {
        url = uriBeacon.getUriString();
        Log.d(TAG, "onReadUrlComplete" + "  url:  " + url);
        if (UrlShortener.isShortUrl(url)) {
          mLengthenShortUrlTask = new UrlShortener.LengthenShortUrlTask(urlSetter);
          mLengthenShortUrlTask.execute(url);
        } else {
          urlSetter.onNewUrl(url);
        }
      } else {
        Toast.makeText(getActivity(), R.string.config_url_read_error, Toast.LENGTH_SHORT).show();
        urlSetter.onNewUrl(url);
      }
    }
  }

  public void onBeaconConfigWriteUrlComplete(final int status) {
    // Detach this fragment from its activity
    getFragmentManager().popBackStack();
    // Show a toast to the user to let them know if the Url was written or error occurred.
    int msgId = (status == BluetoothGatt.GATT_SUCCESS)
        ? R.string.config_url_saved : R.string.config_url_error;
    Toast.makeText(getActivity(), msgId, Toast.LENGTH_SHORT).show();
  }

  @SuppressWarnings("deprecation")
  private void scanLeDevice(final boolean enable) {
    if (mIsScanRunning != enable) {
      mIsScanRunning = enable;
      // If we should start scanning
      if (enable) {
        // Stops scanning after the predefined scan time has elapsed.
        mHandler.postDelayed(mScanTimeout, SCAN_TIME_MILLIS);
        // Start the scan
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        // If we should stop scanning
      } else {
        // Cancel the scan timeout callback if still active or else it may fire later.
        mHandler.removeCallbacks(mScanTimeout);
        // Stop the scan
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
      }
    }
  }

  private ParcelUuid leScanMatches(ScanRecord scanRecord) {
    List services = scanRecord.getServiceUuids();
    if (services != null) {
      for (ParcelUuid uuid : mScanFilterUuids) {
        if (services.contains(uuid)) {
          return uuid;
        }
      }
    }
    return null;
  }

  private void handleFoundDevice(final ScanResult scanResult, ParcelUuid filteredUuid) {
    final String address = scanResult.getDevice().getAddress();
    int rxPower = scanResult.getRssi();
    int txPower = scanResult.getScanRecord().getTxPowerLevel();
    mRegionResolver.onUpdate(address, rxPower, txPower);
    final String nearestAddress = mRegionResolver.getNearestAddress();
    // TODO(?): re-implement nearest address methodology once ranging bug is fixed
    // When the current sighting comes from the nearest device...
    //if (address.equals(nearestAddress)) {
    if (true) {
      // Stopping the scan in this thread is important for responsiveness
      scanLeDevice(false);
      mUriBeaconConfig = new UriBeaconConfig(getActivity(), new UriBeaconConfigCallback(),
                                             filteredUuid);
      if (mUriBeaconConfig != null) {
        mNearestDevice = scanResult.getDevice();
        mUriBeaconConfig.connectUriBeacon(mNearestDevice);
      }
    } else {
      mNearestDevice = null;
    }
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (mNearestDevice != null) {
          // Remove animation
          mScanningStatus.setCompoundDrawables(null, null, null, null);
          mScanningStatus.setText(R.string.config_found_near_beacon);
          mEditCardAddress.setText(nearestAddress);
        } else {
          mScanningStatus.setText(R.string.config_found_far_beacon);
        }
      }
    });
  }

  /**
   * Hide the software keyboard.
   */
  private void hideSoftKeyboard() {
    InputMethodManager imm =
        (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(mEditCardUrl.getWindowToken(), 0);
  }

  /**
   * Show the card that displays the address and url of the currently-being-configured beacon.
   */
  private void showConfigurableBeaconCard() {
    mEditCard.setVisibility(View.VISIBLE);
    Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in_and_slide_up);
    mEditCard.startAnimation(animation);
  }

  /**
   * Write a beacon url to the beacon.
   */
  private void setUriBeaconUrl(String url) {
    try {
      // Note: setting txPower here only really updates txPower for v1 beacons
      ConfigUriBeacon configUriBeacon = new ConfigUriBeacon.Builder()
          .txPowerLevel(TX_POWER_DEFAULT)
          .uriString(url)
          .build();
      mUriBeaconConfig.writeUriBeacon(configUriBeacon);
    } catch (URISyntaxException e) {
      e.printStackTrace();
      Toast.makeText(getActivity(), R.string.config_url_error, Toast.LENGTH_SHORT).show();
    }
  }

  /**
   * This is called when the user taps the write-to-beacon button.
   */
  private void saveEditCardUrlToBeacon() {
    // Update the status text
    mScanningStatus.setText(R.string.config_writing_to_beacon_text);
    // Remove the focus from the url edit text field
    mEditCard.clearFocus();
    // Get the current text in the url edit text field.
    String url = mEditCardUrl.getText().toString();
    // Ensure an http prefix exists in the url
    if (!URLUtil.isNetworkUrl(url)) {
      url = "http://" + url;
    }
    // Create the callback object to set the url
    PwsClient.ShortenUrlCallback urlSetter = new PwsClient.ShortenUrlCallback() {
      @Override
      public void onUrlShortened(String newUrl) {
        setUriBeaconUrl(newUrl);
      }
      @Override
      public void onError(String oldUrl) {
        Toast.makeText(getActivity(), R.string.url_shortening_error, Toast.LENGTH_SHORT).show();
      }
    };
    // Shorten the url if necessary
    if (ConfigUriBeacon.uriLength(url) > ConfigUriBeacon.MAX_URI_LENGTH) {
      PwsClient.getInstance(getActivity()).shortenUrl(url, urlSetter, TAG);
    } else {
      setUriBeaconUrl(url);
    }
  }

  /**
   * Callback for LE scan results.
   */
  private class LeScanCallback implements BluetoothAdapter.LeScanCallback {
    @Override
    public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanBytes) {
      ScanRecord scanRecord = ScanRecord.parseFromBytes(scanBytes);
      ParcelUuid filteredUuid = leScanMatches(scanRecord);
      if (filteredUuid != null) {
        final ScanResult scanResult = new ScanResult(device, scanRecord, rssi,
                                                     SystemClock.elapsedRealtimeNanos());
        handleFoundDevice(scanResult, filteredUuid);
      }
    }
  }

  class UriBeaconConfigCallback implements UriBeaconConfig.UriBeaconCallback {

    @Override
    public void onUriBeaconRead(ConfigUriBeacon configUriBeacon, int status) {
      onBeaconConfigReadUrlComplete(configUriBeacon, status);
    }

    @Override
    public void onUriBeaconWrite(int status) {
      onBeaconConfigWriteUrlComplete(status);
    }
  }

}



