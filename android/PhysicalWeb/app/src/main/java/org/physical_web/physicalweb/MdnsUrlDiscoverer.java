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

class MdnsUrlDiscoverer {

  private static final String TAG = "MdnsUrlDiscoverer";
  NsdManager.DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener() {

    @Override
    public void onDiscoveryStarted(String regType) {
      Log.d(TAG, "Service discovery started");
    }

    @Override
    public void onServiceFound(NsdServiceInfo service) {
      Log.d(TAG, "Service discovery success" + service);
      String name = service.getServiceName();
      if (URLUtil.isNetworkUrl(name)) {
        mMdnsUrlDiscovererCallback.onMdnsUrlFound(name);
      }
    }

    @Override
    public void onServiceLost(NsdServiceInfo service) {
      Log.e(TAG, "service lost" + service);
    }

    @Override
    public void onDiscoveryStopped(String serviceType) {
      Log.i(TAG, "Discovery stopped: " + serviceType);
    }

    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
      Log.e(TAG, "Discovery failed: Error code:" + errorCode);
      mNsdManager.stopServiceDiscovery(this);
    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
      Log.e(TAG, "Discovery failed: Error code:" + errorCode);
      mNsdManager.stopServiceDiscovery(this);
    }
  };
  private static final String MDNS_SERVICE_TYPE = "_http._tcp.";
  private NsdManager mNsdManager;
  private MdnsUrlDiscovererCallback mMdnsUrlDiscovererCallback;

  public MdnsUrlDiscoverer(Context context, MdnsUrlDiscovererCallback mdnsUrlDiscovererCallback) {
    mMdnsUrlDiscovererCallback = mdnsUrlDiscovererCallback;
    mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
  }

  public void startScanning() {
    mNsdManager.discoverServices(MDNS_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
  }

  public void stopScanning() {
    mNsdManager.stopServiceDiscovery(mDiscoveryListener);
  }

  public interface MdnsUrlDiscovererCallback {
    public void onMdnsUrlFound(String url);
  }
}