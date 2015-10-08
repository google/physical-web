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

/**
 * A physical web pair represents a UrlDevice and its corresponding PwsResult.
 */
public class PwPair implements Comparable<PwPair> {
  private final UrlDevice mUrlDevice;
  private final PwsResult mPwsResult;

  /**
   * Construct a PwPair.
   */
  public PwPair(UrlDevice urlDevice, PwsResult pwsResult) {
    mUrlDevice = urlDevice;
    mPwsResult = pwsResult;
  }

  /**
   * Get the rank for the UrlDevice/PwsResult pair.
   * @return the rank.
   */
  public double getRank() {
    return mUrlDevice.getRank(mPwsResult);
  }

  /**
   * Get the UrlDevice represented in the pair.
   * @return the UrlDevice.
   */
  public UrlDevice getUrlDevice() {
    return mUrlDevice;
  }

  /**
   * Get the PwsResult represented in the pair.
   * @return the PwsResult.
   */
  public PwsResult getPwsResult() {
    return mPwsResult;
  }

  /**
   * Unimplemented hash code method.
   * @return 42.
   */
  public int hashCode() {
    assert false : "hashCode not designed";
    return 42;
  }

  /**
   * Check if two PwPairs are equal based on rank.
   * @param other the PwPair to compare to.
   * @return true if the ranks are equal.
   */
  public boolean equals(Object other) {
    if (other instanceof PwPair) {
      PwPair otherPwPair = (PwPair) other;
      return getRank() == otherPwPair.getRank();
    }
    return false;
  }

  /**
   * Compare two PwPairs based on rank.
   * @param other the PwPair to compare to.
   * @return the comparison value.
   */
  public int compareTo(PwPair other) {
    return new Double(getRank()).compareTo(other.getRank());
  }
}
