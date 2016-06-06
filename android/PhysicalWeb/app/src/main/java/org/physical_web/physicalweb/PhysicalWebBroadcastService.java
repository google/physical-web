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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import java.net.URI;
import java.net.URISyntaxException;

import org.physical_web.physicalweb.ble.AdvertiseDataUtils;
import org.physical_web.physicalweb.ble.UriBeacon;
/**
  * Shares URLs via bluetooth.
  * Also interfaces with PWS to shorten URLs that are too long for Eddystone URLs.
  * Lastly, it surfaces a persistent notification whenever a URL is currently being broadcast.
**/

@TargetApi(21)
public class PhysicalWebBroadcastService extends Service {

    private static final String TAG = "PhysicalWebBroadcastService";
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private static final int BROADCASTING_NOTIFICATION_ID = 6;
    public static final String DISPLAY_URL_KEY = "displayUrl";
    public static final String PREVIOUS_BROADCAST_URL_KEY = "previousUrl";
    public static final int MAX_URI_LENGTH = 18;
    private NotificationManagerCompat mNotificationManager;
    private Handler mHandler = new Handler();
    private String mDisplayUrl;
    private String mShareUrl;
    private boolean mStartedByRestart;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "in receiver");
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "stop because BT off");
                        stopSelf();
                        break;
                    default:

                }
            }
        }
    };

    /////////////////////////////////
    // callbacks
    /////////////////////////////////

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SERVICE onCreate");
        BluetoothManager bluetoothManager = (BluetoothManager) getApplicationContext()
            .getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothLeAdvertiser = bluetoothManager.getAdapter().getBluetoothLeAdvertiser();
        mNotificationManager = NotificationManagerCompat.from(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        fetchBroadcastData(intent);
        if (mDisplayUrl == null){
            stopSelf();
            return START_STICKY;
        }
        Log.d(TAG, "SERVICE onStartCommand");
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        Log.d(TAG, mDisplayUrl);

        if (hasValidUrlLength(mDisplayUrl) && checkAndHandleAsciiUrl(mDisplayUrl)) {
            // Set the url if we can
            Log.d(TAG, "valid length");
            mShareUrl = mDisplayUrl;
            broadcastUrl();
        } else {
            Log.d(TAG, "needs shortening");
            // Shorten the url if necessary
            UrlShortenerClient.ShortenUrlCallback urlSetter =
            new UrlShortenerClient.ShortenUrlCallback() {
                @Override
                public void onUrlShortened(String newUrl) {
                    Log.d(TAG, "shortening success");
                    mShareUrl = newUrl;
                    broadcastUrl();
                }
                @Override
                public void onError(String oldUrl) {
                    Toast.makeText(getApplicationContext(), getString(R.string.shorten_error),
                        Toast.LENGTH_LONG).show();
                }
            };
            UrlShortenerClient.getInstance(this).shortenUrl(mDisplayUrl, urlSetter, TAG);
        }
        return START_STICKY;
    }

    private void fetchBroadcastData(Intent intent){
        mStartedByRestart = intent == null;
        if (intent == null){
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            mDisplayUrl = sharedPrefs.getString(PREVIOUS_BROADCAST_URL_KEY, null);
            return;
        }
        mDisplayUrl = intent.getStringExtra(DISPLAY_URL_KEY);
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putString(PREVIOUS_BROADCAST_URL_KEY, mDisplayUrl)
            .commit();
    }

    private static boolean hasValidUrlLength(String url) {
        int uriLength = UriBeacon.uriLength(url);
        return 0 < uriLength && uriLength <= MAX_URI_LENGTH;
    }

    private boolean checkAndHandleAsciiUrl(String url) {
        boolean isCompliant = false;
        try {
          URI uri = new URI(url);
          String urlString = uri.toASCIIString();
          isCompliant = url.equals(urlString);
        } catch (URISyntaxException e) {
            Toast.makeText(this, getString(R.string.no_url_error), Toast.LENGTH_LONG).show();
        }
        return isCompliant;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "SERVICE onDestroy");
        unregisterReceiver(stopServiceReceiver);
        unregisterReceiver(mReceiver);
        disableUrlBroadcasting();
        super.onDestroy();
    }

    // Fires when user swipes away app from the recent apps list
    @Override
    public void onTaskRemoved (Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved");
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // The callbacks for the ble advertisement events
    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {

        // Fires when the URL is successfully being advertised
        @Override
        public void onStartSuccess(AdvertiseSettings advertiseSettings) {
            Log.d(TAG, "URL is broadcasting");
            createNotification();
            if (!mStartedByRestart) {
                Toast.makeText(getApplicationContext(), getString(R.string.url_broadcast),
                    Toast.LENGTH_LONG).show();
            }
        }

        // Fires when the URL could not be advertised
        @Override
        public void onStartFailure(int result) {
            Log.d(TAG, "onStartFailure" + result);
        }
    };



    /////////////////////////////////
    // utilities
    /////////////////////////////////

    // Broadcast via bluetooth the stored URL
    private void broadcastUrl() {
        Log.d(TAG, "broadcastUrl: " + mShareUrl);
        final AdvertiseData advertisementData = AdvertiseDataUtils.getAdvertisementData(mShareUrl);
        final AdvertiseSettings advertiseSettings = AdvertiseDataUtils.getAdvertiseSettings();
        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        mBluetoothLeAdvertiser.startAdvertising(advertiseSettings,
            advertisementData, mAdvertiseCallback);
    }

    // Turn off URL broadcasting
    public void disableUrlBroadcasting() {
        Log.d(TAG, "disableUrlBroadcasting");
        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        mNotificationManager.cancel(BROADCASTING_NOTIFICATION_ID);
    }



    // Surface a notification to the user that a URL is being broadcast
    // The notification specifies the URL being broadcast (the long URL)
    // and cannot be swiped away
    private void createNotification() {
                //.setPriority(NotificationCompat.PRIORITY_MIN);
        Intent resultIntent = new Intent();
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(BroadcastActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
            PendingIntent.FLAG_UPDATE_CURRENT);
        registerReceiver(stopServiceReceiver, new IntentFilter("myFilter"));
        PendingIntent pIntent = PendingIntent.getBroadcast(this, 0, new Intent("myFilter"),
            PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_leak_add_white_24dp)
                .setContentTitle(getString(R.string.broadcast_notif))
                .setContentText(mDisplayUrl)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                    getString(R.string.stop), pIntent);
        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(
            Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(BROADCASTING_NOTIFICATION_ID, mBuilder.build());
    }

    protected BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        Log.d(TAG, context.toString());
        stopSelf();
      }
    };

}
