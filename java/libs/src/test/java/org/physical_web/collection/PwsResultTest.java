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
 * PwsResult unit test class.
 */
public class PwsResultTest {
  private static final String URL1 = "http://example.com";
  private static final String URL2 = "http://physical-web.org";
  private static final String GROUPID1 = "group1";
  private PwsResult mPwsResult1;
  private PwsResult mPwsResult2;

  @Before
  public void setUp() {
    mPwsResult1 = new PwsResult(URL1, URL1, null);
    mPwsResult2 = new PwsResult(URL1, URL1, GROUPID1);
  }

  @Test
  public void constructorCreatesProperObject() {
    assertEquals(mPwsResult1.getRequestUrl(), URL1);
    assertEquals(mPwsResult1.getSiteUrl(), URL1);
  }

  @Test
  public void resultIsEqualToItself() {
    assertEquals(mPwsResult1, mPwsResult1);
  }

  @Test
  public void alikeResultsAreEqual() {
    PwsResult pwsResult3 = new PwsResult(URL1, URL1, null); // identical to mPwsResult1
    PwsResult pwsResult4 = new PwsResult(URL1, URL1, GROUPID1); // identical to mPwsResult2
    assertEquals(mPwsResult1, pwsResult3);
    assertEquals(mPwsResult2, pwsResult4);
  }

  @Test
  public void unalikeResultsAreNotEqual() {
    PwsResult pwsResult3 = new PwsResult(URL2, URL2, null);
    assertNotEquals(mPwsResult1, mPwsResult2); // groupid mismatch
    assertNotEquals(mPwsResult2, mPwsResult1); // groupid mismatch (reverse comparison)
    assertNotEquals(mPwsResult1, pwsResult3); // URL mismatch
  }

  @Test
  public void compareResultToItselfReturnsZero() {
    assertEquals(mPwsResult1.compareTo(mPwsResult1), 0);
    assertEquals(mPwsResult2.compareTo(mPwsResult2), 0);
  }

  @Test
  public void compareResultToAlikeResultReturnsZero() {
    PwsResult pwsResult3 = new PwsResult(URL1, URL1, null); // identical to mPwsResult1
    PwsResult pwsResult4 = new PwsResult(URL1, URL1, GROUPID1); // identical to mPwsResult2
    assertEquals(mPwsResult1.compareTo(pwsResult3), 0);
    assertEquals(pwsResult3.compareTo(mPwsResult1), 0);
    assertEquals(mPwsResult2.compareTo(pwsResult4), 0);
  }

  @Test
  public void compareResultToUnalikeResultReturnsNonZero() {
    PwsResult pwsResult3 = new PwsResult(URL2, URL2, null);
    assertTrue(mPwsResult1.compareTo(mPwsResult2) < 0); // null < "group1"
    assertTrue(mPwsResult2.compareTo(mPwsResult1) > 0);
    assertTrue(mPwsResult1.compareTo(pwsResult3) < 0); // "example.com" < "physical-web.org"
    assertTrue(pwsResult3.compareTo(mPwsResult1) > 0);
  }
}
