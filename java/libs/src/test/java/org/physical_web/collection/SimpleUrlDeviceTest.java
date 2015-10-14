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
  private static final String URL1 = "http://example.com";
  private SimpleUrlDevice urlDevice1;

  @Before
  public void setUp() {
    urlDevice1 = new SimpleUrlDevice(ID1, URL1);
  }

  @Test
  public void getIdReturnsId() {
    assertEquals(urlDevice1.getId(), ID1);
  }

  @Test
  public void getUrlReturnsUrl() {
    assertEquals(urlDevice1.getUrl(), URL1);
  }

  @Test
  public void getRankReturnsPointFive() {
    PwsResult pwsResult = new PwsResult(URL1, URL1, null);
    assertEquals(.5, urlDevice1.getRank(pwsResult), .0001);
  }
}
