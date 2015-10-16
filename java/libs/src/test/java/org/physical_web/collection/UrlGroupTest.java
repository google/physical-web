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
 * UrlGroup unit test class.
 */
public class UrlGroupTest {
  private static final String ID1 = "id1";
  private static final String ID2 = "id2";
  private static final String URL1 = "http://physical-web.org/#a";
  private static final String URL2 = "http://physical-web.org/#b";
  private static final String GROUPID1 = "group1";
  private static final String GROUPID2 = "group2";
  private static final double RANK1 = 0.5d;
  private static final double RANK2 = 0.9d;

  private PwPair mPwPair1;
  private PwPair mPwPair2;

  @Before
  public void setUp() {
    mPwPair1 = RankedDevice.createRankedPair(ID1, URL1, GROUPID1, RANK1);
    mPwPair2 = RankedDevice.createRankedPair(ID2, URL2, GROUPID1, RANK2);
  }

  @Test
  public void constructorCreatesProperObject() {
    UrlGroup urlGroup = new UrlGroup(GROUPID1);
    assertEquals(urlGroup.getGroupId(), GROUPID1);
  }

  @Test
  public void addPairAndGetTopPairWorks() {
    UrlGroup urlGroup = new UrlGroup(GROUPID1);
    urlGroup.addPair(mPwPair1);

    PwPair pwPair = urlGroup.getTopPair();
    assertEquals(pwPair.getUrlDevice().getId(), ID1);
    assertEquals(pwPair.getUrlDevice().getUrl(), URL1);
    assertEquals(pwPair.getPwsResult().getRequestUrl(), URL1);
    assertEquals(pwPair.getPwsResult().getSiteUrl(), URL1);
    assertEquals(pwPair.getPwsResult().getGroupId(), GROUPID1);
  }

  @Test
  public void addPairTwiceAndGetTopPairWorks() {
    UrlGroup urlGroup = new UrlGroup(GROUPID1);
    urlGroup.addPair(mPwPair1);
    urlGroup.addPair(mPwPair2); // higher rank

    PwPair pwPair = urlGroup.getTopPair();
    assertEquals(pwPair.getUrlDevice().getId(), ID2);
  }

  @Test
  public void groupIsEqualToItself() {
    UrlGroup urlGroup1 = new UrlGroup(GROUPID1);
    assertEquals(urlGroup1, urlGroup1);
  }

  @Test
  public void alikeEmptyGroupsAreEqual() {
    UrlGroup urlGroup1 = new UrlGroup(GROUPID1);
    UrlGroup urlGroup2 = new UrlGroup(GROUPID1); // same groupid
    assertEquals(urlGroup1, urlGroup2);
  }

  @Test
  public void unalikeEmptyGroupsAreNotEqual() {
    UrlGroup urlGroup1 = new UrlGroup(GROUPID1);
    UrlGroup urlGroup2 = new UrlGroup(GROUPID2); // different groupid
    assertNotEquals(urlGroup1, urlGroup2);
  }

  @Test
  public void alikeGroupsAreEqual() {
    UrlGroup urlGroup1 = new UrlGroup(GROUPID1);
    UrlGroup urlGroup2 = new UrlGroup(GROUPID1);
    urlGroup1.addPair(mPwPair1);
    urlGroup1.addPair(mPwPair2);
    urlGroup2.addPair(mPwPair2); // add the pairs in a different order
    urlGroup2.addPair(mPwPair1);
    assertEquals(urlGroup1, urlGroup2);
  }

  @Test
  public void unalikeGroupsAreNotEqual() {
    UrlGroup urlGroup1 = new UrlGroup(GROUPID1);
    UrlGroup urlGroup2 = new UrlGroup(GROUPID1); // same groupid
    UrlGroup urlGroup3 = new UrlGroup(GROUPID2); // different groupid
    PwPair pwPair1 = RankedDevice.createRankedPair(ID1, URL1, GROUPID1, RANK1);
    PwPair pwPair2 = RankedDevice.createRankedPair(ID2, URL2, GROUPID1, RANK1);
    PwPair pwPair3 = RankedDevice.createRankedPair(ID1, URL1, GROUPID2, RANK1);
    urlGroup1.addPair(pwPair1);
    urlGroup2.addPair(pwPair2);
    urlGroup3.addPair(pwPair3);
    assertNotEquals(urlGroup1, urlGroup2); // same groupid, different URLs
    assertNotEquals(urlGroup1, urlGroup3); // different groupid, same URLs
    urlGroup1.addPair(pwPair2);
    assertNotEquals(urlGroup1, urlGroup2); // urlGroup1 has two items, urlGroup2 has only one
  }

  @Test
  public void compareGroupToItselfReturnsZero() {
    UrlGroup urlGroup1 = new UrlGroup(GROUPID1);
    assertEquals(urlGroup1.compareTo(urlGroup1), 0); // compare empty group to itself
    urlGroup1.addPair(mPwPair1);
    assertEquals(urlGroup1.compareTo(urlGroup1), 0); // compare non-empty group to itself
  }

  @Test
  public void compareGroupToAlikeGroupReturnsZero() {
    UrlGroup urlGroup1 = new UrlGroup(GROUPID1);
    UrlGroup urlGroup2 = new UrlGroup(GROUPID1);
    assertEquals(urlGroup1.compareTo(urlGroup2), 0); // compare identical empty groups
    urlGroup1.addPair(mPwPair1); // add pair 1 to both
    urlGroup2.addPair(mPwPair1);
    assertEquals(urlGroup1.compareTo(urlGroup2), 0); // compare identical groups with one pair
  }

  @Test
  public void compareGroupToAlikeGroupWithDifferentOrderReturnsZero() {
    UrlGroup urlGroup1 = new UrlGroup(GROUPID1);
    UrlGroup urlGroup2 = new UrlGroup(GROUPID1);
    urlGroup1.addPair(mPwPair1); // add pair1, then pair2
    urlGroup1.addPair(mPwPair2);
    urlGroup2.addPair(mPwPair2); // add pair2, then pair1
    urlGroup2.addPair(mPwPair1);
    assertEquals(urlGroup1.compareTo(urlGroup2), 0);
  }

  @Test
  public void compareGroupToUnalikeGroupReturnsNonZero() {
    UrlGroup urlGroup1 = new UrlGroup(GROUPID1);
    UrlGroup urlGroup2 = new UrlGroup(GROUPID1);
    UrlGroup urlGroup3 = new UrlGroup(GROUPID2);
    assertTrue(urlGroup1.compareTo(urlGroup3) < 0); // "group1" < "group2"
    PwPair pwPair3 = RankedDevice.createRankedPair(ID1, URL1, GROUPID1, RANK2);
    urlGroup1.addPair(mPwPair1);
    urlGroup2.addPair(pwPair3);
    assertTrue(urlGroup1.compareTo(urlGroup2) < 0); // 0.5 < 0.9
    assertTrue(urlGroup2.compareTo(urlGroup1) > 0);
  }
}
