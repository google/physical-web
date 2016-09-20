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

import android.annotation.TargetApi;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Shares URLs via bluetooth.
 * Also interfaces with PWS to shorten URLs that are too long for Eddystone URLs.
 * Lastly, it surfaces a persistent notification whenever a URL is currently being broadcast.
 **/
@TargetApi(21)
public class FileBroadcastService extends Service {

    private static final String TAG = FileBroadcastService.class.getSimpleName();
    private static final int BROADCASTING_NOTIFICATION_ID = 7;
    public static final String FILE_KEY = "file";
    public static final String MIME_TYPE_KEY = "type";
    public static final String TITLE_KEY = "title";
    private static final String DEFAULT_DEVICE_NAME = "PW-Share-";
    private static final int MAX_DEVICE_NAME_LENGTH = 30;
    private int mPort;
    private NotificationManagerCompat mNotificationManager;
    private Uri mUri;
    private String mType;
    private String mTitle;
    private byte[] mFile;
    private FileBroadcastServer mFileBroadcastServer;

    /////////////////////////////////
    // callbacks
    /////////////////////////////////

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
      if (mFileBroadcastServer != null) {
        mFileBroadcastServer.stop();
      }
      mType = intent.getStringExtra(MIME_TYPE_KEY);
      Log.d(TAG, mType);
      mUri = Uri.parse(intent.getStringExtra(FILE_KEY));
      Log.d(TAG, mUri.toString());
      mTitle = intent.getStringExtra(TITLE_KEY);
      mTitle = mTitle == null ? "Share" : mTitle;
      mPort = Utils.getWifiDirectPort(this);
      try {
        mFile = Utils.getBytes(getContentResolver().openInputStream(mUri));
      } catch (FileNotFoundException e) {
        Log.d(TAG, e.getMessage());
        stopSelf();
        return START_STICKY;
      } catch (IOException e) {
        Log.d(TAG, e.getMessage());
        stopSelf();
        return START_STICKY;
      }
      mNotificationManager = NotificationManagerCompat.from(this);
      mFileBroadcastServer = new FileBroadcastServer(mPort, mType, mFile);
      try {
        mFileBroadcastServer.start();
        Utils.createBroadcastNotification(this, stopServiceReceiver, BROADCASTING_NOTIFICATION_ID,
            getString(R.string.wifi_direct_notification_title), Integer.toString(mPort),
            "myFilter2");
      } catch (IOException e) {
        Log.d(TAG, e.getMessage());
        stopSelf();
        return START_STICKY;
      }
      sendBroadcast(new Intent("server"));
      WifiP2pManager mManager = (WifiP2pManager) this.getSystemService(Context.WIFI_P2P_SERVICE);
      WifiP2pManager.Channel mChannel = mManager.initialize(this, this.getMainLooper(), null);
      changeWifiName();
      mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
          Log.d(TAG, "discovering");
        }

        @Override
        public void onFailure(int reasonCode) {
          Log.d(TAG, "discovery failed " + reasonCode);
        }
      });
      Toast.makeText(this, R.string.wifi_direct_broadcasting_confirmation, Toast.LENGTH_SHORT)
          .show();
      return START_STICKY;
    }



    @Override
    public void onDestroy() {
      Log.d(TAG, "SERVICE onDestroy");
      unregisterReceiver(stopServiceReceiver);
      mFileBroadcastServer.stop();
      mNotificationManager.cancel(BROADCASTING_NOTIFICATION_ID);
      super.onDestroy();
    }

    protected BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        Log.d(TAG, context.toString());
        stopSelf();
      }
    };

    private void changeWifiName() {
      String deviceName = "PW-" + mTitle + "-" + mPort;
      if (deviceName.length() > MAX_DEVICE_NAME_LENGTH) {
        deviceName = DEFAULT_DEVICE_NAME + mPort;
      }
      try {
        WifiP2pManager manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        WifiP2pManager.Channel channel = manager.initialize(this, getMainLooper(), null);
        Class[] paramTypes = new Class[3];
        paramTypes[0] = WifiP2pManager.Channel.class;
        paramTypes[1] = String.class;
        paramTypes[2] = WifiP2pManager.ActionListener.class;
        Method setDeviceName = manager.getClass().getMethod(
            "setDeviceName", paramTypes);
        setDeviceName.setAccessible(true);

        Object arglist[] = new Object[3];
        arglist[0] = channel;
        arglist[1] = deviceName;
        arglist[2] = new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "setDeviceName succeeded");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "setDeviceName failed");
            }
        };
        setDeviceName.invoke(manager, arglist);

      } catch (NoSuchMethodException e) {
        Log.d(TAG, e.getMessage());
      } catch (IllegalAccessException e) {
        Log.d(TAG, e.getMessage());
      } catch (IllegalArgumentException e) {
        Log.d(TAG, e.getMessage());
      } catch (InvocationTargetException e) {
        Log.d(TAG, e.getMessage());
      }
    }
}
