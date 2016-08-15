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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;

/**
 * This class is for using WifiDirect to discover Physical Web
 * Devices through peer discovery.
 */
class WifiUrlDeviceDiscoverer extends UrlDeviceDiscoverer {
  private static final String TAG = WifiUrlDeviceDiscoverer.class.getSimpleName();
  private Context mContext;
  private WifiP2pManager mManager;
  private Channel mChannel;
  private IntentFilter mIntentFilter = new IntentFilter();
  private boolean mIsRunning = false;
  private BroadcastReceiver mReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)
          && mManager != null) {
        mManager.requestPeers(mChannel, mPeerListener);
      }
    }
  };
  private final PeerListListener mPeerListener = new PeerListListener() {
    @Override
    public void onPeersAvailable(WifiP2pDeviceList list) {
      Log.d(TAG, list.toString());
      for (WifiP2pDevice device : list.getDeviceList()) {
        Utils.WifiDirectInfo info = Utils.parseWifiDirectName(device.deviceName);
        if (info != null) {
          String name = info.title;
          int port = info.port;
          reportUrlDevice(createUrlDeviceBuilder("WifiDirect" + name,
              device.deviceAddress + ":" + port)
            .setWifiAddress(device.deviceAddress)
            .setWifiPort(port)
            .setTitle(name)
            .setDescription("")
            .setDeviceType(Utils.WIFI_DIRECT_DEVICE_TYPE)
            .build());
        }
      }
    }
  };

  public WifiUrlDeviceDiscoverer(Context context) {
    mContext = context;
  }

  @Override
  public synchronized void startScanImpl() {
    mManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
    mChannel = mManager.initialize(mContext, mContext.getMainLooper(), null);
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
    mContext.registerReceiver(mReceiver, intentFilter);
    mIsRunning = true;
    discoverHelper(true);
  }

  private void discoverHelper(boolean retry) {
    mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
      @Override
      public void onSuccess() {
      }

      @Override
      public void onFailure(int reasonCode) {
        if (retry && reasonCode == WifiP2pManager.BUSY) {
          mManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
              Log.d(TAG, "cancel connect call success");
              if (mIsRunning) {
                discoverHelper(false);
              }
            }
            @Override
            public void onFailure(int reason) {
              Log.d(TAG, "cancel connect call fail " + reason);
            }
          });
        }
      }
    });
  }

  @Override
  public synchronized void stopScanImpl() {
    mIsRunning = false;
    mContext.unregisterReceiver(mReceiver);
    mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
      @Override
      public void onSuccess() {
        Log.d(TAG, "stop discovering");
      }

      @Override
      public void onFailure(int reasonCode) {
        Log.d(TAG, "stop discovery failed " + reasonCode);
      }
    });
  }
}
