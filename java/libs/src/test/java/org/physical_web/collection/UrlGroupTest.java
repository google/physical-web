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

import java.util.Comparator;

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

  private static Comparator<PwPair> testComparator = new Comparator<PwPair>() {
    @Override
    public int compare(PwPair lhs, PwPair rhs) {
      return lhs.getUrlDevice().getId().compareTo(rhs.getUrlDevice().getId());
    }
  };

  public static PwPair createRankedPair(String id, String url, String groupId) {
    UrlDevice urlDevice = new UrlDevice(id, url);
    PwsResult pwsResult = new PwsResult.Builder(url, url)
        .setTitle("title1")
        .setDescription("description1")
        .setGroupId(groupId)
        .build();
    return new PwPair(urlDevice, pwsResult);
  }

  @Before
  public void setUp() {
    mPwPair1 = createRankedPair(ID1, URL1, GROUPID1);
    mPwPair2 = createRankedPair(ID2, URL2, GROUPID1);
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

    PwPair pwPair = urlGroup.getTopPair(testComparator);
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
    PwPair pwPair = urlGroup.getTopPair(testComparator);
    assertEquals(pwPair.getUrlDevice().getId(), ID1);
  }
}
