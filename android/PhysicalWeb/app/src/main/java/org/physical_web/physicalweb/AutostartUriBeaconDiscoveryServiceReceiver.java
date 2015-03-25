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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * This receiver starts the UriBeaconDiscoveryService
 */
public class AutostartUriBeaconDiscoveryServiceReceiver extends BroadcastReceiver {
  public void onReceive(Context context, Intent intent) {
    String preferences_key = context.getString(R.string.physical_web_preference_file_name);
    SharedPreferences sharedPreferences =
        context.getSharedPreferences(preferences_key, Context.MODE_PRIVATE);
    if (sharedPreferences.getBoolean(context.getString(R.string.user_opted_in_flag), false)) {
      Intent newIntent = new Intent(context, UriBeaconDiscoveryService.class);
      context.startService(newIntent);
    }
  }
}
