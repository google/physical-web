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
package org.physical_web.collection;

import static org.junit.Assert.*;

import org.json.JSONObject;

import org.junit.Before;
import org.junit.Test;

import org.skyscreamer.jsonassert.JSONAssert;

import java.util.List;

/**
 * PhysicalWebCollection unit test class.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class PhysicalWebCollectionTest {
  private static final String ID1 = "id1";
  private static final String ID2 = "id2";
  private static final String ID3 = "id3";
  private static final String ID4 = "id4";
  private static final String ID5 = "id5";
  private static final String URL1 = "http://example.com";
  private static final String URL2 = "http://physical-web.org";
  private static final String URL3a = "http://example.com/#a";
  private static final String URL3b = "http://example.com/#b";
  private static final String TITLE1 = "title1";
  private static final String TITLE2 = "title2";
  private static final String DESCRIPTION1 = "description1";
  private static final String DESCRIPTION2 = "description2";
  private static final String ICON_URL1 = "http://example.com/favicon.ico";
  private static final String ICON_URL2 = "http://physical-web.org/favicon.ico";
  private static final String GROUP_ID1 = "group1";
  private static final String GROUP_ID2 = "group2";
  private PhysicalWebCollection physicalWebCollection1;
  private JSONObject jsonObject1;

  private void addRankedDeviceAndMetadata(PhysicalWebCollection collection, String id, String url,
                                          String groupId, double rank) {
    PwPair rankedPair = RankedDevice.createRankedPair(id, url, groupId, rank);
    collection.addUrlDevice(rankedPair.getUrlDevice());
    collection.addMetadata(rankedPair.getPwsResult());
  }

  @Before
  public void setUp() {
    physicalWebCollection1 = new PhysicalWebCollection();
    UrlDevice urlDevice = new UrlDevice(ID1, URL1);
    PwsResult pwsResult = new PwsResult.Builder(URL1, URL1)
        .setTitle(TITLE1)
        .setDescription(DESCRIPTION1)
        .setIconUrl(ICON_URL1)
        .setGroupId(GROUP_ID1)
        .build();
    physicalWebCollection1.addUrlDevice(urlDevice);
    physicalWebCollection1.addMetadata(pwsResult);
    jsonObject1 = new JSONObject("{"
        + "    \"schema\": 1,"
        + "    \"devices\": [{"
        + "        \"id\": \"" + ID1 + "\","
        + "        \"url\": \"" + URL1 + "\""
        + "    }],"
        + "    \"metadata\": [{"
        + "        \"requesturl\": \"" + URL1 + "\","
        + "        \"siteurl\": \"" + URL1 + "\","
        + "        \"title\": \"" + TITLE1 + "\","
        + "        \"description\": \"" + DESCRIPTION1 + "\","
        + "        \"iconurl\": \"" + ICON_URL1 + "\","
        + "        \"groupid\": \"" + GROUP_ID1 + "\""
        + "    }]"
        + "}");
  }

  @Test
  public void getUrlDeviceByIdReturnsFoundUrlDevice() {
    UrlDevice urlDevice = physicalWebCollection1.getUrlDeviceById(ID1);
    assertEquals(urlDevice.getId(), ID1);
    assertEquals(urlDevice.getUrl(), URL1);
  }

  @Test
  public void getUrlDeviceByIdReturnsNullForMissingUrlDevice() {
    UrlDevice fetchedUrlDevice = physicalWebCollection1.getUrlDeviceById(ID2);
    assertNull(fetchedUrlDevice);
  }

  @Test
  public void getMetadataByRequestUrlReturnsFoundMetadata() {
    PwsResult pwsResult = physicalWebCollection1.getMetadataByBroadcastUrl(URL1);
    assertEquals(pwsResult.getRequestUrl(), URL1);
    assertEquals(pwsResult.getSiteUrl(), URL1);
    assertEquals(pwsResult.getGroupId(), GROUP_ID1);
  }

  @Test
  public void getMetadataByRequestUrlReturnsNullForMissingMetadata() {
    PwsResult pwsResult = physicalWebCollection1.getMetadataByBroadcastUrl(URL2);
    assertNull(pwsResult);
  }

  @Test
  public void jsonSerializeWorks() {
    JSONAssert.assertEquals(physicalWebCollection1.jsonSerialize(), jsonObject1, true);
  }

  @Test
  public void jsonDeserializeWorks() throws PhysicalWebCollectionException {
    PhysicalWebCollection physicalWebCollection =
        PhysicalWebCollection.jsonDeserialize(jsonObject1);
    UrlDevice urlDevice = physicalWebCollection.getUrlDeviceById(ID1);
    PwsResult pwsResult = physicalWebCollection.getMetadataByBroadcastUrl(URL1);
    assertNotNull(urlDevice);
    assertEquals(urlDevice.getId(), ID1);
    assertEquals(urlDevice.getUrl(), URL1);
    assertNotNull(pwsResult);
    assertEquals(pwsResult.getRequestUrl(), URL1);
    assertEquals(pwsResult.getSiteUrl(), URL1);
    assertEquals(pwsResult.getGroupId(), GROUP_ID1);
  }

  @Test
  public void getPwPairsSortedByRankWorks() {
    PhysicalWebCollection physicalWebCollection = new PhysicalWebCollection();
    addRankedDeviceAndMetadata(physicalWebCollection, ID1, URL1, null, .1);
    addRankedDeviceAndMetadata(physicalWebCollection, ID2, URL2, null, .5);
    addRankedDeviceAndMetadata(physicalWebCollection, ID3, URL2, null, .9);  // Duplicate URL
    List<PwPair> pwPairs = physicalWebCollection.getPwPairsSortedByRank();
    assertEquals(pwPairs.size(), 2);
    assertEquals(pwPairs.get(0).getUrlDevice().getId(), ID3);
    assertEquals(pwPairs.get(1).getUrlDevice().getId(), ID1);
  }

  @Test
  public void getGroupedPwPairsSortedByRankWorks() {
    PhysicalWebCollection physicalWebCollection = new PhysicalWebCollection();
    addRankedDeviceAndMetadata(physicalWebCollection, ID1, URL1, GROUP_ID1, .1);  // Group 1
    addRankedDeviceAndMetadata(physicalWebCollection, ID2, URL2, null, .6);  // Ungrouped
    addRankedDeviceAndMetadata(physicalWebCollection, ID3, URL2, null, .7);  // Duplicate URL
    addRankedDeviceAndMetadata(physicalWebCollection, ID4, URL3a, GROUP_ID2, .5);  // Group 2
    addRankedDeviceAndMetadata(physicalWebCollection, ID5, URL3b, GROUP_ID2, .9);  // Also group 2
    List<PwPair> groupedPairs = physicalWebCollection.getGroupedPwPairsSortedByRank();
    assertEquals(groupedPairs.size(), 3);
    assertEquals(groupedPairs.get(0).getPwsResult().getGroupId(), GROUP_ID2);
    assertEquals(groupedPairs.get(0).getUrlDevice().getId(), ID5);
    assertEquals(groupedPairs.get(1).getPwsResult().getGroupId(), null);
    assertEquals(groupedPairs.get(1).getUrlDevice().getId(), ID3);
    assertEquals(groupedPairs.get(2).getPwsResult().getGroupId(), GROUP_ID1);
    assertEquals(groupedPairs.get(2).getUrlDevice().getId(), ID1);
  }

  @Test
  public void getTopRankedPwPairByGroupIdWorks() {
    PhysicalWebCollection physicalWebCollection = new PhysicalWebCollection();
    addRankedDeviceAndMetadata(physicalWebCollection, ID1, URL1, GROUP_ID1, .1);  // Group 1
    addRankedDeviceAndMetadata(physicalWebCollection, ID2, URL2, GROUP_ID1, .2);  // Better rank
    addRankedDeviceAndMetadata(physicalWebCollection, ID1, URL1, GROUP_ID2, .3);  // Group 2
    assertNull(physicalWebCollection.getTopRankedPwPairByGroupId("notagroup"));
    PwPair pwPair = physicalWebCollection.getTopRankedPwPairByGroupId(GROUP_ID1);
    assertNotNull(pwPair);
    assertEquals(ID2, pwPair.getUrlDevice().getId());
  }
}
