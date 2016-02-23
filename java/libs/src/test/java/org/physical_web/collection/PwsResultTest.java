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
 * PwsResult unit test class.
 */
public class PwsResultTest {
  private static final String URL1 = "http://example.com";
  private static final String URL2 = "http://physical-web.org";
  private static final String TITLE1 = "title1";
  private static final String TITLE2 = "title2";
  private static final String DESCRIPTION1 = "description1";
  private static final String DESCRIPTION2 = "description2";
  private static final String ICON_URL1 = "http://example.com/favicon.ico";
  private static final String ICON_URL2 = "http://physical-web.org/favicon.ico";
  private static final String GROUP_ID1 = "group1";
  private static final String GROUP_ID2 = "group2";
  private PwsResult mPwsResult1 = null;
  private JSONObject jsonObject1 = null;

  @Before
  public void setUp() {
    mPwsResult1 = new PwsResult(URL1, URL1, TITLE1, DESCRIPTION1, ICON_URL1, GROUP_ID1);
    mPwsResult1.getExtraData().put("key", "value");
    String endline = "\",";
    jsonObject1 = new JSONObject("{"
        + "    \"requesturl\": \"" + URL1 + endline
        + "    \"siteurl\": \"" + URL1 + endline
        + "    \"title\": \"" + TITLE1 + endline
        + "    \"description\": \"" + DESCRIPTION1 + endline
        + "    \"iconurl\": \"" + ICON_URL1 + endline
        + "    \"groupid\": \"" + GROUP_ID1 + endline
        + "    \"extra\": {"
        + "        \"key\": \"value\""
        + "    }"
        + "}");
  }

  @Test
  public void constructorCreatesProperObject() {
    assertEquals(mPwsResult1.getRequestUrl(), URL1);
    assertEquals(mPwsResult1.getSiteUrl(), URL1);
    assertEquals(mPwsResult1.getTitle(), TITLE1);
    assertEquals(mPwsResult1.getDescription(), DESCRIPTION1);
    assertEquals(mPwsResult1.getIconUrl(), ICON_URL1);
    assertEquals(mPwsResult1.getGroupId(), GROUP_ID1);
  }

  @Test
  public void getExtraDataWorks() {
    assertEquals(1, mPwsResult1.getExtraData().length());
  }

  @Test
  public void jsonSerializeWorks() {
    JSONAssert.assertEquals(mPwsResult1.jsonSerialize(), jsonObject1, true);
  }

  @Test
  public void jsonDeserializeWorks() throws PhysicalWebCollectionException {
    PwsResult pwsResult = PwsResult.jsonDeserialize(jsonObject1);
    assertNotNull(pwsResult);
    assertEquals(pwsResult.getRequestUrl(), URL1);
    assertEquals(pwsResult.getSiteUrl(), URL1);
    assertEquals(pwsResult.getGroupId(), GROUP_ID1);
  }

  @Test
  public void jsonSerializeAndDeserializePreservesNullValues() throws Exception {
    PwsResult pwsResult =
        new PwsResult(URL1, URL1, TITLE1, DESCRIPTION1, null, null);  // null values
    pwsResult = PwsResult.jsonDeserialize(pwsResult.jsonSerialize());
    assertNull(pwsResult.getIconUrl());
    assertNull(pwsResult.getGroupId());
  }

  @Test
  public void resultIsEqualToItself() {
    assertEquals(mPwsResult1, mPwsResult1);
  }

  @Test
  public void alikeResultsAreEqual() {
    PwsResult pwsResult = new PwsResult(URL1, URL1, TITLE1, DESCRIPTION1, ICON_URL1, GROUP_ID1);
    pwsResult.getExtraData().put("key", "value");
    assertEquals(mPwsResult1, pwsResult);
  }

  @Test
  public void unalikeResultsAreNotEqual() {
    assertNotEquals(mPwsResult1,
        new PwsResult(URL2, URL1, TITLE1, DESCRIPTION1, ICON_URL1, GROUP_ID1));
    assertNotEquals(mPwsResult1,
        new PwsResult(URL1, URL2, TITLE1, DESCRIPTION1, ICON_URL1, GROUP_ID1));
    assertNotEquals(mPwsResult1,
        new PwsResult(URL1, URL1, TITLE2, DESCRIPTION1, ICON_URL1, GROUP_ID1));
    assertNotEquals(mPwsResult1,
        new PwsResult(URL1, URL1, TITLE1, DESCRIPTION2, ICON_URL1, GROUP_ID1));
    assertNotEquals(mPwsResult1,
        new PwsResult(URL1, URL1, TITLE1, DESCRIPTION1, ICON_URL2, GROUP_ID1));
    assertNotEquals(mPwsResult1,
        new PwsResult(URL1, URL1, TITLE1, DESCRIPTION1, ICON_URL1, GROUP_ID2));
  }

  @Test
  public void compareResultToItselfReturnsZero() {
    assertEquals(mPwsResult1.compareTo(mPwsResult1), 0);
  }

  @Test
  public void compareResultToAlikeResultReturnsZero() {
    PwsResult pwsResult = new PwsResult(URL1, URL1, TITLE1, DESCRIPTION1, ICON_URL1, GROUP_ID1);
    assertEquals(0, mPwsResult1.compareTo(pwsResult));
  }

  @Test
  public void compareResultToUnalikeResultReturnsNonZero() {
    // "example.com" < "physical-web.org"
    assertTrue(mPwsResult1.compareTo(
        new PwsResult(URL2, URL1, TITLE1, DESCRIPTION1, ICON_URL1, GROUP_ID1)) < 0);
    // "example.com" < "physical-web.org"
    assertTrue(mPwsResult1.compareTo(
        new PwsResult(URL1, URL2, TITLE1, DESCRIPTION1, ICON_URL1, GROUP_ID1)) < 0);
    // "title1" < "title2"
    assertTrue(mPwsResult1.compareTo(
        new PwsResult(URL1, URL1, TITLE2, DESCRIPTION1, ICON_URL1, GROUP_ID1)) < 0);
    // "description1" < "description2"
    assertTrue(mPwsResult1.compareTo(
        new PwsResult(URL1, URL1, TITLE1, DESCRIPTION2, ICON_URL1, GROUP_ID1)) < 0);
    // "example.com/favicon.ico" < "physical-web.org/favicon.ico"
    assertTrue(mPwsResult1.compareTo(
        new PwsResult(URL1, URL1, TITLE1, DESCRIPTION1, ICON_URL2, GROUP_ID1)) < 0);
    // "group1" < "group2"
    assertTrue(mPwsResult1.compareTo(
        new PwsResult(URL1, URL1, TITLE1, DESCRIPTION1, ICON_URL1, GROUP_ID2)) < 0);
  }
}
