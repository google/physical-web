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

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import java.util.List;

/**
 * The main entry point for the app.
 */

public class MainActivity extends Activity {

  private static String TAG = "MainActivity";
  private int REQUEST_ENABLE_BT = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    if (savedInstanceState == null) {
      showNearbyBeaconsFragment();
    }

    initialize();
  }

  private void showNearbyBeaconsFragment() {
    getFragmentManager().beginTransaction()
        .add(R.id.homeScreen_container, NearbyDevicesFragment.newInstance())
        .commit();
  }

  private void initialize() {
    Log.d(TAG, "<<<<<<<<<<<< MainActivity initialize >>>>>>>>>>>>");

    initializeActionBar();
    ensureBluetoothIsEnabled();
    stopDeviceDiscoveryService();
    startBeaconDiscoveryService();
  }

  /**
   * Setup the action bar at the top of the screen.
   */
  private void initializeActionBar() {
    getActionBar().setTitle(getString(R.string.title_nearby_beacons));
  }

  /**
   * Ensures Bluetooth is available on the beacon and it is enabled. If not,
   * displays a dialog requesting user permission to enable Bluetooth.
   */
  private void ensureBluetoothIsEnabled() {
    BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }
  }

  /**
   * Check if the service that tracks
   * currently available beacons is running.
   */
  private boolean checkIfBeaconDiscoveryServiceIsRunning() {
    ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
    List<ActivityManager.RunningServiceInfo> RunningServiceInfos = activityManager.getRunningServices(Integer.MAX_VALUE);
    for (ActivityManager.RunningServiceInfo runningServiceInfo : RunningServiceInfos) {
      if (runningServiceInfo.service.getClassName().equals(getString(R.string.service_name_device_discovery))) {
        return true;
      }
    }
    return false;
  }


  /////////////////////////////////
  // callbacks
  /////////////////////////////////

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(R.id.action_config).setVisible(true);
    menu.findItem(R.id.action_about).setVisible(true);
    return true;
  }

  /**
   * Called when a menu item is tapped.
   */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      // If the configuration menu item was selected
      case R.id.action_config:
        handleActionConfigMenuItemSelected();
        return true;
      // If the about menu item was selected
      case R.id.action_about:
        handleActionAboutMenuItemSelected();
    }
    return super.onOptionsItemSelected(item);
  }


  /////////////////////////////////
  // utilities
  /////////////////////////////////

  /**
   * Stop the beacon discovery service from running.
   */
  private void stopDeviceDiscoveryService() {
    Intent intent = new Intent(this, DeviceDiscoveryService.class);
    stopService(intent);
  }

  /**
   * Start up the BeaconDiscoveryService
   */
  private void startBeaconDiscoveryService() {
    if (!checkIfBeaconDiscoveryServiceIsRunning()) {
      Intent intent = new Intent(this, DeviceDiscoveryService.class);
      startService(intent);
    }
  }

  /**
   * Do a list of actions, given that
   * the config menu item was tapped.
   */
  private void handleActionConfigMenuItemSelected() {
    // Show the config ui
    showBeaconConfigFramgent();
  }

  /**
   * Show the ui for configuring a beacon,
   * which is a fragment.
   */
  private void showBeaconConfigFramgent() {
    BeaconConfigFragment beaconConfigFragment = BeaconConfigFragment.newInstance();
    getFragmentManager().beginTransaction()
        .setCustomAnimations(R.anim.fade_in_and_slide_up_fragment, R.anim.fade_out_fragment, R.anim.fade_in_activity, R.anim.fade_out_fragment)
        .replace(R.id.main_activity_container, beaconConfigFragment)
        .addToBackStack(null)
        .commit();
  }

  private void handleActionAboutMenuItemSelected() {
    // Show the about ui
    showAboutFragment();
  }

  private void showAboutFragment() {
    AboutFragment aboutFragment = AboutFragment.newInstance();
    getFragmentManager().beginTransaction()
        .setCustomAnimations(R.anim.fade_in_and_slide_up_fragment, R.anim.fade_out_fragment, R.anim.fade_in_activity, R.anim.fade_out_fragment)
        .replace(R.id.main_activity_container, aboutFragment)
        .addToBackStack(null)
        .commit();
  }

}
