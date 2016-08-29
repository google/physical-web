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

import org.physical_web.collection.UrlDevice;

import android.app.Activity;
import android.content.Intent;

/**
 * Activity that handles intents for offline transports that need to be connected to before opening
 * the page. This is used when the user clicks on the notification for a connectable transports,
 * such as FatBeacon.
 */
public class OfflineTransportConnectionActivity extends Activity {
  private static final String TAG  = OfflineTransportConnectionActivity.class.getSimpleName();
  public static final String EXTRA_DEVICE_ADDRESS = "address";
  public static final String EXTRA_DEVICE_PORT = "port";
  public static final String EXTRA_PAGE_TITLE = "title";
  public static final String EXTRA_CONNECTION_TYPE = "connection_type";
  public static final String EXTRA_FAT_BEACON_CONNECTION = "fat_beacon";
  public static final String EXTRA_WIFI_DIRECT_CONNECTION = "wi-fi_direct";

  private ConnectionListener listener = new ConnectionListener() {
    @Override
    public void onConnectionFinished() {
      finish();
    }
  };

  @Override
  protected void onStart() {
    super.onStart();
    Intent intent = getIntent();
    String connectionType = intent.getStringExtra(EXTRA_CONNECTION_TYPE);
    String deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
    int devicePort = intent.getIntExtra(EXTRA_DEVICE_PORT, -1);
    String title = intent.getStringExtra(EXTRA_PAGE_TITLE);
    if (connectionType.equals(EXTRA_FAT_BEACON_CONNECTION) && deviceAddress != null &&
        title != null) {
      (new BluetoothSite(this)).connect(deviceAddress, title, listener);
    } else if (connectionType.equals(EXTRA_WIFI_DIRECT_CONNECTION) && deviceAddress != null &&
        devicePort != -1 && title != null) {
      UrlDevice device = new Utils.UrlDeviceBuilder("id", "url")
          .setWifiAddress(deviceAddress)
          .setWifiPort(devicePort)
          .build();
      (new WifiDirectConnect(this)).connect(device, title, listener);
    } else {
      finish();
    }
  }

}
