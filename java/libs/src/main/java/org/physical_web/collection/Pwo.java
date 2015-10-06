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

import org.json.JSONObject;

/**
 * The interface defining a PWO (Physical Web Object).
 */
public interface Pwo {
  /**
   * Fetches the ID of the PWO.
   * The ID should be unique across PWOs.  This should even be the case when
   * one real world device is broadcasting multiple URLs.
   * @return The ID of the PWO.
   */
  String getId();

  /**
   * Fetches the URL broadcasted by the PWO.
   * @return The broadcasted URL.
   */
  String getUrl();

  /**
   * Creates a JSON object that represents the PWO.
   * This will only be used in serialization and deserialization.
   * PWOs that are not anticipated to be serialized or deserialized may just
   * return null.
   * @return The JSON object representing the PWO.
   */
  JSONObject toJsonObject();
}
