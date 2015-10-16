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
 * PwPair unit test class.
 */
public class PwPairTest {
  private static final String ID1 = "id1";
  private static final String ID2 = "id2";
  private static final String URL1 = "http://example.com";
  private static final String URL2 = "http://physical-web.org";
  private static final String GROUPID1 = "group1";
  private static final double RANK1 = 0.5d;
  private static final double RANK2 = 0.9d;
  private SimpleUrlDevice mUrlDevice1;
  private PwsResult mPwsResult1;
  private PwPair mPwPair1;

  @Before
  public void setUp() {
    mUrlDevice1 = new SimpleUrlDevice(ID1, URL1);
    mPwsResult1 = new PwsResult(URL1, URL1, null);
    mPwPair1 = new PwPair(mUrlDevice1, mPwsResult1);
  }

  @Test
  public void constructorCreatesProperObject() {
    assertEquals(mUrlDevice1, mPwPair1.getUrlDevice());
    assertEquals(mPwsResult1, mPwPair1.getPwsResult());
  }

  @Test
  public void pairIsEqualToItself() {
    assertEquals(mPwPair1, mPwPair1);
  }

  @Test
  public void alikePairsAreEqual() {
    UrlDevice urlDevice2 = new SimpleUrlDevice(ID1, URL1);
    PwsResult pwsResult2 = new PwsResult(URL1, URL1, null);
    UrlDevice urlDevice3 = new RankedDevice(ID1, URL1, RANK1);
    PwPair pwPair2 = new PwPair(urlDevice2, pwsResult2); // identical PwPair
    PwPair pwPair3 = new PwPair(urlDevice3, mPwsResult1); // same info, but uses a RankedDevice
    assertEquals(mPwPair1, pwPair2);
    assertEquals(mPwPair1, pwPair3);
  }

  @Test
  public void unalikePairsAreNotEqual() {
    PwsResult pwsResult2 = new PwsResult(URL1, URL2, GROUPID1);
    UrlDevice urlDevice2 = new SimpleUrlDevice(ID2, URL1);
    UrlDevice urlDevice3 = new RankedDevice(ID1, URL1, RANK2);
    PwPair pwPair2 = new PwPair(mUrlDevice1, pwsResult2); // different URL metadata
    PwPair pwPair3 = new PwPair(urlDevice2, mPwsResult1); // different device
    PwPair pwPair4 = new PwPair(urlDevice3, mPwsResult1); // different rank
    assertNotEquals(mPwPair1, pwPair2);
    assertNotEquals(mPwPair1, pwPair3);
    assertNotEquals(mPwPair1, pwPair4);
  }

  @Test
  public void comparePairToItselfReturnsZero() {
    assertEquals(mPwPair1.compareTo(mPwPair1), 0);
  }

  @Test
  public void comparePairToAlikePairReturnsZero() {
    UrlDevice urlDevice2 = new SimpleUrlDevice(ID1, URL1);
    PwsResult pwsResult2 = new PwsResult(URL1, URL1, null);
    UrlDevice urlDevice3 = new RankedDevice(ID1, URL1, RANK1);
    PwPair pwPair2 = new PwPair(urlDevice2, pwsResult2); // identical PwPair
    PwPair pwPair3 = new PwPair(urlDevice3, mPwsResult1); // same info, but uses a RankedDevice
    assertEquals(mPwPair1.compareTo(pwPair2), 0);
    assertEquals(mPwPair1.compareTo(pwPair3), 0);
    assertEquals(pwPair3.compareTo(mPwPair1), 0); // exercise null checks with reverse compare
  }

  @Test
  public void comparePairToUnalikePairReturnsNonZero() {
    UrlDevice urlDevice2 = new SimpleUrlDevice(ID2, URL1);
    UrlDevice urlDevice3 = new RankedDevice(ID1, URL1, RANK2);
    PwsResult pwsResult2 = new PwsResult(URL1, URL1, GROUPID1);
    PwPair pwPair2 = new PwPair(urlDevice2, mPwsResult1);
    PwPair pwPair3 = new PwPair(urlDevice3, mPwsResult1);
    PwPair pwPair4 = new PwPair(mUrlDevice1, pwsResult2);
    assertTrue(mPwPair1.compareTo(pwPair2) < 0); // "ID1" < "ID2"
    assertTrue(mPwPair1.compareTo(pwPair3) < 0); // 0.5 < 0.9
    assertTrue(mPwPair1.compareTo(pwPair4) < 0); // null < "group1"
    assertTrue(pwPair2.compareTo(mPwPair1) > 0);
    assertTrue(pwPair3.compareTo(mPwPair1) > 0);
    assertTrue(pwPair4.compareTo(mPwPair1) > 0);
  }
}
