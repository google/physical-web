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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.ParcelUuid;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.uribeacon.beacon.UriBeacon;
import org.uribeacon.scan.compat.BluetoothLeScannerCompat;
import org.uribeacon.scan.compat.BluetoothLeScannerCompatProvider;
import org.uribeacon.scan.compat.ScanCallback;
import org.uribeacon.scan.compat.ScanFilter;
import org.uribeacon.scan.compat.ScanResult;
import org.uribeacon.scan.compat.ScanSettings;
import org.uribeacon.scan.util.RegionResolver;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * This fragment is the ui that the user sees when
 * they have entered the app's beacon configuration mode.
 * This ui is where the user can view the current configuration
 * of a beacon (including it's address and url)
 * and also allows the user to enter a new url for that beacon.
 */

public class BeaconConfigFragment extends Fragment implements BeaconConfigHelper.BeaconConfigCallback,
    TextView.OnEditorActionListener {

  private static final String TAG = "BeaconConfigFragment";
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
  // TODO: default value for TxPower should be in another module
  private static final int TX_POWER_DEFAULT = -63;
  private static final ParcelUuid CHANGE_URL_SERVICE_UUID = ParcelUuid.fromString("B35D7DA6-EED4-4D59-8F89-F6573EDEA967");
  private BluetoothDevice mNearestDevice;
  private RegionResolver mRegionResolver;
  private EditText mEditCardUrl;
  private TextView mScanningStatus;
  private TextView mEditCardAddress;
  private LinearLayout mEditCard;
  private AnimationDrawable mScanningAnimation;
  private BeaconConfigHelper mBeaconConfig;

  public BeaconConfigFragment() {
  }

  public static BeaconConfigFragment newInstance() {
    return new BeaconConfigFragment();
  }

  private BluetoothLeScannerCompat getLeScanner() {
    return BluetoothLeScannerCompatProvider.getBluetoothLeScannerCompat(getActivity());
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mRegionResolver = new RegionResolver();
    mBeaconConfig = new BeaconConfigHelper(getActivity(), this);
    setHasOptionsMenu(true);
    getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
    mScanningAnimation = (AnimationDrawable) getResources().getDrawable(R.drawable.scanning_animation);
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
    mEditCardAddress.setText("");
    mEditCardUrl.setText("");
    mEditCard.setVisibility(View.INVISIBLE);
    mScanningStatus.setText(R.string.config_searching_for_beacons_text);
    mScanningAnimation.start();
    startSearchingForDevices();
  }

  @Override
  public void onPause() {
    super.onPause();
    mScanningAnimation.stop();
    stopSearchingForDevices();
    mBeaconConfig.closeUriBeacon();
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(R.id.action_config).setVisible(false);
    menu.findItem(R.id.action_about).setVisible(false);
    menu.findItem(R.id.action_demo).setVisible(false);
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


  @Override
  public void onBeaconConfigReadUrlComplete(byte[] scanRecord, int status) {
    if (status != BluetoothGatt.GATT_SUCCESS) {
      Log.e(TAG, "onUriBeaconRead - error " + status);
    } else {
      UriBeacon uriBeacon = UriBeacon.parseFromBytes(scanRecord);
      final String url = (uriBeacon != null) ? uriBeacon.getUriString() : "";
      Log.d(TAG, "onReadUrlComplete" + "  url:  " + url);
      getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          // Update the url edit text field with the given url
          mEditCardUrl.setText(url);
          // Show the beacon configuration card
          showConfigurableBeaconCard();
        }
      });

    }
  }

  @Override
  public void onBeaconConfigWriteUrlComplete(final int status) {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        // Detach this fragment from its activity
        getFragmentManager().popBackStack();
        // Show a toast to the user to let them know if the Url was written or error occurred.
        int msgId = (status == BluetoothGatt.GATT_SUCCESS)
            ? R.string.config_url_saved : R.string.config_url_error;
        Toast.makeText(getActivity(), msgId, Toast.LENGTH_SHORT).show();
      }
    });
  }

  private void startSearchingForDevices() {
    Log.v(TAG, "startSearchingForDevices");

    ScanSettings settings = new ScanSettings.Builder()
        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build();

    List<ScanFilter> filters = new ArrayList<>();

    ScanFilter filter = new ScanFilter.Builder()
        .setServiceUuid(CHANGE_URL_SERVICE_UUID)
        .build();

    filters.add(filter);

    boolean started = getLeScanner().startScan(filters, settings, mScanCallback);
    Log.v(TAG, started ? "... scan started" : "... scan NOT started");
  }

  private void stopSearchingForDevices() {
    Log.v(TAG, "stopSearchingForDevices");
    getLeScanner().stopScan(mScanCallback);
  }

  private void handleFoundDevice(final ScanResult scanResult) {
    final String address = scanResult.getDevice().getAddress();
    int rxPower = scanResult.getRssi();
    Log.i(TAG, String.format("handleFoundDevice: %s, RSSI: %d", address, rxPower));
    mRegionResolver.onUpdate(address, rxPower, TX_POWER_DEFAULT);
    final String nearestAddress = mRegionResolver.getNearestAddress();
    // When the current sighting comes from the nearest device...
    if (address.equals(nearestAddress)) {
      // Stopping the scan in this thread is important for responsiveness
      stopSearchingForDevices();
      mNearestDevice = scanResult.getDevice();
      mBeaconConfig.connectUriBeacon(mNearestDevice);
    } else {
      mNearestDevice = null;
    }
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (mNearestDevice != null) {
          //Remove animation
          mScanningStatus.setCompoundDrawables(null, null, null, null);

          mScanningStatus.setText(R.string.config_found_near_beacon);
          mEditCardAddress.setText(nearestAddress);
        } else {
          mScanningStatus.setText(R.string.config_found_far_beacon);
        }
      }
    });
  }


  private void handleLostDevice(ScanResult scanResult) {
    String address = scanResult.getDevice().getAddress();
    Log.i(TAG, String.format("handleLostDevice: %s", address));
    mRegionResolver.onLost(address);
  }

  /**
   * Hide the software keyboard
   */
  private void hideSoftKeyboard() {
    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(mEditCardUrl.getWindowToken(), 0);
  }

  /**
   * Show the card that displays the address and url
   * of the currently-being-configured beacon
   */
  private void showConfigurableBeaconCard() {
    mEditCard.setVisibility(View.VISIBLE);
    Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in_and_slide_up);
    mEditCard.startAnimation(animation);
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
    // Write the url to the device
    try {
      byte[] scanResult = BeaconHelper.createAdvertisingPacket(url);
      mBeaconConfig.writeUriBeacon(scanResult);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }
}



