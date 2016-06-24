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
  private static final String TITLE1 = "title1";
  private static final String TITLE2 = "title2";
  private static final String DESCRIPTION1 = "description1";
  private static final String DESCRIPTION2 = "description2";
  private static final String ICON_URL1 = "http://example.com/favicon.ico";
  private static final String ICON_URL2 = "http://physical-web.org/favicon.ico";
  private static final String GROUP_ID1 = "group1";
  private static final String GROUP_ID2 = "group2";
  private static final double RANK1 = 0.5d;
  private static final double RANK2 = 0.9d;
  private UrlDevice mUrlDevice1;
  private PwsResult mPwsResult1;
  private PwPair mPwPair1 = null;

  @Before
  public void setUp() {
    mUrlDevice1 = new UrlDevice(ID1, URL1);
    mPwsResult1 = new PwsResult.Builder(URL1, URL1)
        .setTitle(TITLE1)
        .setDescription(DESCRIPTION1)
        .setIconUrl(ICON_URL1)
        .setGroupId(GROUP_ID1)
        .build();
    mPwPair1 = new PwPair(mUrlDevice1, mPwsResult1);
  }

  @Test
  public void constructorCreatesProperObject() {
    assertEquals(mUrlDevice1, mPwPair1.getUrlDevice());
    assertEquals(mPwsResult1, mPwPair1.getPwsResult());
  }
}
