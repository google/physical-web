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

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.widget.Toast;


/**
 * This class is for using WifiDirect to create a WifiDirect
 * P2P connection with the Physical Web Device.
 */
class WifiDirectConnect {
  private static final String TAG = WifiDirectConnect.class.getSimpleName();
  private ProgressDialog mProgress;
  private Context mContext;
  private WifiP2pManager mManager;
  private Channel mChannel;
  private UrlDevice mDevice;
  private BroadcastReceiver mReceiver;
  private ConnectionListener mCallback;

  public WifiDirectConnect(Context context) {
    mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
    mChannel = mManager.initialize(context, context.getMainLooper(), null);
    mContext = context;
    mReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
          if (mManager == null) {
            return;
          }
          NetworkInfo networkInfo = (NetworkInfo) intent
              .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
          if (networkInfo.isConnected()) {
            mManager.requestConnectionInfo(mChannel,
                new WifiP2pManager.ConnectionInfoListener() {
                  @Override
                  public void onConnectionInfoAvailable(final WifiP2pInfo info) {
                    if (mDevice != null) {
                      Intent intent = new Intent(Intent.ACTION_VIEW);
                      intent.setData(Uri.parse("http:/" + info.groupOwnerAddress + ":" +
                          Integer.toString(Utils.getWifiPort(mDevice))));
                      mContext.startActivity(intent);
                      close();
                    }
                  }
                });
          }
        }
      }
    };
  }

  public void connect(UrlDevice urlDevice, String title) {
    String progressTitle = mContext.getString(R.string.page_loading_title) + " " + title;
    mProgress = new ProgressDialog(mContext);
    mProgress.setCancelable(true);
    mProgress.setOnCancelListener(new OnCancelListener() {
      @Override
      public void onCancel(DialogInterface dialogInterface) {
        Log.i(TAG, "Dialog box canceled");
        mDevice = null;
        mManager.cancelConnect(mChannel, new ActionListener() {
          @Override
          public void onSuccess() {
            Log.d(TAG, "cancel connect call success");
          }
          @Override
          public void onFailure(int reason) {
            Log.d(TAG, "cancel connect call fail " + reason);
          }
        });
      }
    });
    mProgress.setTitle(progressTitle);
    mProgress.setMessage(mContext.getString(R.string.page_loading_message));
    mProgress.show();
    mDevice = urlDevice;
    WifiP2pConfig config = new WifiP2pConfig();
    config.deviceAddress = Utils.getWifiAddress(mDevice);
    config.groupOwnerIntent = 0;
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
    mContext.registerReceiver(mReceiver, intentFilter);
    mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
      public void onGroupInfoAvailable(final WifiP2pGroup group) {
        if (group != null) {
          Log.d(TAG, "group not null");
          if (group.getOwner().deviceAddress.equals(Utils.getWifiAddress(mDevice))) {
            Log.i(TAG, "Already connected");
            mManager.requestConnectionInfo(mChannel,
              new WifiP2pManager.ConnectionInfoListener() {
                public void onConnectionInfoAvailable(final WifiP2pInfo info) {
                  if (mDevice != null && info.groupOwnerAddress != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("http:/" + info.groupOwnerAddress + ":" +
                        Integer.toString(Utils.getWifiPort(mDevice))));
                    mContext.startActivity(intent);
                  }
                }
              }
            );
          } else {
            mManager.removeGroup(mChannel, new ActionListener() {
              @Override
              public void onSuccess() {
                Log.d(TAG, "remove call success");
                connectHelper(true, config);
              }

              @Override
              public void onFailure(int reason) {
                Log.d(TAG, "remove call fail " + reason);
              }
            });
          }
        } else {
          Log.d(TAG, "group null");
          connectHelper(true, config);
        }
      }
    });
  }

  public void connect(UrlDevice urlDevice, String title, ConnectionListener callback) {
    mCallback = callback;
    connect(urlDevice, title);
  }

  private void connectHelper(boolean retry, WifiP2pConfig config) {
    mManager.connect(mChannel, config, new ActionListener() {
      @Override
      public void onSuccess() {
        Log.d(TAG, "connect call success");
        Toast.makeText(mContext, R.string.wifi_direct_connection_succeeded, Toast.LENGTH_SHORT)
            .show();
      }

      @Override
      public void onFailure(int reason) {
        Log.d(TAG, "connect call fail " + reason);
        if (retry && reason == WifiP2pManager.BUSY) {
          mManager.cancelConnect(mChannel, new ActionListener() {
            @Override
            public void onSuccess() {
              Log.d(TAG, "cancel connect call success");
              connectHelper(false, config);
            }
            @Override
            public void onFailure(int reason) {
              Log.d(TAG, "cancel connect call fail " + reason);
              Toast.makeText(mContext, R.string.wifi_direct_connection_failed, Toast.LENGTH_SHORT)
                  .show();
              close();
            }
          });
        } else {
          Toast.makeText(mContext, R.string.wifi_direct_connection_failed, Toast.LENGTH_SHORT)
              .show();
          close();
        }
      }
    });
  }

  private void close() {
    mProgress.dismiss();
    if (mCallback != null) {
      mCallback.onConnectionFinished();
    }
  }
}
