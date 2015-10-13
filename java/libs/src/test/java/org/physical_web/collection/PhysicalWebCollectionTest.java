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
  private static final String URL1 = "http://example.com";
  private static final String URL2 = "http://physical-web.org";
  private PhysicalWebCollection physicalWebCollection1;
  private PhysicalWebCollection physicalWebCollection2;

  private static class RankedDevice extends SimpleUrlDevice {
    private double mRank;

    RankedDevice(String id, String url, double rank) {
      super(id, url);
      mRank = rank;
    }

    public double getRank(PwsResult pwsResult) {
      return mRank;
    }
  }

  private void addRankedDevice(String id, String url, double rank) {
    UrlDevice urlDevice = new RankedDevice(id, url, rank);
    PwsResult pwsResult = new PwsResult(url, url);
    physicalWebCollection2.addUrlDevice(urlDevice);
    physicalWebCollection2.addMetadata(pwsResult);
  }

  @Before
  public void setUp() {
    physicalWebCollection1 = new PhysicalWebCollection();
    UrlDevice urlDevice = new SimpleUrlDevice(ID1, URL1);
    PwsResult pwsResult = new PwsResult(URL1, URL1);
    physicalWebCollection1.addUrlDevice(urlDevice);
    physicalWebCollection1.addMetadata(pwsResult);
    physicalWebCollection2 = new PhysicalWebCollection();
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
        + "        \"siteurl\": \"" + URL1 + "\""
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
        + "        \"siteurl\": \"" + URL1 + "\""
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

  public void getPwPairsSortedByRankWorks() {
    addRankedDevice(ID1, URL1, .1);
    addRankedDevice(ID2, URL2, .5);
    addRankedDevice(ID3, URL2, .9);  // Duplicate URL
    List<PwPair> pwPairs = physicalWebCollection2.getPwPairsSortedByRank();
    assertEquals(pwPairs.size(), 3);
    assertEquals(pwPairs.get(0).getUrlDevice().getId(), ID3);
    assertEquals(pwPairs.get(1).getUrlDevice().getId(), ID2);
    assertEquals(pwPairs.get(2).getUrlDevice().getId(), ID1);
  }
}
