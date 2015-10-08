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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * PhysicalWebCollection unit test class.
 */
public class PhysicalWebCollectionTest {
  private final String ID1 = "id1";
  private final String ID2 = "id2";
  private final String URL1 = "http://example.com";

  @Test
  public void getUrlDeviceByIdReturnsFoundDevice() {
    PhysicalWebCollection physicalWebCollection = new PhysicalWebCollection();
    UrlDevice addedUrlDevice = new SimpleUrlDevice(ID1, URL1);
    physicalWebCollection.addUrlDevice(addedUrlDevice);
    UrlDevice fetchedUrlDevice = physicalWebCollection.getUrlDeviceById(ID1);
    assertEquals(fetchedUrlDevice.getId(), ID1);
    assertEquals(fetchedUrlDevice.getUrl(), URL1);
  }

  @Test
  public void getUrlDeviceByIdReturnsNullForMissingDevice() {
    PhysicalWebCollection physicalWebCollection = new PhysicalWebCollection();
    UrlDevice fetchedUrlDevice = physicalWebCollection.getUrlDeviceById(ID1);
    assertNull(fetchedUrlDevice);
  }
}
