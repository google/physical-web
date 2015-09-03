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

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * The main entry point for the app.
 */

public class MainActivity extends Activity {
  private static final int REQUEST_ENABLE_BT = 0;
  private static final String NEARBY_BEACONS_FRAGMENT_TAG = "NearbyBeaconsFragmentTag";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }

  /**
   * Ensures Bluetooth is available on the beacon and it is enabled. If not,
   * displays a dialog requesting user permission to enable Bluetooth.
   */
  private void ensureBluetoothIsEnabled(BluetoothAdapter bluetoothAdapter) {
    if (!bluetoothAdapter.isEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
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
        showBeaconConfigFragment();
        return true;
      // If the about menu item was selected
      case R.id.action_about:
        showAboutFragment();
        return true;
      // If the action bar up button was pressed
      case android.R.id.home:
        getFragmentManager().popBackStack();
        getActionBar().setDisplayHomeAsUpEnabled(false);
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onResume() {
    super.onResume();
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (bluetoothAdapter == null) {
      Toast.makeText(getApplicationContext(),
              R.string.error_bluetooth_support, Toast.LENGTH_LONG).show();
      finish();
      return;
    }
    if (checkIfUserHasOptedIn()) {
      ensureBluetoothIsEnabled(bluetoothAdapter);
      showNearbyBeaconsFragment();
      Intent intent = new Intent(this, ScreenListenerService.class);
      startService(intent);
    } else {
      // Show the oob activity
      Intent intent = new Intent(this, OobActivity.class);
      startActivity(intent);
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
  }

  /**
   * Show the fragment scanning nearby UriBeacons.
   */
  private void showNearbyBeaconsFragment() {
    // Look for an instance of the nearby beacons fragment
    Fragment nearbyBeaconsFragment =
        getFragmentManager().findFragmentByTag(NEARBY_BEACONS_FRAGMENT_TAG);
    // If the fragment does not exist
    if (nearbyBeaconsFragment == null) {
      // Create the fragment
      getFragmentManager().beginTransaction()
          .replace(R.id.main_activity_container, NearbyBeaconsFragment.newInstance(),
                   NEARBY_BEACONS_FRAGMENT_TAG)
          .commit();
      // If the fragment does exist
    } else {
      // If the fragment is not currently visible
      if (!nearbyBeaconsFragment.isVisible()) {
        // Assume another fragment is visible, so pop that fragment off the stack
        getFragmentManager().popBackStack();
      }
    }
  }

  /**
   * Show the fragment configuring a beacon.
   */
  private void showBeaconConfigFragment() {
    BeaconConfigFragment beaconConfigFragment = BeaconConfigFragment.newInstance();
    getFragmentManager().beginTransaction()
        .setCustomAnimations(R.anim.fade_in_and_slide_up_fragment, R.anim.fade_out_fragment,
                             R.anim.fade_in_activity, R.anim.fade_out_fragment)
        .replace(R.id.main_activity_container, beaconConfigFragment)
        .addToBackStack(null)
        .commit();
  }

  /**
   * Show the fragment displaying information about this application.
   */
  private void showAboutFragment() {
    AboutFragment aboutFragment = AboutFragment.newInstance();
    getFragmentManager().beginTransaction()
        .setCustomAnimations(R.anim.fade_in_and_slide_up_fragment, R.anim.fade_out_fragment,
                             R.anim.fade_in_activity, R.anim.fade_out_fragment)
        .replace(R.id.main_activity_container, aboutFragment)
        .addToBackStack(null)
        .commit();
  }

  private boolean checkIfUserHasOptedIn() {
    String preferencesKey = getString(R.string.main_prefs_key);
    SharedPreferences sharedPreferences = getSharedPreferences(preferencesKey,
                                                               Context.MODE_PRIVATE);
    return sharedPreferences.getBoolean(getString(R.string.user_opted_in_flag), false);
  }
}
