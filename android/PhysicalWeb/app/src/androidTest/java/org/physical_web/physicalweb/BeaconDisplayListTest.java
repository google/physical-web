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

import org.physical_web.physicalweb.PwoMetadata.UrlMetadata;

import android.test.AndroidTestCase;

/**
 * Tests for BeaconDisplayList.
 */
public class BeaconDisplayListTest extends AndroidTestCase {
  // sample data for constructing dummy beacon metadata
  private static final String URL = "http://physical-web.org";
  private static final String TITLE = "Physical Web";
  private static final String DEVICEADDRESS = "01:02:03:04:05:06";
  private static final String GROUPID = "groupid";
  private static final double RANK = 1000.0d;
  private static final long SCANMILLIS = 1000;
  private static final int RSSI = -100;
  private static final int TXPOWER = -20;
  private static final long PWSTRIPMILLIS = 2000;
  private static final String DESCRIPTION = "description";
  private static final String ICONURL = "iconUrl";

  // a second set of sample data for test cases that need two beacons
  private static final String URL2 = "http://example.com";
  private static final String TITLE2 = "Example Domain";
  private static final String DEVICEADDRESS2 = "0a:0b:0c:0d:0e:0f";
  private static final String GROUPID2 = "groupid2";
  private static final double RANK2 = 500.d;

  public BeaconDisplayListTest() {
    super();
  }

  private PwoMetadata createDummyDefaultPwo(String url) {
    return new PwoMetadata(url, SCANMILLIS);
  }

  private PwoMetadata createDummyBlePwo(String url) {
    PwoMetadata pwoMetadata = new PwoMetadata(url, SCANMILLIS);
    pwoMetadata.setBleMetadata(DEVICEADDRESS, RSSI, TXPOWER);
    return pwoMetadata;
  }

  private PwoMetadata createDummyBlePwoWithUrlMetadata(String url) {
    PwoMetadata pwoMetadata = new PwoMetadata(url, SCANMILLIS);
    pwoMetadata.setBleMetadata(DEVICEADDRESS, RSSI, TXPOWER);
    pwoMetadata.setUrlMetadata(createDummyUrlMetadata(url), PWSTRIPMILLIS);
    return pwoMetadata;
  }

  private UrlMetadata createDummyUrlMetadata(String url) {
    UrlMetadata urlMetadata = new UrlMetadata();
    urlMetadata.id = url;
    urlMetadata.siteUrl = url;
    urlMetadata.displayUrl = url;
    urlMetadata.title = TITLE;
    urlMetadata.description = DESCRIPTION;
    urlMetadata.iconUrl = ICONURL;
    urlMetadata.rank = RANK;
    urlMetadata.groupid = GROUPID;

    // no icon yet
    urlMetadata.icon = null;

    return urlMetadata;
  }

  public void testDisplayListAddAndRetrieveGenericPwo() throws Exception {
    BeaconDisplayList displayList = new BeaconDisplayList();
    assertTrue(displayList.size() == 0);
    assertNull(displayList.getItem(0));

    PwoMetadata pwoMetadata = createDummyDefaultPwo(URL);
    boolean isPublic = pwoMetadata.isPublic;

    displayList.addItem(pwoMetadata);
    assertTrue(displayList.size() == 1);

    PwoMetadata retrievedPwoMetadata = displayList.getItem(0);
    assertNotNull(retrievedPwoMetadata);
    assertEquals(retrievedPwoMetadata.url, URL);
    assertEquals(retrievedPwoMetadata.scanMillis, SCANMILLIS);
    assertEquals(retrievedPwoMetadata.isPublic, isPublic);
    assertFalse(retrievedPwoMetadata.hasBleMetadata());
    assertFalse(retrievedPwoMetadata.hasUrlMetadata());
  }

  public void testDisplayListAddAndRetrieveBlePwo() throws Exception {
    BeaconDisplayList displayList = new BeaconDisplayList();
    assertTrue(displayList.size() == 0);
    assertNull(displayList.getItem(0));

    PwoMetadata pwoMetadata = createDummyBlePwo(URL);
    boolean isPublic = pwoMetadata.isPublic;

    displayList.addItem(pwoMetadata);
    assertTrue(displayList.size() == 1);

    PwoMetadata retrievedPwoMetadata = displayList.getItem(0);
    assertNotNull(retrievedPwoMetadata);
    assertEquals(retrievedPwoMetadata.url, URL);
    assertEquals(retrievedPwoMetadata.scanMillis, SCANMILLIS);
    assertEquals(retrievedPwoMetadata.isPublic, isPublic);
    assertTrue(retrievedPwoMetadata.hasBleMetadata());
    assertEquals(retrievedPwoMetadata.bleMetadata.deviceAddress, DEVICEADDRESS);
    assertEquals(retrievedPwoMetadata.bleMetadata.rssi, RSSI);
    assertEquals(retrievedPwoMetadata.bleMetadata.txPower, TXPOWER);
    assertFalse(retrievedPwoMetadata.hasUrlMetadata());
  }

  public void testDisplayListAddAndRetrieveBlePwoWithUrlMetadata() throws Exception {
    BeaconDisplayList displayList = new BeaconDisplayList();
    assertTrue(displayList.size() == 0);
    assertNull(displayList.getItem(0));

    PwoMetadata pwoMetadata = createDummyBlePwoWithUrlMetadata(URL);
    boolean isPublic = pwoMetadata.isPublic;

    displayList.addItem(pwoMetadata);
    assertTrue(displayList.size() == 1);

    PwoMetadata retrievedPwoMetadata = displayList.getItem(0);
    assertNotNull(retrievedPwoMetadata);
    assertEquals(retrievedPwoMetadata.url, URL);
    assertEquals(retrievedPwoMetadata.scanMillis, SCANMILLIS);
    assertEquals(retrievedPwoMetadata.isPublic, isPublic);
    assertTrue(retrievedPwoMetadata.hasBleMetadata());
    assertEquals(retrievedPwoMetadata.bleMetadata.deviceAddress, DEVICEADDRESS);
    assertEquals(retrievedPwoMetadata.bleMetadata.rssi, RSSI);
    assertEquals(retrievedPwoMetadata.bleMetadata.txPower, TXPOWER);
    assertTrue(retrievedPwoMetadata.hasUrlMetadata());
    assertEquals(retrievedPwoMetadata.urlMetadata.id, URL);
    assertEquals(retrievedPwoMetadata.urlMetadata.siteUrl, URL);
    assertEquals(retrievedPwoMetadata.urlMetadata.displayUrl, URL);
    assertEquals(retrievedPwoMetadata.urlMetadata.title, TITLE);
    assertEquals(retrievedPwoMetadata.urlMetadata.description, DESCRIPTION);
    assertEquals(retrievedPwoMetadata.urlMetadata.iconUrl, ICONURL);
    assertNull(retrievedPwoMetadata.urlMetadata.icon);
    assertEquals(retrievedPwoMetadata.urlMetadata.rank, RANK);
    assertEquals(retrievedPwoMetadata.urlMetadata.groupid, GROUPID);
  }

  public void testDisplayListAddPwoAndClear() throws Exception {
    BeaconDisplayList displayList = new BeaconDisplayList();
    assertTrue(displayList.size() == 0);
    assertNull(displayList.getItem(0));

    displayList.addItem(new PwoMetadata(URL, SCANMILLIS));
    assertTrue(displayList.size() == 1);
    assertNotNull(displayList.getItem(0));

    displayList.clear();
    assertTrue(displayList.size() == 0);
    assertNull(displayList.getItem(0));
  }

  public void testDisplayListAddTwoDefaultPwos() throws Exception {
    BeaconDisplayList displayList = new BeaconDisplayList();

    // add two PWOs with different URLs
    PwoMetadata deviceA = createDummyDefaultPwo(URL);
    PwoMetadata deviceB = createDummyDefaultPwo(URL2);
    displayList.addItem(deviceA);
    displayList.addItem(deviceB);

    // there should be two items
    assertTrue(displayList.size() == 2);
  }

  public void testDisplayListAddTwoBlePwos() throws Exception {
    BeaconDisplayList displayList = new BeaconDisplayList();

    // add two BLE PWOs with different URLs
    PwoMetadata deviceA = createDummyBlePwo(URL);
    PwoMetadata deviceB = createDummyBlePwo(URL2);
    deviceB.setBleMetadata(DEVICEADDRESS2, RSSI, TXPOWER);
    displayList.addItem(deviceA);
    displayList.addItem(deviceB);

    // there should be two items
    assertTrue(displayList.size() == 2);
  }

  public void testDisplayListAddTwoBlePwosWithUrlMetadata() throws Exception {
    BeaconDisplayList displayList = new BeaconDisplayList();

    // add two BLE PWOs with different URLs and URL metadata
    PwoMetadata deviceA = createDummyBlePwoWithUrlMetadata(URL);
    PwoMetadata deviceB = createDummyBlePwoWithUrlMetadata(URL2);
    deviceB.setBleMetadata(DEVICEADDRESS2, RSSI, TXPOWER);
    deviceB.urlMetadata.groupid = GROUPID2;
    displayList.addItem(deviceA);
    displayList.addItem(deviceB);

    // there should be two items
    assertTrue(displayList.size() == 2);
  }

  public void testDisplayListAddDuplicateDefaultPwo() throws Exception {
    BeaconDisplayList displayList = new BeaconDisplayList();

    // add the same PWO twice
    PwoMetadata device = createDummyDefaultPwo(URL);
    displayList.addItem(device);
    displayList.addItem(device);

    // there should only be one item
    assertTrue(displayList.size() == 1);
  }

  public void testDisplayListAddDuplicateBlePwo() throws Exception {
    BeaconDisplayList displayList = new BeaconDisplayList();

    // add the same BLE PWO twice
    PwoMetadata device = createDummyBlePwo(URL);
    displayList.addItem(device);
    displayList.addItem(device);

    // there should only be one item
    assertTrue(displayList.size() == 1);
  }

  public void testDisplayListAddDuplicateBlePwoWithUrlMetadata() throws Exception {
    BeaconDisplayList displayList = new BeaconDisplayList();

    // add the same BLE PWO (with URL metadata) twice
    PwoMetadata device = createDummyBlePwoWithUrlMetadata(URL);
    displayList.addItem(device);
    displayList.addItem(device);

    // there should only be one item
    assertTrue(displayList.size() == 1);
  }

  public void testDisplayListSameUrlDifferentDeviceAddress() throws Exception {
    BeaconDisplayList displayList = new BeaconDisplayList();

    // add two BLE PWOs with the same URL that only differ by deviceAddress
    // (simulates two beacons advertising the same URL)
    PwoMetadata deviceA = createDummyBlePwo(URL);
    PwoMetadata deviceB = createDummyBlePwo(URL);
    deviceB.setBleMetadata(DEVICEADDRESS2, RSSI, TXPOWER);

    displayList.addItem(deviceA);
    displayList.addItem(deviceB);

    // both items should be displayed
    assertTrue(displayList.size() == 2);
  }

  public void testDisplayListAddDefaultPwoThenUrlMetadata() throws Exception {
    BeaconDisplayList displayList = new BeaconDisplayList();

    // add a PWO, then add the URL metadata for that PWO (simulate a PWS response)
    PwoMetadata device = createDummyDefaultPwo(URL);
    PwoMetadata deviceWithUrlMetadata = createDummyDefaultPwo(URL);
    deviceWithUrlMetadata.setUrlMetadata(createDummyUrlMetadata(URL), PWSTRIPMILLIS);

    displayList.addItem(device);

    // verify that the item has no URL metadata
    assertTrue(displayList.size() == 1);
    assertFalse(displayList.getItem(0).hasUrlMetadata());

    displayList.addItem(deviceWithUrlMetadata);

    // verify that we still only have one item and it now has URL metadata
    assertTrue(displayList.size() == 1);
    assertTrue(displayList.getItem(0).hasUrlMetadata());
  }

  public void testDisplayListAddBlePwoThenUrlMetadata() throws Exception {
    BeaconDisplayList displayList = new BeaconDisplayList();

    // add a PWO, then add the URL metadata for that PWO (simulate a PWS response)
    PwoMetadata device = createDummyBlePwo(URL);
    PwoMetadata deviceWithUrlMetadata = createDummyBlePwoWithUrlMetadata(URL);

    displayList.addItem(device);

    // verify that the item has no URL metadata
    assertTrue(displayList.size() == 1);
    assertFalse(displayList.getItem(0).hasUrlMetadata());

    displayList.addItem(deviceWithUrlMetadata);

    // verify that we still only have one item and it now has URL metadata
    assertTrue(displayList.size() == 1);
    assertTrue(displayList.getItem(0).hasUrlMetadata());
  }

  public void testDisplayListUpdateRank() throws Exception {
    BeaconDisplayList displayList = new BeaconDisplayList();

    // add two PwoMetadatas that differ only by rank
    // (simulates a second PWS request for the same PWO at different range)
    PwoMetadata device = createDummyBlePwoWithUrlMetadata(URL);
    PwoMetadata deviceUpdated = createDummyBlePwoWithUrlMetadata(URL);
    deviceUpdated.urlMetadata.rank = RANK2;

    displayList.addItem(device);

    // verify original rank
    assertTrue(displayList.size() == 1);
    assertEquals(displayList.getItem(0).urlMetadata.rank, RANK);

    displayList.addItem(deviceUpdated);

    // verify updated rank
    assertTrue(displayList.size() == 1);
    assertEquals(displayList.getItem(0).urlMetadata.rank, RANK2);
  }

  public void testDisplayListUpdateTitleAndGroupid() throws Exception {
    BeaconDisplayList displayList = new BeaconDisplayList();

    // add two PwoMetadatas that differ only by title and groupid
    // (simulates updating the cached PWS site info after the page title is updated)
    PwoMetadata device = createDummyBlePwoWithUrlMetadata(URL);
    PwoMetadata deviceUpdated = createDummyBlePwoWithUrlMetadata(URL);
    deviceUpdated.urlMetadata.title = TITLE2;
    deviceUpdated.urlMetadata.groupid = GROUPID2;

    displayList.addItem(device);

    // verify original title and groupid
    assertTrue(displayList.size() == 1);
    assertEquals(displayList.getItem(0).urlMetadata.title, TITLE);
    assertEquals(displayList.getItem(0).urlMetadata.groupid, GROUPID);

    displayList.addItem(deviceUpdated);

    // verify updated title and groupid
    assertTrue(displayList.size() == 1);
    assertEquals(displayList.getItem(0).urlMetadata.title, TITLE2);
    assertEquals(displayList.getItem(0).urlMetadata.groupid, GROUPID2);
  }
}
