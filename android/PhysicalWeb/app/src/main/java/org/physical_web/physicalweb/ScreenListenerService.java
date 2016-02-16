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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

/**
 * This is a service registers a broadcast receiver to listen for screen on/off events.
 * It is a very unfortunate service that must exist because we can't register for screen on/off
 * in the manifest.
 */

public class ScreenListenerService extends Service {
  private BroadcastReceiver mScreenStateBroadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      Intent discoveryIntent = new Intent(context, UrlDeviceDiscoveryService.class);
      if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
        context.startService(discoveryIntent);
      } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
        context.stopService(discoveryIntent);
      }
    }
  };

  @Override
  public void onCreate() {
    super.onCreate();
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(Intent.ACTION_SCREEN_ON);
    intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
    registerReceiver(mScreenStateBroadcastReceiver, intentFilter);
  }

  @Override
  public void onDestroy() {
    unregisterReceiver(mScreenStateBroadcastReceiver);
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent intent) {
    // Nothing should bind to this service
    return null;
  }
}
