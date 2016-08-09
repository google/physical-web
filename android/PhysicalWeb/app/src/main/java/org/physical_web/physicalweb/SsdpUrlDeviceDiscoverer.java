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

import org.physical_web.physicalweb.ssdp.Ssdp;
import org.physical_web.physicalweb.ssdp.SsdpMessage;

import android.content.Context;

import java.io.IOException;

/**
 * This class discovers Physical Web URI/URLs over SSDP.
 */
public class SsdpUrlDeviceDiscoverer extends UrlDeviceDiscoverer implements Ssdp.SsdpCallback {
  private static final String TAG = SsdpUrlDeviceDiscoverer.class.getSimpleName();
  private static final String PHYSICAL_WEB_SSDP_TYPE = "urn:physical-web-org:device:Basic:1";
  private Context mContext;
  private Thread mThread;
  private Ssdp mSsdp;

  public SsdpUrlDeviceDiscoverer(Context context) {
    mContext = context;
  }

  public void startScanImpl() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          // TODO(?): set timeout using getSsdp().start(NearbyBeaconsFragment.SCAN_TIME_MILLIS)
          // to ensure that SSDP scan thread is stopped automatically after timeout.
          // In this case there is no need to call stop().
          getSsdp().start(null);
          Thread.sleep(200);
          getSsdp().search(PHYSICAL_WEB_SSDP_TYPE);
        } catch (Exception e) {
          Log.e(TAG, e.getMessage(), e);
        }
      }
    }).start();
  }

  public void stopScanImpl() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          getSsdp().stop();
        } catch (IOException e) {
          Log.e(TAG, e.getMessage(), e);
        }
      }
    }).start();
  }

  public synchronized Ssdp getSsdp() throws IOException {
    if (mSsdp == null) {
      mSsdp = new Ssdp(this);
    }
    return mSsdp;
  }

  @Override
  public void onSsdpMessageReceived(SsdpMessage ssdpMessage) {
    final String url = ssdpMessage.get("LOCATION");
    final String st = ssdpMessage.get("ST");
    if (url != null && PHYSICAL_WEB_SSDP_TYPE.equals(st)) {
      Log.d(TAG, "SSDP url received: " + url);
      new Thread(new Runnable() {
        @Override
        public void run() {
          reportUrlDevice(createUrlDeviceBuilder(TAG + url, url)
              .setDeviceType(Utils.SSDP_DEVICE_TYPE)
              .build());
        }
      }).start();
    }
  }
}
