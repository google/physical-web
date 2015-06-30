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

package org.physical_web.physicalweb;

import java.util.Date;

abstract class PwoDiscoverer {

  private PwoDiscoveryCallback mPwoDiscoveryCallback;
  private long mScanStartTime;

  public abstract void startScanImpl();
  public abstract void stopScanImpl();

  public void startScan() {
    mScanStartTime = new Date().getTime();
    startScanImpl();
  }

  public void stopScan() {
    stopScanImpl();
  }

  public void setCallback(PwoDiscoveryCallback pwoDiscoveryCallback) {
    mPwoDiscoveryCallback = pwoDiscoveryCallback;
  }

  protected PwoMetadata createPwoMetadata(String url) {
    PwoMetadata pwoMetadata = new PwoMetadata(url, new Date().getTime() - mScanStartTime);
    return pwoMetadata;
  }

  protected void reportPwo(PwoMetadata pwoMetadata) {
    mPwoDiscoveryCallback.onPwoDiscovered(pwoMetadata);
  }

  public interface PwoDiscoveryCallback {
    public void onPwoDiscovered(PwoMetadata pwoMetadata);
  }
}
