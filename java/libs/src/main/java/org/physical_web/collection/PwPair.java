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
   * @param urlDevice The URL device.
   * @param pwsResult The metadata returned by PWS for the URL broadcast by the device.
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
   * Return a hash code for this PwPair.
   * @return hash code
   */
  @Override
  public int hashCode() {
    int hash = 1;
    hash = hash * 31 + Double.valueOf(getRank()).hashCode();
    hash = hash * 31 + mUrlDevice.hashCode();
    hash = hash * 31 + mPwsResult.hashCode();
    return hash;
  }

  /**
   * Check if two PwPairs are equal.
   * @param other the PwPair to compare to.
   * @return true if the PwPairs are equal.
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (other instanceof PwPair) {
      PwPair otherPwPair = (PwPair) other;
      return getRank() == otherPwPair.getRank() &&
          mUrlDevice.equals(otherPwPair.mUrlDevice) &&
          mPwsResult.equals(otherPwPair.mPwsResult);
    }
    return false;
  }

  /**
   * Compare two PwPairs based on rank, breaking ties by comparing UrlDevice and PwsResult.
   * @param other the PwPair to compare to.
   * @return the comparison value.
   */
  @Override
  public int compareTo(PwPair other) {
    if (this == other) {
      return 0;
    }

    int compareValue = Double.compare(getRank(), other.getRank());
    if (compareValue != 0) {
      return compareValue;
    }

    compareValue = mUrlDevice.compareTo(other.mUrlDevice);
    if (compareValue != 0) {
      return compareValue;
    }

    return mPwsResult.compareTo(other.mPwsResult);
  }
}
