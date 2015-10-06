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

import java.util.HashMap;
import java.util.Map;

/**
 * Collection of Physical Web Objects and related metadata.
 */
public class PwoCollection {
  private Map<String, Pwo> mIdToPwoMap;
  private final String PWOS_KEY = "pwos";

  /**
   * Construct a PwoCollection.
   */
  public PwoCollection() {
    mIdToPwoMap = new HashMap<>();
  }

  /**
   * Add a PWO to the collection.
   * @param pwo The PWO to add.
   */
  public void addPwo(Pwo pwo) {
    mIdToPwoMap.put(pwo.getId(), pwo);
  }

  /**
   * Fetches a PWO by its ID.
   * @param id The ID of the PWO.
   * @return the PWO with the given ID.
   */
  public Pwo getPwoById(String id) {
    return mIdToPwoMap.get(id);
  }
}
