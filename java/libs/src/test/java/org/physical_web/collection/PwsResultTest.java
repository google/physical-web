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
  private static final String KEY1 = "key1";
  private static final String VALUE1 = "value1";
  private PwsResult mPwsResult1 = null;
  private JSONObject jsonObject1 = null;

  @Before
  public void setUp() {
    mPwsResult1 = new PwsResult.Builder(URL1, URL1)
        .setTitle(TITLE1)
        .setDescription(DESCRIPTION1)
        .setIconUrl(ICON_URL1)
        .setGroupId(GROUP_ID1)
        .addExtra(KEY1, VALUE1)
        .build();
    String endline = "\",";
    jsonObject1 = new JSONObject("{"
        + "    \"requesturl\": \"" + URL1 + endline
        + "    \"siteurl\": \"" + URL1 + endline
        + "    \"title\": \"" + TITLE1 + endline
        + "    \"description\": \"" + DESCRIPTION1 + endline
        + "    \"iconurl\": \"" + ICON_URL1 + endline
        + "    \"groupid\": \"" + GROUP_ID1 + endline
        + "    \"extra\": {"
        + "        \"" + KEY1 + "\": \"" + VALUE1 + "\""
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
  public void jsonSerializeWorks() {
    JSONAssert.assertEquals(mPwsResult1.jsonSerialize(), jsonObject1, true);
  }

  @Test
  public void jsonDeserializeWorks() throws PhysicalWebCollectionException {
    PwsResult pwsResult = PwsResult.jsonDeserialize(jsonObject1);
    assertNotNull(pwsResult);
    assertEquals(pwsResult.getRequestUrl(), URL1);
    assertEquals(pwsResult.getSiteUrl(), URL1);
    assertEquals(pwsResult.getTitle(), TITLE1);
    assertEquals(pwsResult.getDescription(), DESCRIPTION1);
    assertEquals(pwsResult.getIconUrl(), ICON_URL1);
    assertEquals(pwsResult.getGroupId(), GROUP_ID1);
  }

  @Test
  public void jsonSerializeAndDeserializePreservesNullValues() throws Exception {
    PwsResult pwsResult = new PwsResult(URL1, URL1);
    pwsResult = PwsResult.jsonDeserialize(pwsResult.jsonSerialize());
    assertNull(pwsResult.getTitle());
    assertNull(pwsResult.getDescription());
    assertNull(pwsResult.getIconUrl());
    assertNull(pwsResult.getGroupId());
  }
}
