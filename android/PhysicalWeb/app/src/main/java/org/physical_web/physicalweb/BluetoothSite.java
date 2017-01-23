/*
 * Copyright 2016 Google Inc. All rights reserved.
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
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * This class is used to connect to a ble gatt service and download a web page.
 */
public class BluetoothSite extends BluetoothGattCallback {

  private static final String TAG = BluetoothSite.class.getSimpleName();
  private static final UUID SERVICE_UUID = UUID.fromString("ae5946d4-e587-4ba8-b6a5-a97cca6affd3");
  private static final UUID CHARACTERISTIC_WEBPAGE_UUID = UUID.fromString(
      "d1a517f0-2499-46ca-9ccc-809bc1c966fa");
  // This is BluetoothGatt.CONNECTION_PRIORITY_HIGH, from API level 21
  private static final int CONNECTION_PRIORITY_HIGH = 1;
  private FileOutputStream mHtml;
  private Activity activity;
  private BluetoothGatt mBluetoothGatt;
  private BluetoothGattCharacteristic characteristic;
  private ConnectionListener mCallback;
  private ProgressDialog progress;
  private int transferRate = 20;
  private boolean running = false;

  public BluetoothSite(Activity activity) {
    this.activity = activity;
  }


  public Boolean isRunning() {
    return running;
  }

  /**
   * Connects to the Gatt service of the device to download a web page and displays a progress bar
   * for the title.
   * @param deviceAddress The mac address of the bar
   * @param title The title of the web page being downloaded
   */
  public void connect(String deviceAddress, String title) {
    running = true;
    String progressTitle = activity.getString(R.string.page_loading_title) + " " + title;
    progress = new ProgressDialog(activity);
    progress.setCancelable(true);
    progress.setOnCancelListener(new OnCancelListener() {
      @Override
      public void onCancel(DialogInterface dialogInterface) {
        Log.i(TAG, "Dialog box canceled");
        close();
      }
    });
    progress.setTitle(progressTitle);
    progress.setMessage(activity.getString(R.string.page_loading_message));
    progress.show();
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mBluetoothGatt = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress)
            .connectGatt(activity, false, BluetoothSite.this);
      }
    });
  }

  /**
   * Connects to the Gatt service of the device to download a web page and displays a progress bar
   * for the title.
   * @param deviceAddress The mac address of the bar
   * @param title The title of the web page being downloaded
   * @param callback The callback for when the connection is complete.
   */
  public void connect(String deviceAddress, String title, ConnectionListener callback) {
    mCallback = callback;
    connect(deviceAddress, title);
  }

  @Override
  public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
      int status) {
    // Make sure the site is running.  It can stop if the dialog is dismissed very quickly.
    if (!isRunning()) {
      close();
      return;
    }

    // Make sure the read was successful.
    if (status != BluetoothGatt.GATT_SUCCESS) {
      Log.i(TAG, "onCharacteristicRead unsuccessful: " + status);
      close();
      Toast.makeText(activity, R.string.ble_download_error_message, Toast.LENGTH_SHORT).show();
      return;
    }

    // Record the data.
    Log.i(TAG, "onCharacteristicRead successful");
    try {
      mHtml.write(characteristic.getValue());
    } catch (IOException e) {
      Log.e(TAG, "Could not write to buffer", e);
      close();
      return;
    }

    // Request a new read if we are not done.
    if (characteristic.getValue().length == transferRate) {
      gatt.readCharacteristic(this.characteristic);
      return;
    }

    // At this point we are done.  Show the file.
    Log.i(TAG, "transfer is complete");
    close();
    openInChrome(getHtmlFile());
  }

  @Override
  public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
    if (newState == BluetoothProfile.STATE_CONNECTED && status == gatt.GATT_SUCCESS) {
      Log.i(TAG, "Connected to GATT server");
      mBluetoothGatt = gatt;
      if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
        gatt.requestConnectionPriority(CONNECTION_PRIORITY_HIGH);
        gatt.requestMtu(505);
      } else {
        gatt.discoverServices();
      }
    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
      Log.i(TAG, "Disconnected to GATT server");
      // ensure progress dialog is removed and running is set false
      close();
    } else if (status != gatt.GATT_SUCCESS) {
      Log.i(TAG, "Status is " + status);
      close();
    }
  }


  @Override
  public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
    Log.i(TAG, "MTU changed to " + mtu);
    transferRate = mtu - 5;
    gatt.discoverServices();
  }

  @Override
  public void onServicesDiscovered(BluetoothGatt gatt, int status) {
    Log.i(TAG, "Services Discovered");
    if (status != BluetoothGatt.GATT_SUCCESS) {
      Log.e(TAG, "Service discovery failed!");
      return;
    }

    try {
      mHtml = new FileOutputStream(getHtmlFile());
    } catch (FileNotFoundException e) {
      Log.e(TAG, "File not found", e);
      return;
    }

    characteristic = gatt.getService(SERVICE_UUID).getCharacteristic(CHARACTERISTIC_WEBPAGE_UUID);
    gatt.readCharacteristic(characteristic);
  }

  private void close() {
    if (mCallback != null) {
      mCallback.onConnectionFinished();
    }
    if (progress != null) {
      progress.dismiss();
    }

    if (mHtml != null) {
      try {
        mHtml.close();
      } catch (IOException e) {
        Log.e(TAG, "Failed to close file", e);
        return;
      }
      mHtml = null;
    }

    running = false;
    if (mBluetoothGatt == null) {
      return;
    }
    mBluetoothGatt.close();
    mBluetoothGatt = null;
  }

  private File getHtmlFile() {
    File websiteDir = new File(activity.getFilesDir(), "Websites");
    websiteDir.mkdir();
    return new File(websiteDir, "website.html");
  }

  private File getTempFile() {
    File websiteDir = new File(activity.getFilesDir(), "Websites");
    websiteDir.mkdir();
    return new File(websiteDir, "temp.html");
  }

  private void openInChrome(File file) {
    if(Utils.isGzippedFile(file)) {
      file = Utils.gunzip(file, getTempFile());
    }
    Intent intent = new Intent(Intent.ACTION_VIEW);
    Uri contentUri = new FileProvider()
        .getUriForFile(activity, "org.physical_web.fileprovider", file);
    activity.grantUriPermission("com.android.chrome", contentUri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION);
    intent.setDataAndType(contentUri, "text/html");
    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    activity.startActivity(intent);
  }
}
