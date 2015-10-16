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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * A collection of similar URLs, the devices broadcasting those URLs, and associated metadata.
 */
public class UrlGroup implements Comparable<UrlGroup> {
  private String mGroupId;
  private PriorityQueue<PwPair> mPwPairs;

  /**
   * Construct a UrlGroup.
   * @param groupId The URL group ID of this group.
   */
  public UrlGroup(String groupId) {
    mGroupId = groupId;
    mPwPairs = new PriorityQueue<>(1, Collections.reverseOrder());
  }

  /**
   * Gets the group ID for this group.
   * @return Group ID
   */
  public String getGroupId() {
    return mGroupId;
  }

  /**
   * Add a PwPair to this group.
   * @param pwPair The PwPair to add.
   */
  public void addPair(PwPair pwPair) {
    mPwPairs.add(pwPair);
  }

  /**
   * Get the top-ranked PwPair in this group.
   * @return The top PwPair.
   */
  public PwPair getTopPair() {
    return mPwPairs.peek();
  }

  /**
   * Return a hash code for this UrlGroup.
   * @return hash code
   */
  @Override
  public int hashCode() {
    int hash = 1;
    hash = hash * 31 + ((mGroupId == null) ? 0 : mGroupId.hashCode());
    hash = hash * 31 + mPwPairs.hashCode();
    return hash;
  }

  /**
   * Check if two UrlGroups are equal.
   * @param other the UrlGroup to compare to.
   * @return true if the top pairs are of equal rank.
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (other instanceof UrlGroup) {
      UrlGroup otherUrlGroup = (UrlGroup) other;
      if (mPwPairs.size() == otherUrlGroup.mPwPairs.size() &&
          mGroupId.equals(otherUrlGroup.mGroupId)) {
        // don't consider order when comparing lists
        List<PwPair> myPairs = new ArrayList<>(mPwPairs);
        List<PwPair> otherPairs = new ArrayList<>(otherUrlGroup.mPwPairs);
        Collections.sort(myPairs);
        Collections.sort(otherPairs);
        return myPairs.equals(otherPairs);
      }
    }

    return false;
  }

  /**
   * Compare two UrlGroups based on the ranks of their top pairs.
   * Ties are broken by alphabetical comparison of groupid strings.
   * @param other the UrlGroup to compare to.
   * @return the comparison value.
   */
  @Override
  public int compareTo(UrlGroup other) {
    if (this == other) {
      return 0;
    }

    int compareValue = Utils.nullSafeCompare(getTopPair(), other.getTopPair());
    if (compareValue != 0) {
      return compareValue;
    }

    return Utils.nullSafeCompare(mGroupId, other.mGroupId);
  }

}
