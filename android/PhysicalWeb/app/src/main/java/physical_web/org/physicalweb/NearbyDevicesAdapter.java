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

package physical_web.org.physicalweb;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.uribeacon.beacon.UriBeacon;
import org.uribeacon.scan.compat.ScanResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * This class is responsible for
 * displaying the list of nearby devices.
 */

public class NearbyDevicesAdapter extends BaseAdapter {
  private static String TAG = "NearbyDevicesAdapter";
  private HashMap<String, Device> mDeviceAddressToDeviceMap;
  private List<Device> mSortedDevices;
  private Activity mActivity;

  NearbyDevicesAdapter(Activity activity) {
    mActivity = activity;
    mDeviceAddressToDeviceMap = new HashMap<>();
    mSortedDevices = null;
  }


  /////////////////////////////////
  // accessors
  /////////////////////////////////

  @Override
  public int getCount() {
    return mDeviceAddressToDeviceMap.size();
  }

  @Override
  public Device getItem(int position) {
    sortDevices();
    return mSortedDevices.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  /**
   * Build out the given view for the given position.
   * The view content is taken from the metadata
   * for the device at the given position in the list.
   *
   * @param position
   * @param view
   * @param viewGroup
   * @return
   */
  @Override
  public View getView(int position, View view, ViewGroup viewGroup) {
    // Get the list view item for the given position
    if (view == null) {
      view = mActivity.getLayoutInflater().inflate(R.layout.list_item_nearby_device, viewGroup, false);
    }
    // Get the device for the given position
    Device device = getItem(position);

    // Get the metadata for this device
    MetadataResolver.DeviceMetadata deviceMetadata = device.getMetadata();

    // If the metadata exists
    if (deviceMetadata != null) {
      // Set the title text
      TextView infoView = (TextView) view.findViewById(R.id.title);
      infoView.setText(deviceMetadata.title);

      // Set the site url text
      infoView = (TextView) view.findViewById(R.id.url);
      infoView.setText(deviceMetadata.siteUrl);

      // Set the description text
      infoView = (TextView) view.findViewById(R.id.description);
      infoView.setText(deviceMetadata.description);

      // Set the favicon image
      ImageView iconView = (ImageView) view.findViewById(R.id.icon);
      iconView.setImageBitmap(deviceMetadata.icon);

      // If the metadata does not exist
    } else {
      Log.i(TAG, String.format("Beacon with URL %s has no metadata.", device.getUriBeacon().getUriString()));
    }
    return view;
  }


  /////////////////////////////////
  // utilities
  /////////////////////////////////

  public Device handleFoundDevice(ScanResult scanResult) {
    Device newDevice = null;
    // Try to create a UriBeacon object using the scan record
    byte[] scanRecordBytes = scanResult.getScanRecord().getBytes();
    if (scanRecordBytes != null) {
      UriBeacon uriBeacon = UriBeacon.parseFromBytes(scanRecordBytes);
      // If this device is a beacon
      if (uriBeacon != null) {
        //Log.i(TAG, String.format("onLeScan: %s, RSSI: %d", scanResult.getDevice().getAddress(), scanResult.getRssi));
        // Try to get a stored nearby device that matches this device
        Device existingDevice = mDeviceAddressToDeviceMap.get(scanResult.getDevice().getAddress());
        // If no match was found (i.e. if this a newly discovered device)
        if (existingDevice == null) {
          // Create a new device sighting
          newDevice = new Device(uriBeacon, scanResult.getDevice(), scanResult.getRssi());
          // Add it to the stored map
          addDevice(newDevice);
        } else {
          // Update the time this device was last seen
          updateDevice(existingDevice, scanResult.getRssi());
        }
      }
    }
    return newDevice;
  }

  private void addDevice(final Device device) {
    mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mDeviceAddressToDeviceMap.put(device.getBluetoothDevice().getAddress(), device);
        notifyDataSetChanged();
      }
    });
  }

  private void updateDevice(final Device device, final int rssi) {
    mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        device.updateRssiHistory(rssi);
        notifyDataSetChanged();
      }
    });
  }

  public void handleLostDevice(ScanResult scanResult) {
    // Try to get a stored nearby device that matches this device
    Device existingDevice = mDeviceAddressToDeviceMap.get(scanResult.getDevice().getAddress());
    if (existingDevice != null) {
      removeDevice(existingDevice);
    }
  }

  public void removeDevice(final Device device) {
    mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        String bluetoothDeviceAddress = device.getBluetoothDevice().getAddress();
        mDeviceAddressToDeviceMap.remove(bluetoothDeviceAddress);
        notifyDataSetChanged();
      }
    });
  }

  @Override
  public void notifyDataSetChanged() {
    mSortedDevices = null;
    super.notifyDataSetChanged();
  }

  private void sortDevices() {
    if (mSortedDevices == null) {
      mSortedDevices = new ArrayList<>(mDeviceAddressToDeviceMap.values());
      Collections.sort(mSortedDevices, mRssiComparator);
    }
  }

  private Comparator<Device> mRssiComparator = new Comparator<Device>() {
    @Override
    public int compare(Device lhs, Device rhs) {
      return rhs.calculateAverageRssi() - lhs.calculateAverageRssi();
    }
  };
}