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
 * An interface that defines objects that can serialize and deserialize
 * UrlDevices to and from JSON.
 * @param <T> The implementation of UrlDevice that this serializer is designed
 *            to serialize and deserialize.
 */
public interface UrlDeviceJsonSerializer<T extends UrlDevice> {
  /**
   * Creates a JSON object that represents the UrlDevice.
   * @param urlDevice The UrlDevice to serialize.
   * @return The JSON object representing the UrlDevice.
   */
  JSONObject serialize(T urlDevice);

  /**
   * Creates a UrlDevice represented by the supplied JSON object.
   * @param jsonObject The serialized UrlDevice.
   * @return The deserialized UrlDevice.
   */
  UrlDevice deserialize(JSONObject jsonObject);
}
