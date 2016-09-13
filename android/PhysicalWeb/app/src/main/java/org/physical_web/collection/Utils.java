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
 * Utility methods for the Physical Web library.
 */
class Utils {
  /**
   * Compare two nullable comparables, preferring any non-null value over null.
   * @param o1 first object to compare
   * @param o2 second object to compare
   * @param <T> a type that implements Comparable&lt;T&gt;
   * @return comparison value
   */
  public static <T extends Comparable<T>> int nullSafeCompare(T o1, T o2) {
    if (o1 == o2) {
      return 0;
    }
    if (o1 == null) {
      return -1;
    }
    if (o2 == null) {
      return 1;
    }
    return o1.compareTo(o2);
  }
}
