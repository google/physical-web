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
 * SimpleUrlDevice unit test class.
 */
public class UrlDeviceTest {
  private static final String ID1 = "id1";
  private static final String ID2 = "id2";
  private static final String URL1 = "http://example.com";
  private static final String URL2 = "http://physical-web.org";
  private static final double RANK1 = 0.5d;
  private static final double RANK2 = 0.9d;
  private UrlDevice mUrlDevice1;
  private JSONObject jsonObject1;

  @Before
  public void setUp() {
    mUrlDevice1 = new UrlDevice.Builder(ID1, URL1)
        .addExtra("key", "value")
        .build();
    jsonObject1 = new JSONObject("{"
        + "    \"id\": \"" + ID1 + "\","
        + "    \"url\": \"" + URL1 + "\","
        + "    \"extra\": {"
        + "        \"key\": \"value\""
        + "    }"
        + "}");
  }

  @Test
  public void getIdReturnsId() {
    assertEquals(mUrlDevice1.getId(), ID1);
  }

  @Test
  public void getUrlReturnsUrl() {
    assertEquals(mUrlDevice1.getUrl(), URL1);
  }

  @Test
  public void getRankReturnsPointFive() {
    PwsResult pwsResult = new PwsResult.Builder(URL1, URL1)
        .setTitle("title1")
        .setDescription("description1")
        .build();
    assertEquals(.5, mUrlDevice1.getRank(pwsResult), .0001);
  }

  @Test
  public void jsonSerializeWorks() {
    JSONAssert.assertEquals(mUrlDevice1.jsonSerialize(), jsonObject1, true);
  }

  @Test
  public void jsonDeserializeWorks() {
    UrlDevice urlDevice = UrlDevice.jsonDeserialize(jsonObject1);
    assertNotNull(urlDevice);
    assertEquals(urlDevice.getId(), ID1);
    assertEquals(urlDevice.getUrl(), URL1);
  }

  @Test
  public void deviceIsEqualToItself() {
    assertEquals(mUrlDevice1, mUrlDevice1);
  }

  @Test
  public void alikeDevicesAreEqual() {
    UrlDevice urlDevice = new UrlDevice.Builder(ID1, URL1)
        .addExtra("key", "value")
        .build();
    assertEquals(mUrlDevice1, urlDevice);
  }

  @Test
  public void devicesWithDifferentRankButSameInfoAreEqual() {
    UrlDevice urlDevice1 = new RankedDevice(ID1, URL1, RANK1); // different rank
    UrlDevice urlDevice2 = new RankedDevice(ID1, URL1, RANK2); // different rank
    assertEquals(urlDevice1, urlDevice2); // equals should not consider rank
  }

  @Test
  public void unalikeDevicesAreNotEqual() {
    UrlDevice urlDevice1 = new UrlDevice(ID1, URL1);
    UrlDevice urlDevice2 = new UrlDevice(ID1, URL2); // same id, different url
    UrlDevice urlDevice3 = new UrlDevice(ID2, URL1); // same url, different id
    assertNotEquals(urlDevice1, urlDevice2);
    assertNotEquals(urlDevice1, urlDevice3);
  }

  @Test
  public void compareDeviceToItselfReturnsZero() {
    assertEquals(mUrlDevice1.compareTo(mUrlDevice1), 0);
  }

  @Test
  public void compareDeviceToAlikeDeviceReturnsZero() {
    UrlDevice urlDevice2 = new UrlDevice(ID1, URL1);
    UrlDevice urlDevice3 = new RankedDevice(ID1, URL1, RANK1);
    assertEquals(mUrlDevice1.compareTo(urlDevice2), 0); // identical device
    assertEquals(mUrlDevice1.compareTo(urlDevice3), 0); // same info, but uses a RankedDevice
    assertEquals(urlDevice3.compareTo(mUrlDevice1), 0); // reverse comparison
  }

  @Test
  public void compareDeviceWithDifferentRankReturnsZero() {
    UrlDevice urlDevice2 = new RankedDevice(ID1, URL1, RANK2); // different rank
    assertEquals(mUrlDevice1.compareTo(urlDevice2), 0); // compareTo should not consider rank
    assertEquals(urlDevice2.compareTo(mUrlDevice1), 0);
  }

  @Test
  public void compareDeviceToUnalikeDeviceReturnsNonZero() {
    UrlDevice urlDevice2 = new UrlDevice(ID2, URL1); // different device ID
    UrlDevice urlDevice3 = new UrlDevice(ID1, URL2); // different URL
    assertTrue(mUrlDevice1.compareTo(urlDevice2) < 0); // "id1" < "id2"
    assertTrue(urlDevice2.compareTo(mUrlDevice1) > 0);
    assertTrue(mUrlDevice1.compareTo(urlDevice3) < 0); // "example.com" < "physical-web.org"
    assertTrue(urlDevice3.compareTo(mUrlDevice1) > 0);
  }
}
