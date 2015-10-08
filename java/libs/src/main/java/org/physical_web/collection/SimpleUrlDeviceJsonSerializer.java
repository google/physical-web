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
 * A simple UrlDevice serializer that only worries about core properties.
 */
public class SimpleUrlDeviceJsonSerializer implements UrlDeviceJsonSerializer<UrlDevice> {
  private static final String ID_KEY = "id";
  private static final String URL_KEY = "url";

  /**
   * Creates a JSON object that represents the UrlDevice.
   * @param urlDevice The UrlDevice to serialize.
   * @return The JSON object representing the UrlDevice.
   */
  public JSONObject serialize(UrlDevice urlDevice) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(ID_KEY, urlDevice.getId());
    jsonObject.put(URL_KEY, urlDevice.getUrl());
    return jsonObject;
  }

  /**
   * Creates a UrlDevice represented by the supplied JSON object.
   * @param jsonObject The serialized UrlDevice.
   * @return The deserialized UrlDevice.
   */
  public SimpleUrlDevice deserialize(JSONObject jsonObject) {
    String id = jsonObject.getString("id");
    String url = jsonObject.getString("url");
    return new SimpleUrlDevice(id, url);
  }
}
