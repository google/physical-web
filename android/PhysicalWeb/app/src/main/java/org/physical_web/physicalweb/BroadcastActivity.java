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
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * This is the main entry point for the broadcasting service.
 * Handles the share intent.
 */
public class BroadcastActivity extends Activity {

    private static final String TAG  = BroadcastActivity.class.getSimpleName();
    private static final int ENABLE_BLUETOOTH_REQUEST_ID = 1;
    public static final int MAX_URI_LENGTH = 18;
    private final BroadcastReceiver serverStartReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        finish();
      }
    };

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        if (requestCode == ENABLE_BLUETOOTH_REQUEST_ID && resultCode == -1) {
            checkBleAndStart();
        } else {
            Toast.makeText(this, getString(R.string.bt_on), Toast.LENGTH_LONG).show();
        }
        finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver(serverStartReceiver, new IntentFilter("server"));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(serverStartReceiver);
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        String type = intent.getType();
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (type.equals("text/plain")) {
            if (Build.VERSION.SDK_INT < 21) {
                Toast.makeText(this, getString(R.string.ble_os_error),
                    Toast.LENGTH_LONG).show();
                return;
            }
            if (!checkIfBluetoothIsEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_ID);
            } else {
                checkBleAndStart();
            }
        } else if (type.startsWith("image/") || type.startsWith("text/html") ||
            type.startsWith("video") || type.startsWith("audio")) {
            Log.d(TAG, type);
            Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            Log.d(TAG, fileUri.toString());
            startFileBroadcastService(fileUri.toString(), type);
        }
    }

    private void checkBleAndStart() {
        if (hasBleAdvertiseCapability()) {
            parseAndHandleUrl();
        } else {
            Toast.makeText(this, getString(R.string.ble_error),
                Toast.LENGTH_LONG).show();
        }
        finish();
    }

    // Check if bluetooth is on
    private boolean checkIfBluetoothIsEnabled() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(
            Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
          Log.d(TAG, "not enabled");
            return false;
        }
        Log.d(TAG, "enabled");
        return true;
    }

    // Check if the given bluetooth hardware
    // on the current device supports ble advertisemetns
    @TargetApi(21)
    private boolean hasBleAdvertiseCapability() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(
            Context.BLUETOOTH_SERVICE);
        if (bluetoothManager.getAdapter().getBluetoothLeAdvertiser() == null) {
            Log.d(TAG, "cant advertise");
            return false;
        }
        Log.d(TAG, "can advertise");
        return true;
    }

    private void parseAndHandleUrl() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        // Look to see if the text contains a URL
        String url = findUrlInText(text);
        // If the text is a URL
        if (url != null) {
            startBroadcastService(url);
        } else {
            Toast.makeText(this, getString(R.string.no_url_error),
                Toast.LENGTH_LONG).show();
        }
    }

    private void startBroadcastService(String url) {
        Intent intent = new Intent(this, PhysicalWebBroadcastService.class);
        intent.putExtra(PhysicalWebBroadcastService.DISPLAY_URL_KEY, url);
        startService(intent);
    }

    private void startFileBroadcastService(String uri, String type) {
        Intent intent = new Intent(this, FileBroadcastService.class);
        intent.putExtra(FileBroadcastService.FILE_KEY, uri);
        intent.putExtra(FileBroadcastService.MIME_TYPE_KEY, type);
        startService(intent);
    }

    // Check if the given text contains a URL
    private String findUrlInText(String text) {
        List<String> urls = new ArrayList<>();
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            String url = m.group();
            urls.add(url);
        }
        if (urls.size() > 0) {
            return urls.get(0);
        }
        return null;
    }
}
