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

import org.junit.Before;
import org.junit.Test;

/**
 * SimpleUrlDevice unit test class.
 */
public class SimpleUrlDeviceTest {
  private static final String ID1 = "id1";
  private static final String ID2 = "id2";
  private static final String URL1 = "http://example.com";
  private static final String URL2 = "http://physical-web.org";
  private static final double RANK1 = 0.5d;
  private static final double RANK2 = 0.9d;
  private SimpleUrlDevice mUrlDevice1;

  @Before
  public void setUp() {
    mUrlDevice1 = new SimpleUrlDevice(ID1, URL1);
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
    PwsResult pwsResult = new PwsResult(URL1, URL1, null);
    assertEquals(.5, mUrlDevice1.getRank(pwsResult), .0001);
  }

  @Test
  public void deviceIsEqualToItself() {
    assertEquals(mUrlDevice1, mUrlDevice1);
  }

  @Test
  public void alikeDevicesAreEqual() {
    SimpleUrlDevice urlDevice2 = new SimpleUrlDevice(ID1, URL1);
    assertEquals(mUrlDevice1, urlDevice2);
  }

  @Test
  public void devicesWithDifferentClassButSameInfoAreEqual() {
    UrlDevice simpleUrlDevice = new SimpleUrlDevice(ID1, URL1);
    UrlDevice rankedDevice = new RankedDevice(ID1, URL1, RANK1);
    assertEquals(rankedDevice, simpleUrlDevice);
    assertEquals(simpleUrlDevice, rankedDevice);
  }

  @Test
  public void devicesWithDifferentRankButSameInfoAreEqual() {
    UrlDevice urlDevice2 = new RankedDevice(ID1, URL1, RANK2); // different rank
    assertEquals(mUrlDevice1, urlDevice2); // equals should not consider rank
  }

  @Test
  public void unalikeDevicesAreNotEqual() {
    SimpleUrlDevice urlDevice2 = new SimpleUrlDevice(ID1, URL2); // same id, different url
    SimpleUrlDevice urlDevice3 = new SimpleUrlDevice(ID2, URL1); // same url, different id
    assertNotEquals(mUrlDevice1, urlDevice2);
    assertNotEquals(mUrlDevice1, urlDevice3);
  }

  @Test
  public void compareDeviceToItselfReturnsZero() {
    assertEquals(mUrlDevice1.compareTo(mUrlDevice1), 0);
  }

  @Test
  public void compareDeviceToAlikeDeviceReturnsZero() {
    UrlDevice urlDevice2 = new SimpleUrlDevice(ID1, URL1);
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
    UrlDevice urlDevice2 = new SimpleUrlDevice(ID2, URL1); // different device ID
    UrlDevice urlDevice3 = new SimpleUrlDevice(ID1, URL2); // different URL
    assertTrue(mUrlDevice1.compareTo(urlDevice2) < 0); // "id1" < "id2"
    assertTrue(urlDevice2.compareTo(mUrlDevice1) > 0);
    assertTrue(mUrlDevice1.compareTo(urlDevice3) < 0); // "example.com" < "physical-web.org"
    assertTrue(urlDevice3.compareTo(mUrlDevice1) > 0);
  }
}
