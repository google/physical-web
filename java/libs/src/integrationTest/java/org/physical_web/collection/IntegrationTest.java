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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * PhysicalWebCollection integration class.
 */
public class IntegrationTest {
  private PhysicalWebCollection physicalWebCollection;
  private static Comparator<PwPair> testComparator = new Comparator<PwPair>() {
    @Override
    public int compare(PwPair lhs, PwPair rhs) {
      return lhs.getUrlDevice().getId().compareTo(rhs.getUrlDevice().getId());
    }
  };

  private class FetchPwsResultsTask {
    private int mNumExpected;
    private int mNumFound;
    private int mNumMissing;
    private Semaphore mMutex;
    private Exception mErr;

    public FetchPwsResultsTask(int numExpected) {
      mNumExpected = numExpected;
      mNumFound = 0;
      mNumMissing = 0;
      mMutex = new Semaphore(1);
      mErr = null;
    }

    private void testDone() {
      if (mNumFound + mNumMissing == mNumExpected) {
        mMutex.release();
      }
    }

    private void addFound() {
      mNumFound += 1;
      testDone();
    }

    private void addMissing() {
      mNumMissing += 1;
      testDone();
    }

    private void setError(Exception e) {
      mErr = e;
      mMutex.release();
    }

    public boolean run() throws InterruptedException {
      final int numExpected = mNumExpected;
      PwsResultCallback pwsResultCallback = new PwsResultCallback() {
        public void onPwsResult(PwsResult pwsResult) {
          addFound();
        }

        public void onPwsResultAbsent(String url) {
          addMissing();
        }

        public void onPwsResultError(Collection<String> urls, int httpResponseCode, Exception e) {
          setError(e);
        }
      };
      PwsResultIconCallback pwsResultIconCallback = new PwsResultIconCallback() {
        public void onIcon(byte[] icon) {
          addFound();
        }

        public void onError(int httpResponseCode, Exception e) {
          setError(e);
        }
      };

      mMutex.acquire();
      physicalWebCollection.fetchPwsResults(pwsResultCallback, pwsResultIconCallback);

      if (mMutex.tryAcquire(3, TimeUnit.SECONDS)) {
        mMutex.release();
        return true;
      }
      return false;
    }

    public Exception getException() {
      return mErr;
    }
  }

  @Before
  public void setUp() {
    physicalWebCollection = new PhysicalWebCollection();
  }

  @Test
  public void resolveSomeUrls() throws InterruptedException {
    physicalWebCollection.addUrlDevice(new UrlDevice("id1", "https://google.com"));
    physicalWebCollection.addUrlDevice(new UrlDevice("id2", "https://goo.gl/mo6YnG"));
    FetchPwsResultsTask task = new FetchPwsResultsTask(4);
    assertTrue(task.run());
    assertNull(task.getException());
    List<PwPair> pwPairs = physicalWebCollection.getPwPairsSortedByRank(testComparator);
    assertEquals(2, pwPairs.size());
    assertEquals("https://www.google.com/", pwPairs.get(0).getPwsResult().getSiteUrl());
    assertEquals("https://github.com/google/physical-web",
                 pwPairs.get(1).getPwsResult().getSiteUrl());
  }
}
