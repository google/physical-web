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

package physical_web.org.physicalweb;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.uribeacon.scan.compat.BluetoothLeScannerCompat;
import org.uribeacon.scan.compat.BluetoothLeScannerCompatProvider;
import org.uribeacon.scan.compat.ScanCallback;
import org.uribeacon.scan.compat.ScanFilter;
import org.uribeacon.scan.compat.ScanResult;
import org.uribeacon.scan.compat.ScanSettings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This fragment is the ui that the user sees when
 * they have entered the app's beacon configuration mode.
 * This ui is where the user can view the current configuration
 * of a beacon (including it's address and url)
 * and also allows the user to enter a new url for that beacon.
 */

public class BeaconConfigFragment extends Fragment implements BeaconConfigHelper.BeaconConfigCallback{

  private static String TAG = "BeaconConfigFragment";
  private boolean mShowingConfigurableCard = false;
  private BluetoothDevice mFoundConfigurableBeaconBluetoothDevice;
  private Timer mCheckForFoundConfigurableDevicesTimer;
  private static final int CHECK_FOR_FOUND_CONFIGURABLE_DEVICES_PERIOD = 2000;
  private HashMap<String, Device> mDeviceAddressToDeviceMap;
  public static final ParcelUuid CHANGE_URL_SERVICE_UUID = ParcelUuid.fromString("B35D7DA6-EED4-4D59-8F89-F6573EDEA967");
  private EditText mConfigurableBeaconUrlEditText;
  private TextView mStatusTextView;
  private TextView mConfigurableBeaconAddressTextView;
  private LinearLayout mConfigurableBeaconLinearLayout;
  private ProgressBar msearchingForBeaconsProgressBar;

  public static BeaconConfigFragment newInstance() {
    return new BeaconConfigFragment();
  }

  public BeaconConfigFragment() {
  }

  private void initialize() {
    mFoundConfigurableBeaconBluetoothDevice = null;
    mShowingConfigurableCard = false;
    mDeviceAddressToDeviceMap = new HashMap<>();
    getActivity().getActionBar().setTitle(getString(R.string.title_edit_urls));
    inititalizeSearchingForBeaconsProgressBar();
    initializeTextViews();
    initializeConfigurableBeaconCard();
    startSearchingForDevices();
  }

  private void inititalizeSearchingForBeaconsProgressBar() {
    msearchingForBeaconsProgressBar = (ProgressBar) getView().findViewById(R.id.progressBar_searchingForBeacons);
    showSearchingForBeaconsProgressBar();
  }

  /**
   * Setup the card that will display
   * the information about the to-be-configured beacon
   */
  private void initializeConfigurableBeaconCard() {
    mConfigurableBeaconLinearLayout = (LinearLayout) getView().findViewById(R.id.linearLayout_configurableBeaconCard);
    mConfigurableBeaconAddressTextView.setText("");
    mConfigurableBeaconUrlEditText.setText("");
    Button button_writeToBeacon = (Button) getView().findViewById(R.id.button_writeToBeacon);
    button_writeToBeacon.setOnClickListener(writeToBeaconButtonOnClickListener);
    hideConfigurableBeaconCard();
  }

  private void initializeTextViews() {
    mStatusTextView = (TextView) getView().findViewById(R.id.textView_status);
    mStatusTextView.setText(getString(R.string.config_searching_for_beacons_text));
    mConfigurableBeaconAddressTextView = (TextView) getView().findViewById(R.id.textView_configurableBeaconAddress);
    mConfigurableBeaconUrlEditText = (EditText) getView().findViewById(R.id.editText_configurableBeaconUrl);
    mConfigurableBeaconUrlEditText.setOnEditorActionListener(onEditorActionListener_configurableBeaconUrlEditText);
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
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_beacon_config, container, false);
    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    initialize();
  }

  @Override
  public void onPause() {
    super.onPause();
    stopSearchingForDevices();
  }

  @Override
  public void onDetach() {
    super.onDestroy();
    BeaconConfigHelper.shutDownConfigGatt();
    getActivity().getActionBar().setTitle(getString(R.string.title_nearby_beacons));
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(R.id.action_config).setVisible(false);
    menu.findItem(R.id.action_about).setVisible(false);
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
   * This is the class that listens
   * for when the user taps the write-to-beacon button.
   */
  private View.OnClickListener writeToBeaconButtonOnClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View view) {
      // Update the status text
      mStatusTextView.setText(getString(R.string.config_writing_to_beacon_text));
      // Remove the focus from the url edit text field
      mConfigurableBeaconLinearLayout.clearFocus();
      // Get the current text in the url edit text field.
      String url = mConfigurableBeaconUrlEditText.getText().toString();
      // Write the url to the device
      BeaconConfigHelper.writeBeaconUrl(getActivity(), BeaconConfigFragment.this, mFoundConfigurableBeaconBluetoothDevice, url);
    }
  };

  /**
   * This is the class that listens for specific text entry events
   * (e.g. the DONE key)
   * on the edit text field that the user uses
   * to enter a new url for the configurable beacon
   */
  private TextView.OnEditorActionListener onEditorActionListener_configurableBeaconUrlEditText = new TextView.OnEditorActionListener() {
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
      // If the keyboard "DONE" button was pressed
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        onEditorAction_nearestConfigurableBeaconUrlEditTextDoneKeyPressed();
        return true;
      }
      return false;
    }
  };

  /**
   * Called when the user presses the keyboard "DONE" key
   *
   * @throws IOException
   */
  private void onEditorAction_nearestConfigurableBeaconUrlEditTextDoneKeyPressed() {
    // Hide the software keyboard
    hideSoftKeyboard();
    // Get the currently entered url in the url edit text field
    String url = mConfigurableBeaconUrlEditText.getText().toString();
    // Write the url to the device
    BeaconConfigHelper.writeBeaconUrl(getActivity(), this, mFoundConfigurableBeaconBluetoothDevice, url);
  }

  @Override
  public void onBeaconConfigReadUrlComplete(final String url) {
    Log.d(TAG, "onReadUrlComplete" + "  url:  " + url);
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        // Update the url edit text field with the given url
        mConfigurableBeaconUrlEditText.setText(url);
        // Show the beacon configuration card
        if (!mShowingConfigurableCard) {
          showConfigurableBeaconCard();
        }
      }
    });
  }

  @Override
  public void onBeaconConfigWriteUrlComplete() {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        // Detach this fragment from its activity
        getFragmentManager().popBackStack();
        // Show a toast to the user to let them know the url was written to the beacon
        Toast.makeText(getActivity(), getString(R.string.config_url_saved_text), Toast.LENGTH_SHORT).show();
      }
    });
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
        .setServiceUuid(CHANGE_URL_SERVICE_UUID);
    filters.add(builder.build());

    boolean started = getLeScanner().startScan(filters, settings, mScanCallback);
    Log.v(TAG, started ? "... scan started" : "... scan NOT started");

    //every so often look to see if we found any configurable devices
    startCheckForConfigurableDevicesTimer();
  }

  private void stopSearchingForDevices() {
    Log.v(TAG, "stopSearchingForDevices");
    getLeScanner().stopScan(mScanCallback);
    stopCheckForConfigurableDevicesTimer();
  }

  private void startCheckForConfigurableDevicesTimer() {
    mCheckForFoundConfigurableDevicesTimer = new Timer();
    CheckForFoundConfigurableDevicesTask checkForFoundConfigurableDevicesTask = new CheckForFoundConfigurableDevicesTask();
    mCheckForFoundConfigurableDevicesTimer.scheduleAtFixedRate(checkForFoundConfigurableDevicesTask, 0, CHECK_FOR_FOUND_CONFIGURABLE_DEVICES_PERIOD);
  }

  private void stopCheckForConfigurableDevicesTimer() {
    if (mCheckForFoundConfigurableDevicesTimer != null) {
      mCheckForFoundConfigurableDevicesTimer.cancel();
      mCheckForFoundConfigurableDevicesTimer = null;
    }
  }

  private void handleFoundDevice(ScanResult scanResult) {
    Log.i(TAG, String.format("onLeScan: %s, RSSI: %d", scanResult.getDevice().getAddress(), scanResult.getRssi()));

    // Try to get a stored nearby device that matches this device
    Device device = mDeviceAddressToDeviceMap.get(scanResult.getDevice().getAddress());

    // If no match was found (i.e. if this a newly discovered device)
    if (device == null) {
      device = new Device(null, scanResult.getDevice(), scanResult.getRssi());
      addConfigurableDevice(device);
    }
    // If a match was found
    else {
      updateConfigurableDevice(device, scanResult.getRssi());
    }
  }

  private void handleLostDevice(ScanResult scanResult) {
    String address = scanResult.getDevice().getAddress();
    mDeviceAddressToDeviceMap.remove(address);
  }

  private void addConfigurableDevice(Device device) {
    mDeviceAddressToDeviceMap.put(device.getBluetoothDevice().getAddress(), device);
  }

  private void updateConfigurableDevice(Device device, int rssi) {
    device.updateRssiHistory(rssi);
  }

  private class CheckForFoundConfigurableDevicesTask extends TimerTask {
    @Override
    public void run() {
      Device device = findNearestConfigurableDevice();
      if (device != null) {
        handleFoundNearestConfigurableDevice(device);
      }
    }
  }

  private Device findNearestConfigurableDevice() {
    Device nearestConfigurableDevice = null;
    if (mDeviceAddressToDeviceMap.size() > 0) {
      ArrayList<Device> sortedDevices = new ArrayList<>(mDeviceAddressToDeviceMap.values());
      Collections.sort(sortedDevices, mRssiComparator);
      for (Device device : sortedDevices) {
        Log.d(TAG, "device: " + device.getBluetoothDevice().getAddress() + "  rssi:  " + device.calculateAverageRssi());
      }
      nearestConfigurableDevice = sortedDevices.get(0);
    }
    return nearestConfigurableDevice;
  }

  private Comparator<Device> mRssiComparator = new Comparator<Device>() {
    @Override
    public int compare(Device lhs, Device rhs) {
      return rhs.calculateAverageRssi() - lhs.calculateAverageRssi();
    }
  };

  private void handleFoundNearestConfigurableDevice(final Device device) {
    mFoundConfigurableBeaconBluetoothDevice = device.getBluetoothDevice();
    stopSearchingForDevices();
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        hideSearchingForBeaconsProgressBar();
        mStatusTextView.setText(getString(R.string.config_found_beacon_text));
        mConfigurableBeaconAddressTextView.setText(device.getBluetoothDevice().getAddress());
      }
    });
    BeaconConfigHelper.readBeaconUrl(getActivity(), this, mFoundConfigurableBeaconBluetoothDevice);
  }

  /**
   * Hide the software keyboard
   */
  private void hideSoftKeyboard() {
    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(mConfigurableBeaconUrlEditText.getWindowToken(), 0);
  }

  /**
   * Hide the card that displays the address and url
   * of the currently-being-configured beacon
   */
  private void hideConfigurableBeaconCard() {
    mConfigurableBeaconLinearLayout.setVisibility(View.INVISIBLE);
  }

  /**
   * Show the card that displays the address and url
   * of the currently-being-configured beacon
   */
  private void showConfigurableBeaconCard() {
    mShowingConfigurableCard = true;
    mConfigurableBeaconLinearLayout.setVisibility(View.VISIBLE);
    Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in_and_slide_up);
    mConfigurableBeaconLinearLayout.startAnimation(animation);
  }

  /**
   * Show the progress bar that indicates
   * a search for nearby configurable beacons
   * is being performed.
   */
  private void showSearchingForBeaconsProgressBar() {
    msearchingForBeaconsProgressBar.setVisibility(View.VISIBLE);
  }

  /**
   * Hide the progress bar that indicates
   * a search for nearby configurable beacons
   * is being performed.
   */
  private void hideSearchingForBeaconsProgressBar() {
    msearchingForBeaconsProgressBar.setVisibility(View.INVISIBLE);
  }

}



