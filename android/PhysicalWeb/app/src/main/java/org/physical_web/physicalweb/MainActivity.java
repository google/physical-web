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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * The main entry point for the app.
 */

public class MainActivity extends Activity {
  private static final String TAG  = "MainActivity";
  private static final int REQUEST_ENABLE_BT = 0;
  private static final int REQUEST_LOCATION = 1;
  private static final String NEARBY_BEACONS_FRAGMENT_TAG = "NearbyBeaconsFragmentTag";
  private boolean mCheckingPermissions = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mCheckingPermissions = false;
    setContentView(R.layout.activity_main);
    Utils.setSharedPreferencesDefaultValues(this);
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
      // If the settings menu item was selected
      case R.id.action_settings:
        showSettingsFragment();
        return true;
      // If the action bar up button was pressed
      case android.R.id.home:
        getFragmentManager().popBackStack();
        getActionBar().setDisplayHomeAsUpEnabled(false);
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * Ensures Bluetooth is available on the beacon and it is enabled. If not,
   * displays a dialog requesting user permission to enable Bluetooth.
   */
  private void ensureBluetoothAndLocationIsEnabled(BluetoothAdapter bluetoothAdapter) {
    // Acquire lock
    mCheckingPermissions = true;
    if (!bluetoothAdapter.isEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
      return;
    }
    ensureLocationPermissionIsEnabled();
  }

  @Override
  protected void onActivityResult (int requestCode, int resultCode, Intent data) {
    Log.d(TAG, "onActivityResult");
    if (requestCode == REQUEST_ENABLE_BT && resultCode == -1) {
      ensureLocationPermissionIsEnabled();
      return;
    }
    Toast.makeText(this, getString(R.string.bt_on), Toast.LENGTH_LONG).show();
    finish();
  }

  private void ensureLocationPermissionIsEnabled() {
    if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this,
        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{
          android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
      return;
    }
    // Release lock
    mCheckingPermissions = false;
    finishLoad();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
      String permissions[], int[] grantResults) {
    switch (requestCode) {
      case REQUEST_LOCATION: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          mCheckingPermissions = false;
        } else {
          Toast.makeText(getApplicationContext(),
              getString(R.string.loc_permission), Toast.LENGTH_LONG).show();
          finish();
        }
        break;
      }
      default:
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Lock to prevent onResume from running until all permissions are granted
    if (!mCheckingPermissions) {
      Log.d(TAG, "resumed MainActivity");
      BluetoothManager btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
      BluetoothAdapter btAdapter = btManager != null ? btManager.getAdapter() : null;
      if (btAdapter == null) {
        Toast.makeText(getApplicationContext(),
            R.string.error_bluetooth_support, Toast.LENGTH_LONG).show();
        finish();
        return;
      }
      if (checkIfUserHasOptedIn()) {
        Log.d(TAG, "checkingPermissions");
        ensureBluetoothAndLocationIsEnabled(btAdapter);
      } else {
        // Show the oob activity
        Intent intent = new Intent(this, OobActivity.class);
        startActivity(intent);
      }
    }
  }

  public boolean isCheckingPermissions() {
    return mCheckingPermissions;
  }

  private void finishLoad() {
    Intent intent = new Intent(this, ScreenListenerService.class);
    startService(intent);
    NearbyBeaconsFragment nearbyBeaconsFragment =
        (NearbyBeaconsFragment) getFragmentManager().findFragmentByTag(NEARBY_BEACONS_FRAGMENT_TAG);
    if (nearbyBeaconsFragment != null) {
      nearbyBeaconsFragment.restartScan();
    } else  {
      getFragmentManager().beginTransaction()
          .replace(R.id.main_activity_container, NearbyBeaconsFragment.newInstance(this),
              NEARBY_BEACONS_FRAGMENT_TAG)
          .commit();
    }
  }

  /**
   * Show the fragment configuring a beacon.
   */
  private void showBeaconConfigFragment() {
    BeaconConfigFragment beaconConfigFragment = new BeaconConfigFragment();
    getFragmentManager().beginTransaction()
        .setCustomAnimations(R.anim.fade_in_and_slide_up_fragment, R.anim.fade_out_fragment,
                             R.anim.fade_in_activity, R.anim.fade_out_fragment)
        .replace(R.id.main_activity_container, beaconConfigFragment)
        .addToBackStack(null)
        .commit();
  }

  private void showSettingsFragment(){
    SettingsFragment settingsFragment = new SettingsFragment();
    getFragmentManager().beginTransaction()
        .setCustomAnimations(R.anim.fade_in_and_slide_up_fragment, R.anim.fade_out_fragment,
                             R.anim.fade_in_activity, R.anim.fade_out_fragment)
        .replace(R.id.main_activity_container, settingsFragment)
        .addToBackStack(null)
        .commit();
  }

  /**
   * Show the fragment displaying information about this application.
   */
  private void showAboutFragment() {
    AboutFragment aboutFragment = new AboutFragment();
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
