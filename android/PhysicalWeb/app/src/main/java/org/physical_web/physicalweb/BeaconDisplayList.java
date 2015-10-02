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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

class BeaconDisplayList {

  /**
   * An entry in the beacon display list.
   */
  private interface BeaconDisplayItem {
    PwoMetadata top();
  }

  /**
   * Container for all metadata related to a particular physical web object (PWO) device.
   */
  private class PwoDevice implements BeaconDisplayItem {
    private PwoMetadata mPwoMetadata;

    PwoDevice() {
      mPwoMetadata = null;
    }

    /**
     * Append new metadata for this PWO.
     * @param pwoMetadata Updated metadata for this PWO
     */
    public void addItem(PwoMetadata pwoMetadata) {
      // TODO(mattreynolds): track PWO history (for now, just keep the most recent)
      mPwoMetadata = pwoMetadata;
    }

    /**
     * Returns the most relevant metadata for this PWO.
     * @return PwoMetadata
     */
    @Override
    public PwoMetadata top() {
      return mPwoMetadata;
    }
  }

  /**
   * Container for all PWOs in the same group.
   */
  private class PwoGroup implements BeaconDisplayItem {
    private HashMap<String, PwoDevice> mPwoDevices;

    PwoGroup() {
      mPwoDevices = new HashMap<>();
    }

    /**
     * Append new metadata for a PWO in this group, creating a new PwoDevice if needed.
     * @param pwoMetadata New PwoMetadata
     */
    public void addItem(PwoMetadata pwoMetadata) {
      String groupedPwoId = getPwoId(pwoMetadata);

      PwoDevice pwoDevice = mPwoDevices.get(groupedPwoId);
      boolean isNewDevice = false;
      if (pwoDevice == null) {
        pwoDevice = new PwoDevice();
        isNewDevice = true;
      }
      pwoDevice.addItem(pwoMetadata);

      if (isNewDevice) {
        mPwoDevices.put(groupedPwoId, pwoDevice);
      }
    }

    /**
     * Remove a PWO from this group.
     * @param pwoMetadata The PwoDevice to remove
     */
    public void removeItem(PwoMetadata pwoMetadata) {
      mPwoDevices.remove(getPwoId(pwoMetadata));
    }

    /**
     * Return true if the specified PWO is in this group.
     * @param pwoMetadata The PwoDevice
     * @return boolean true if present
     */
    public boolean contains(PwoMetadata pwoMetadata) {
      return mPwoDevices.containsKey(getPwoId(pwoMetadata));
    }

    /**
     * Promote an existing ungrouped PWO device into this PWO group.
     * @param pwoDevice The PwoDevice to promote
     */
    public void promotePwo(PwoDevice pwoDevice) {
      PwoMetadata topPwoMetadata = pwoDevice.top();
      String groupedPwoId = getPwoId(topPwoMetadata);

      // only promote if the PWO doesn't already exist
      if (groupedPwoId != null && !mPwoDevices.containsKey(groupedPwoId)) {
        mPwoDevices.put(groupedPwoId, pwoDevice);
      }
    }

    /**
     * Returns metadata for the top PWO device in this group.
     * @return PwoMetadata
     */
    @Override
    public PwoMetadata top() {
      PriorityQueue<PwoMetadata> groupedPwos = new PriorityQueue<>();
      for (PwoDevice pwoDevice : mPwoDevices.values()) {
        groupedPwos.add(pwoDevice.top());
      }
      return groupedPwos.peek();
    }
  }

  private List<BeaconDisplayItem> mDisplayList;
  private HashMap<String, PwoGroup> mPwoGroups;
  private HashMap<String, PwoDevice> mUngroupedPwoDevices;

  BeaconDisplayList() {
    mDisplayList = new ArrayList<>();
    mPwoGroups = new HashMap<>();
    mUngroupedPwoDevices = new HashMap<>();
  }

  /**
   * Append new PWO metadata and update the list of displayed beacons.
   * @param pwoMetadata New PwoMetadata
   */
  public void addItem(PwoMetadata pwoMetadata) {
    // PWOs in the same group are intended to represent the same result, but with minimally
    // different URLs (e.g. a differing hash variable). To avoid confusing users with many
    // similar results, we instead display one grouped result that links to the URL of the
    // top-ranked PWO in the group.

    String groupid = pwoMetadata.getGroupid();
    if (groupid == null) {
      updateUngroupedPwo(pwoMetadata);
    } else {
      // Check if this metadata matches an ungrouped PWO. If so, promote it to a grouped PWO.
      String ungroupedPwoId = getPwoId(pwoMetadata);
      PwoDevice ungroupedPwoDevice = mUngroupedPwoDevices.get(ungroupedPwoId);
      if (ungroupedPwoDevice != null) {
        promoteUngroupedPwo(groupid, ungroupedPwoDevice);
      }

      updateGroupedPwo(groupid, pwoMetadata);
    }
  }

  /**
   * Return metadata for the PWO at the specified index in the display list.
   * @param i Index into display list
   * @return PwoMetadata
   */
  public PwoMetadata getItem(int i) {
    if (i >= 0 && i < mDisplayList.size()) {
      return mDisplayList.get(i).top();
    }

    return null;
  }

  /**
   * Return the display list item count.
   * @return Count of displayed items
   */
  public int size() {
    return mDisplayList.size();
  }

  /**
   * Clear the display list and forget all cached metadata.
   */
  public void clear() {
    mDisplayList.clear();
    mPwoGroups.clear();
    mUngroupedPwoDevices.clear();
  }

  /**
   * Update metadata for a grouped PWO, creating a new PWO group if needed.
   * @param groupid String identifier for the PWO group
   * @param pwoMetadata The grouped PWO metadata
   */
  private void updateGroupedPwo(String groupid, PwoMetadata pwoMetadata) {
    // Is this device already in a group?  If so, remove it first
    PwoGroup previousPwoGroup = null;
    String previousPwoGroupid = null;
    for (Map.Entry<String, PwoGroup> entry : mPwoGroups.entrySet()) {
      PwoGroup candidateGroup = entry.getValue();
      if (candidateGroup.contains(pwoMetadata)) {
        previousPwoGroup = candidateGroup;
        previousPwoGroupid = entry.getKey();
        break;
      }
    }
    if (previousPwoGroup != null && !groupid.equals(previousPwoGroupid)) {
      // Remove the device from the group
      previousPwoGroup.removeItem(pwoMetadata);

      // Also remove the group if it's empty
      if (previousPwoGroup.top() == null) {
        mDisplayList.remove(previousPwoGroup);
        mPwoGroups.remove(previousPwoGroupid);
      }
    }

    PwoGroup pwoGroup = mPwoGroups.get(groupid);

    boolean isNewGroup = false;
    if (pwoGroup == null) {
      pwoGroup = new PwoGroup();
      mPwoGroups.put(groupid, pwoGroup);
      isNewGroup = true;
    }
    pwoGroup.addItem(pwoMetadata);

    // Avoid adding items to the display list until they are populated with PwoMetadata
    if (isNewGroup) {
      mDisplayList.add(pwoGroup);
    }
  }

  /**
   * Update metadata for an ungrouped PWO, creating a new PWO device if needed.
   * @param pwoMetadata The ungrouped PWO metadata
   */
  private void updateUngroupedPwo(PwoMetadata pwoMetadata) {
    String ungroupedPwoId = getPwoId(pwoMetadata);
    PwoDevice ungroupedPwoDevice = mUngroupedPwoDevices.get(ungroupedPwoId);

    boolean isNewDevice = false;
    if (ungroupedPwoDevice == null) {
      ungroupedPwoDevice = new PwoDevice();
      isNewDevice = true;
    }
    ungroupedPwoDevice.addItem(pwoMetadata);

    // Avoid adding items to the display list until they are populated with PwoMetadata
    if (isNewDevice) {
      mUngroupedPwoDevices.put(ungroupedPwoId, ungroupedPwoDevice);
      mDisplayList.add(ungroupedPwoDevice);
    }
  }

  /**
   * Create a new PWO group and promote an ungrouped PWO as the initial PWO in the group.
   * @param groupid String identifier for the new PWO group
   * @param ungroupedPwo The PWO to promote
   */
  private void promoteUngroupedPwo(String groupid, PwoDevice ungroupedPwo) {
    String ungroupedPwoId = getPwoId(ungroupedPwo.top());

    mDisplayList.remove(ungroupedPwo);
    mUngroupedPwoDevices.remove(ungroupedPwoId);

    PwoGroup pwoGroup = new PwoGroup();
    pwoGroup.promotePwo(ungroupedPwo);
    mPwoGroups.put(groupid, pwoGroup);
    mDisplayList.add(pwoGroup);
  }

  private static String getPwoId(PwoMetadata pwoMetadata) {
    // Prefer an id that uniquely identifies the device, but use URL as a fallback for PWOs
    // that do not have BLE metadata or otherwise don't support a unique device id.
    String deviceAddress = pwoMetadata.getDeviceAddress();
    if (deviceAddress != null && !deviceAddress.equals("")) {
      return "ble:" + deviceAddress;
    }
    return "url:" + pwoMetadata.url;
  }
}
