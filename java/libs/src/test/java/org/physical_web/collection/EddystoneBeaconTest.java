/*
 * Copyright 2016 Google Inc. All rights reserved.
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

import org.junit.Test;

import java.io.UnsupportedEncodingException;

/**
 * Tests for the EddystoneBeacon class.
 */
public class EddystoneBeaconTest {

  @Test
  public void getFatBeaconTitleTest() throws UnsupportedEncodingException {
    // Array length failure
    assertEquals("", EddystoneBeacon.getFatBeaconTitle(new byte[]{}));
    assertEquals("", EddystoneBeacon.getFatBeaconTitle(new byte[]{0x01}));
    assertEquals("", EddystoneBeacon.getFatBeaconTitle(new byte[]{0x01, 0x02}));

    // Invalid byte sequence
    assertEquals("", EddystoneBeacon.getFatBeaconTitle(new byte[]{0x01, 0x02, 0x00}));

    // Valid title
    String title = "title";
    byte[] titleBytes = title.getBytes("UTF-8");
    int length = titleBytes.length;
    byte[] serviceData = new byte[length + 3];
    System.arraycopy(titleBytes, 0, serviceData, 3, length);
    serviceData[0] = 0x10;
    serviceData[1] = 0x00;
    serviceData[2] = 0x0e;
    assertEquals(title, EddystoneBeacon.getFatBeaconTitle(serviceData));
  }

  @Test
  public void isFatBeaconTest() {
    // Array length failure
    assertFalse(EddystoneBeacon.isFatBeacon(null));
    assertFalse(EddystoneBeacon.isFatBeacon(new byte[]{}));
    assertFalse(EddystoneBeacon.isFatBeacon(new byte[]{0x01}));
    assertFalse(EddystoneBeacon.isFatBeacon(new byte[]{0x01, 0x02}));
    assertFalse(EddystoneBeacon.isFatBeacon(new byte[]{0x01, 0x02, 0x03}));

    // Not URL Type failure
    assertFalse(EddystoneBeacon.isFatBeacon(new byte[]{0x01, 0x02, 0x0e, 0x04}));

    // Doesn't start with title type
    assertFalse(EddystoneBeacon.isFatBeacon(new byte[]{0x10, 0x02, 0x03, 0x04}));

    // Fat beacon
    assertTrue(EddystoneBeacon.isFatBeacon(new byte[]{0x10, 0x02, 0x0e, 0x04}));
  }

  @Test
  public void isUrlFrameTest() {
    // Array length failure
    assertFalse(EddystoneBeacon.isUrlFrame(null));
    assertFalse(EddystoneBeacon.isUrlFrame(new byte[]{}));

    // Not URL frame type
    assertFalse(EddystoneBeacon.isUrlFrame(new byte[]{0x20}));

    // URL frame type with various flags
    assertTrue(EddystoneBeacon.isUrlFrame(new byte[]{0x10}));
    assertTrue(EddystoneBeacon.isUrlFrame(new byte[]{0x11}));
    assertTrue(EddystoneBeacon.isUrlFrame(new byte[]{0x18}));
    assertTrue(EddystoneBeacon.isUrlFrame(new byte[]{0x1a}));
    assertTrue(EddystoneBeacon.isUrlFrame(new byte[]{0x1f}));
  }

  @Test
  public void parseFromServiceDataTest() {
    // Array length failure
    assertNull(EddystoneBeacon.parseFromServiceData(null, null));
    assertNull(EddystoneBeacon.parseFromServiceData(new byte[]{}, null));
    assertNull(EddystoneBeacon.parseFromServiceData(new byte[]{0x01}, null));
    assertNull(EddystoneBeacon.parseFromServiceData(new byte[]{0x01, 0x02}, null));
    assertNull(EddystoneBeacon.parseFromServiceData(null, new byte[]{}));
    assertNull(EddystoneBeacon.parseFromServiceData(null, new byte[]{0x01}));
    assertNull(EddystoneBeacon.parseFromServiceData(null, new byte[]{0x01, 0x02}));

    // Invalid URL
    assertNull(EddystoneBeacon.parseFromServiceData(new byte[]{0x10, 0x00, 0x4f}, null));

    // Valid URL
    EddystoneBeacon beacon = EddystoneBeacon.parseFromServiceData(new byte[]{0x10, 0x00, 0x01},
        null);
    assertEquals("https://www.", beacon.getUrl());
    assertEquals(0x00, beacon.getFlags());
    assertEquals(0x00, beacon.getTxPowerLevel());
  }

}
