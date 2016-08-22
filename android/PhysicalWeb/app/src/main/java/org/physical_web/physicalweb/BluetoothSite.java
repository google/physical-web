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
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * This class is used to connect to a ble gatt service and download a web page.
 */
public class BluetoothSite extends BluetoothGattCallback {

  private static final String TAG = BluetoothSite.class.getSimpleName();
  private static final String SERVICE_UUID = "ae5946d4-e587-4ba8-b6a5-a97cca6affd3";
  private Activity activity;
  private BluetoothGattCharacteristic characteristic;
  private ProgressDialog progress;
  private int transferRate = 20;
  private StringBuilder html;

  public BluetoothSite(Activity activity) {
    this.activity = activity;
  }

  /**
   * Connects to the Gatt service of the device to download a web page and displays a progress bar
   * for the title.
   * @param deviceAddress The mac address of the bar
   * @param title The title of the web page being downloaded
   */
  public void connect(String deviceAddress, String title) {
    String progressTitle = activity.getString(R.string.page_loading_title) + " " + title;
    progress = ProgressDialog.show(activity, progressTitle,
        activity.getString(R.string.page_loading_message), true);
    BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress)
        .connectGatt(activity, false, this);
  }

  @Override
  public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
      int status) {
    if (status == BluetoothGatt.GATT_SUCCESS
        && characteristic.getValue().length < transferRate) {
      // Transfer is complete
      html.append(new String(characteristic.getValue()));
      progress.dismiss();
      gatt.close();
      File websiteDir = new File(activity.getFilesDir(), "Websites");
      websiteDir.mkdir();
      File file = new File(websiteDir, "website.html");
      writeToFile(file);
      if (file != null) {
        openInChrome(file);
      }
    } else if (status == BluetoothGatt.GATT_SUCCESS) {
      // Full packet received, check for more data
      html.append(new String(characteristic.getValue()));
      gatt.readCharacteristic(this.characteristic);
    } else {
      Log.i(TAG, "onCharacteristicRead unsuccessful: " + status);
      gatt.disconnect();
      progress.dismiss();
      Toast.makeText(activity, R.string.ble_download_error_message, Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
    if (newState == BluetoothProfile.STATE_CONNECTED) {
      Log.i(TAG, "Connected to GATT server");
      html = new StringBuilder("");
      if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
        gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        gatt.requestMtu(505);
      } else {
        gatt.discoverServices();
      }
    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
      Log.i(TAG, "Disconnected to GATT server");
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
    if (status == BluetoothGatt.GATT_SUCCESS) {
      List<BluetoothGattService> services = gatt.getServices();
      for (BluetoothGattService gattService : services) {
        if (gattService.getUuid().toString().equals(SERVICE_UUID)) {
          characteristic = gattService.getCharacteristics().get(0);
          gatt.readCharacteristic(characteristic);
        }
      }
    }
  }

  private void openInChrome(File file) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    Uri contentUri = new FileProvider()
        .getUriForFile(activity, "org.physical_web.fileprovider", file);
    activity.grantUriPermission("com.android.chrome", contentUri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION);
    intent.setDataAndType(contentUri, "text/html");
    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    activity.startActivity(intent);
  }

  private void writeToFile(File file) {
    FileOutputStream outputStream;
    try {
      outputStream = new FileOutputStream(file);
      try {
        outputStream.write(html.toString().getBytes());
      } catch (IOException e) {
        Log.e(TAG, "Failed to write to file");
      } finally {
        outputStream.close();
      }
    } catch (FileNotFoundException e) {
      Log.e(TAG, "File not found: " + file.getName());
    } catch (IOException e) { }
  }

}
