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
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * PhysicalWebCollection integration class.
 */
public class IntegrationTest {
  private PhysicalWebCollection physicalWebCollection;

  private class FetchPwsResultsTask {
    private int mNumExpected;
    private Semaphore mMutex;
    private Exception mErr;

    public FetchPwsResultsTask(int numExpected) {
      mNumExpected = numExpected;
      mMutex = new Semaphore(1);
      mErr = null;
    }

    public boolean run() throws InterruptedException {
      final int numExpected = mNumExpected;
      PwsResultCallback pwsResultCallback = new PwsResultCallback() {
        int mNumMissing = 0;

        private void testDone() {
          if (physicalWebCollection.getPwPairsSortedByRank().size() + mNumMissing == numExpected) {
            mMutex.release();
          }
        }

        public void onPwsResult(PwsResult pwsResult) {
          testDone();
        }

        public void onPwsResultAbsent(String url) {
          mNumMissing += 1;
          testDone();
        }

        public void onPwsResultError(Collection<String> urls, int httpResponseCode, Exception e) {
          mErr = e;
          mMutex.release();
        }
      };

      mMutex.acquire();
      physicalWebCollection.fetchPwsResults(pwsResultCallback);

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
    physicalWebCollection.addUrlDevice(new RankedDevice("id1", "https://google.com", .5));
    physicalWebCollection.addUrlDevice(new RankedDevice("id2", "https://goo.gl/mo6YnG", .2));
    FetchPwsResultsTask task = new FetchPwsResultsTask(2);
    assertTrue(task.run());
    assertNull(task.getException());
    List<PwPair> pwPairs = physicalWebCollection.getPwPairsSortedByRank();
    assertEquals(2, pwPairs.size());
    assertEquals("https://www.google.com/", pwPairs.get(0).getPwsResult().getSiteUrl());
    assertEquals("https://github.com/google/physical-web",
                 pwPairs.get(1).getPwsResult().getSiteUrl());
  }
}
