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
package org.physical_web.demos;

import org.physical_web.physicalweb.FatBeaconBroadcastService;
import org.physical_web.physicalweb.R;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

/**
 * Hello world demo for FatBeacon.
 */
public class FatBeaconHelloWorld implements Demo {
  private static final String TAG = FatBeaconHelloWorld.class.getSimpleName();
  private static boolean mIsDemoStarted = false;
  private Context mContext;

  public FatBeaconHelloWorld(Context context) {
    mContext = context;
  }

  @Override
  public String getSummary() {
    return mContext.getString(R.string.fat_beacon_demo_summary);
  }

  @Override
  public String getTitle() {
    return mContext.getString(R.string.fat_beacon_demo_title);
  }

  @Override
  public boolean isDemoStarted() {
    return mIsDemoStarted;
  }

  @Override
  public void startDemo() {
    Intent intent = new Intent(mContext, FatBeaconBroadcastService.class);
    intent.putExtra(FatBeaconBroadcastService.TITLE_KEY, "Hello World");
    String uriString = ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
        mContext.getPackageName() + "/" + R.raw.fatbeacon_default_webpage;
    intent.putExtra(FatBeaconBroadcastService.URI_KEY, uriString);
    mContext.startService(intent);
    mIsDemoStarted = true;
  }

  @Override
  public void stopDemo() {
    mContext.stopService(new Intent(mContext, FatBeaconBroadcastService.class));
    mIsDemoStarted = false;
  }
}
