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
import android.webkit.URLUtil;

import com.youview.tinydnssd.DiscoverResolver;
import com.youview.tinydnssd.MDNSDiscover;

import java.util.Map;


class MdnsUrlDeviceDiscoverer extends UrlDeviceDiscoverer {
  private static final String TAG = MdnsUrlDeviceDiscoverer.class.getSimpleName();
  private DiscoverResolver mResolver;
  private static final String MDNS_SERVICE_TYPE = "_physicalweb._tcp";
  private enum State {
    STOPPED,
    WAITING,
    STARTED,
  }
  private State mState;

  public MdnsUrlDeviceDiscoverer(Context context) {
    mState = State.STOPPED;
    mResolver = new DiscoverResolver(context, MDNS_SERVICE_TYPE,
        new DiscoverResolver.Listener() {
      @Override
      public void onServicesChanged(Map<String, MDNSDiscover.Result> services) {
        for (MDNSDiscover.Result result : services.values()) {
          // access the Bluetooth MAC from the TXT record
          String url = result.txt.dict.get("url");
          Log.d(TAG, url);
          String id = TAG + result.srv.fqdn + result.srv.port;
          String title = "";
          String description = "";
          if ("false".equals(result.txt.dict.get("public"))) {
            if (result.txt.dict.containsKey("title")) {
              title = result.txt.dict.get("title");
            }
            if (result.txt.dict.containsKey("description")) {
              description = result.txt.dict.get("description");
            }
            reportUrlDevice(createUrlDeviceBuilder(id, url)
              .setPrivate()
              .setTitle(title)
              .setDescription(description)
              .setDeviceType(Utils.MDNS_LOCAL_DEVICE_TYPE)
              .build());
          } else if (URLUtil.isNetworkUrl(url)) {
            reportUrlDevice(createUrlDeviceBuilder(id, url)
              .setPrivate()
              .setDeviceType(Utils.MDNS_PUBLIC_DEVICE_TYPE)
              .build());
          }
        }
      }
    });
  }

  @Override
  public synchronized void startScanImpl() {
    if (mState != State.STOPPED) {
      return;
    }
    mState = State.WAITING;
    mResolver.start();
  }

  @Override
  public synchronized void stopScanImpl() {
    if (mState != State.STARTED) {
      return;
    }
    mState = State.WAITING;
    mResolver.stop();
  }
}
