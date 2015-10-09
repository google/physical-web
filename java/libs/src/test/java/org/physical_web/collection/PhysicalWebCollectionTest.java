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

/**
 * PhysicalWebCollection unit test class.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class PhysicalWebCollectionTest {
  private static final String ID1 = "id1";
  private static final String ID2 = "id2";
  private static final String URL1 = "http://example.com";
  private static final String URL2 = "http://physical-web.org";
  private PhysicalWebCollection physicalWebCollection1;

  @Before
  public void setUp() {
    physicalWebCollection1 = new PhysicalWebCollection();
    UrlDevice urlDevice = new SimpleUrlDevice(ID1, URL1);
    PwsResult pwsResult = new PwsResult(URL1, URL1);
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
}
