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

import org.json.JSONArray;
import org.json.JSONObject;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.skyscreamer.jsonassert.JSONAssert;

/**
 * PhysicalWebCollection unit test class.
 */
public class PhysicalWebCollectionTest {
  private final String ID1 = "id1";
  private final String ID2 = "id2";
  private final String URL1 = "http://example.com";
  private PhysicalWebCollection physicalWebCollection1;

  @Before
  public void setUp() {
    physicalWebCollection1 = new PhysicalWebCollection();
    UrlDevice urlDevice = new SimpleUrlDevice(ID1, URL1);
    physicalWebCollection1.addUrlDevice(urlDevice);
  }

  @Test
  public void getUrlDeviceByIdReturnsFoundUrlDevice() {
    UrlDevice urlDevice = physicalWebCollection1.getUrlDeviceById(ID1);
    assertEquals(urlDevice.getId(), ID1);
    assertEquals(urlDevice.getUrl(), URL1);
  }

  @Test
  public void getUrlDeviceByReturnsNullForMissingUrlDevice() {
    UrlDevice fetchedUrlDevice = physicalWebCollection1.getUrlDeviceById(ID2);
    assertNull(fetchedUrlDevice);
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
        + "    }]"
        + "}");
    JSONAssert.assertEquals(physicalWebCollection1.jsonSerialize(), jsonObject, true);
  }

  @Test(expected=PhysicalWebCollectionException.class)
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
        + "    }]"
        + "}");
    physicalWebCollection.jsonDeserialize(jsonObject);
    UrlDevice urlDevice = physicalWebCollection.getUrlDeviceById(ID1);
    assertNotNull(urlDevice);
    assertEquals(urlDevice.getId(), ID1);
    assertEquals(urlDevice.getUrl(), URL1);
  }

  @Test(expected=PhysicalWebCollectionException.class)
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
