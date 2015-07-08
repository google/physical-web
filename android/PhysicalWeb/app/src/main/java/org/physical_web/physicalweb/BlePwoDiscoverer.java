/*
 * Copyright 2015 Google Inc. All rights reserved.
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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.webkit.URLUtil;

import org.uribeacon.beacon.UriBeacon;
import org.uribeacon.scan.compat.ScanRecord;

import java.util.List;

class BlePwoDiscoverer extends PwoDiscoverer implements BluetoothAdapter.LeScanCallback {
  private static final String TAG = "BlePwoDiscoverer";
  private BluetoothAdapter mBluetoothAdapter;
  private Parcelable[] mScanFilterUuids;
  private boolean isRunning;

  public BlePwoDiscoverer(Context context) {
    final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(
        Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = bluetoothManager.getAdapter();
    mScanFilterUuids = new ParcelUuid[]{UriBeacon.URI_SERVICE_UUID, UriBeacon.TEST_SERVICE_UUID};
  }

  private boolean leScanMatches(ScanRecord scanRecord) {
    if (mScanFilterUuids == null) {
      return true;
    }
    List services = scanRecord.getServiceUuids();
    if (services != null) {
      for (Parcelable uuid : mScanFilterUuids) {
        if (services.contains(uuid)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanBytes) {
    if (!leScanMatches(ScanRecord.parseFromBytes(scanBytes))) {
      return;
    }

    UriBeacon uriBeacon = UriBeacon.parseFromBytes(scanBytes);
    if (uriBeacon == null) {
      return;
    }

    String url = uriBeacon.getUriString();
    if (!URLUtil.isNetworkUrl(url)) {
      return;
    }

    PwoMetadata pwoMetadata = createPwoMetadata(url);
    pwoMetadata.setBleMetadata(device.getAddress(), rssi, uriBeacon.getTxPowerLevel());
    pwoMetadata.bleMetadata.updateRegionInfo();
    reportPwo(pwoMetadata);
  }

  @Override
  @SuppressWarnings("deprecation")
  public synchronized void startScanImpl() {
    mBluetoothAdapter.startLeScan(this);
  }

  @Override
  @SuppressWarnings("deprecation")
  public synchronized void stopScanImpl() {
    mBluetoothAdapter.stopLeScan(this);
  }
}
