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
import android.net.Uri;
import org.physical_web.collection.UrlDevice;


class WifiDirectConnect {
  private static final String TAG = "WifiDirect";
  private Context mContext;
  private WifiP2pManager mManager;
  private Channel mChannel;
  private WiFiDirectBroadcastReceiver mReceiver;
  private UrlDevice mDevice;

  public WifiDirectConnect(Context context, UrlDevice urlDevice) {
    mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
    mChannel = mManager.initialize(context, context.getMainLooper(), null);
    mContext = context;
    mDevice = urlDevice;
    mReceiver = new WiFiDirectBroadcastReceiver(mManager,mChannel);
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
    mContext.registerReceiver(mReceiver,intentFilter);
    WifiP2pConfig config = new WifiP2pConfig();
    config.deviceAddress = Utils.getWifiAddress(mDevice);
    config.groupOwnerIntent = 0;
    mManager.connect(mChannel, config, new ActionListener() {
        @Override
        public void onSuccess() {
          Log.d(TAG,"connect call success");
        }

        @Override
        public void onFailure(int reason) {
          Log.d(TAG,"connect call fail " + reason);
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

      /*
       * (non-Javadoc)
       * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
       * android.content.Intent)
       */
      @Override
      public void onReceive(Context context, Intent intent) {
          String action = intent.getAction();
          if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
              if (manager == null) {
                  return;
              }
              NetworkInfo networkInfo = (NetworkInfo) intent
                      .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
              if (networkInfo.isConnected()) {
                  manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
                      public void onConnectionInfoAvailable(final WifiP2pInfo info) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("http:/" + info.groupOwnerAddress +":" + 
                            Integer.toString(Utils.getWifiPort(mDevice))));
                        mContext.startActivity(intent);
                        mContext.unregisterReceiver(mReceiver);
                      }
                  });
              }
          }
      }
  }
}
