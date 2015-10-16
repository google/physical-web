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
  private static final String GROUPID1 = "group1";
  private static final String GROUPID2 = "group2";
  private PhysicalWebCollection physicalWebCollection1;

  private void addRankedDeviceAndMetadata(PhysicalWebCollection collection, String id, String url,
                                          String groupId, double rank) {
    PwPair rankedPair = RankedDevice.createRankedPair(id, url, groupId, rank);
    collection.addUrlDevice(rankedPair.getUrlDevice());
    collection.addMetadata(rankedPair.getPwsResult());
  }

  @Before
  public void setUp() {
    physicalWebCollection1 = new PhysicalWebCollection();
    UrlDevice urlDevice = new SimpleUrlDevice(ID1, URL1);
    PwsResult pwsResult = new PwsResult(URL1, URL1, GROUPID1);
    physicalWebCollection1.addUrlDevice(urlDevice);
    physicalWebCollection1.addMetadata(pwsResult);
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
    assertEquals(pwsResult.getGroupId(), GROUPID1);
  }

  @Test
  public void getMetadataByRequestUrlReturnsNullForMissingMetadata() {
    PwsResult pwsResult = physicalWebCollection1.getMetadataByBroadcastUrl(URL2);
    assertNull(pwsResult);
  }

  @Test
  public void jsonSerializeWorks() throws PhysicalWebCollectionException {
    physicalWebCollection1.addUrlDeviceJsonSerializer(SimpleUrlDevice.class,
                                                      new SimpleUrlDeviceJsonSerializer());
    JSONObject jsonObject = new JSONObject("{"
        + "    \"schema\": 1,"
        + "    \"devices\": [{"
        + "        \"type\": \"org.physical_web.collection.SimpleUrlDevice\","
        + "        \"data\": {"
        + "            \"id\": \"" + ID1 + "\","
        + "            \"url\": \"" + URL1 + "\""
        + "        }"
        + "    }],"
        + "    \"metadata\": [{"
        + "        \"requesturl\": \"" + URL1 + "\","
        + "        \"siteurl\": \"" + URL1 + "\","
        + "        \"groupid\": \"" + GROUPID1 + "\""
        + "    }]"
        + "}");
    JSONAssert.assertEquals(physicalWebCollection1.jsonSerialize(), jsonObject, true);
  }

  @Test(expected = PhysicalWebCollectionException.class)
  public void jsonSerializeWithoutSerializerThrowsException()
      throws PhysicalWebCollectionException {
    physicalWebCollection1.jsonSerialize();
  }

  @Test
  public void jsonDeserializeWorks() throws PhysicalWebCollectionException {
    PhysicalWebCollection physicalWebCollection = new PhysicalWebCollection();
    physicalWebCollection.addUrlDeviceJsonSerializer(SimpleUrlDevice.class,
                                                     new SimpleUrlDeviceJsonSerializer());
    JSONObject jsonObject = new JSONObject("{"
        + "    \"schema\": 1,"
        + "    \"devices\": [{"
        + "        \"type\": \"org.physical_web.collection.SimpleUrlDevice\","
        + "        \"data\": {"
        + "            \"id\": \"" + ID1 + "\","
        + "            \"url\": \"" + URL1 + "\""
        + "        }"
        + "    }],"
        + "    \"metadata\": [{"
        + "        \"requesturl\": \"" + URL1 + "\","
        + "        \"siteurl\": \"" + URL1 + "\","
        + "        \"groupid\": \"" + GROUPID1 + "\""
        + "    }]"
        + "}");
    physicalWebCollection.jsonDeserialize(jsonObject);
    UrlDevice urlDevice = physicalWebCollection.getUrlDeviceById(ID1);
    PwsResult pwsResult = physicalWebCollection.getMetadataByBroadcastUrl(URL1);
    assertNotNull(urlDevice);
    assertEquals(urlDevice.getId(), ID1);
    assertEquals(urlDevice.getUrl(), URL1);
    assertEquals(pwsResult.getRequestUrl(), URL1);
    assertEquals(pwsResult.getSiteUrl(), URL1);
    assertEquals(pwsResult.getGroupId(), GROUPID1);
  }

  @Test(expected = PhysicalWebCollectionException.class)
  public void jsonDeserializeWithoutSerializerThrowsException()
      throws PhysicalWebCollectionException {
    PhysicalWebCollection physicalWebCollection = new PhysicalWebCollection();
    JSONObject jsonObject = new JSONObject("{"
        + "    \"schema\": 1,"
        + "    \"devices\": [{"
        + "        \"type\": \"org.physical_web.collection.SimpleUrlDevice\","
        + "        \"data\": {"
        + "            \"id\": \"" + ID1 + "\","
        + "            \"url\": \"" + URL1 + "\""
        + "        }"
        + "    }]"
        + "}");
    physicalWebCollection.jsonDeserialize(jsonObject);
  }

  @Test
  public void jsonSerializeAndDeserializePreservesNullGroupId() throws Exception {
    PhysicalWebCollection physicalWebCollection = new PhysicalWebCollection();
    physicalWebCollection.addMetadata(new PwsResult(URL1, URL1, null)); // no group ID

    PhysicalWebCollection deserializedCollection = new PhysicalWebCollection();
    deserializedCollection.jsonDeserialize(physicalWebCollection.jsonSerialize());
    PwsResult deserializedResult = deserializedCollection.getMetadataByBroadcastUrl(URL1);
    assertNull(deserializedResult.getGroupId());
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
    addRankedDeviceAndMetadata(physicalWebCollection, ID1, URL1, GROUPID1, .1);  // Group 1
    addRankedDeviceAndMetadata(physicalWebCollection, ID2, URL2, null, .6);  // Ungrouped
    addRankedDeviceAndMetadata(physicalWebCollection, ID3, URL2, null, .7);  // Duplicate URL
    addRankedDeviceAndMetadata(physicalWebCollection, ID4, URL3a, GROUPID2, .5);  // Group 2
    addRankedDeviceAndMetadata(physicalWebCollection, ID5, URL3b, GROUPID2, .9);  // Also group 2
    List<PwPair> groupedPairs = physicalWebCollection.getGroupedPwPairsSortedByRank();
    assertEquals(groupedPairs.size(), 3);
    assertEquals(groupedPairs.get(0).getPwsResult().getGroupId(), GROUPID2);
    assertEquals(groupedPairs.get(0).getUrlDevice().getId(), ID5);
    assertEquals(groupedPairs.get(1).getPwsResult().getGroupId(), null);
    assertEquals(groupedPairs.get(1).getUrlDevice().getId(), ID3);
    assertEquals(groupedPairs.get(2).getPwsResult().getGroupId(), GROUPID1);
    assertEquals(groupedPairs.get(2).getUrlDevice().getId(), ID1);
  }
}
