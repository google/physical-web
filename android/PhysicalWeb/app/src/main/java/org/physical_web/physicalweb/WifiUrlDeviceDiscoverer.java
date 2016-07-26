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

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import android.webkit.URLUtil;
import org.physical_web.collection.UrlDevice;

import java.util.HashMap;
import java.util.Map;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.os.Handler;
import android.net.Uri;


class WifiUrlDeviceDiscoverer extends UrlDeviceDiscoverer {
  private static final String TAG = "wifidirect";
  private Context mContext;
  private WifiP2pManager mManager;
  private Channel mChannel;
  private WiFiDirectBroadcastReceiver mReceiver;
  private final IntentFilter mIntentFilter = new IntentFilter();
  private WifiP2pDnsSdServiceRequest mWifiP2pServiceRequest;
  private PeerListListener myPeerListListener = new PeerListListener() {
    @Override
    public void onPeersAvailable(WifiP2pDeviceList list) {
      Log.d(TAG,list.toString());
      for (WifiP2pDevice device : list.getDeviceList()) {
        if (Utils.isWifiDirectName(device.deviceName)) {
          String name = Utils.getTitleOrURL(device.deviceName);
          if (Utils.getWifiDirectPort(device.deviceName) == 0) {
            reportUrlDevice(createUrlDeviceBuilder("WifiDirect"+ name, name)
              .build());
          } else {
            reportUrlDevice(createUrlDeviceBuilder("WifiDirect"+ name, device.deviceAddress + Utils.getWifiDirectPort(device.deviceName))
              .setWifiAddress(device.deviceAddress)
              .setWifiPort(Utils.getWifiDirectPort(device.deviceName))
              .setTitle(name)
              .setDescription("")
              .build());
          }
        }
      }
    }
  };

  public WifiUrlDeviceDiscoverer(Context context) {
    mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
    mChannel = mManager.initialize(context, context.getMainLooper(), null);
    mContext = context;
    mReceiver = new WiFiDirectBroadcastReceiver(mManager,mChannel);
  }

  @Override
  public synchronized void startScanImpl() {
    Log.d(TAG,new Object(){}.getClass().getEnclosingMethod().getName());
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
    mContext.registerReceiver(mReceiver,intentFilter);
    mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
      @Override
      public void onSuccess() {
        Log.d(TAG,"discovering");
      } 

      @Override
      public void onFailure(int reasonCode) {
        Log.d(TAG,"discovery failed " + reasonCode);
      }
    });
  }

  @Override 
  public synchronized void stopScanImpl() {
    mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
      @Override
      public void onSuccess() {
        Log.d(TAG,"stop discovering");
      } 

      @Override
      public void onFailure(int reasonCode) {
        Log.d(TAG,"stop discovery failed " + reasonCode);
      }
    });
  }

  public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

      private WifiP2pManager manager;
      private Channel channel;

      /**
       * @param manager WifiP2pManager system service
       * @param channel Wifi p2p channel
       * @param activity activity associated with the receiver
       */
      public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel) {
          super();
          this.manager = manager;
          this.channel = channel;
      }

      @Override
      public void onReceive(Context context, Intent intent) {
          String action = intent.getAction();
          if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
              if (mManager != null) {
                  mManager.requestPeers(mChannel, myPeerListListener);
              }
          }
      }
  }
}
