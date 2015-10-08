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

import org.json.JSONObject;

import org.junit.Test;

import org.skyscreamer.jsonassert.JSONAssert;

/**
 * SimpleUrlDeviceJsonSerializer unit test class.
 */
public class SimpleUrlDeviceJsonSerializerTest {
  private static final String ID1 = "id1";
  private static final String URL1 = "http://example.com";

  @Test
  public void serializeWorks() {
    SimpleUrlDevice simpleUrlDevice = new SimpleUrlDevice(ID1, URL1);
    SimpleUrlDeviceJsonSerializer simpleUrlDeviceJsonSerializer =
        new SimpleUrlDeviceJsonSerializer();
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("id", ID1);
    jsonObject.put("url", URL1);
    JSONAssert.assertEquals(simpleUrlDeviceJsonSerializer.serialize(simpleUrlDevice), jsonObject,
                            true);
  }

  @Test
  public void deserializeWorks() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("id", ID1);
    jsonObject.put("url", URL1);
    SimpleUrlDeviceJsonSerializer simpleUrlDeviceJsonSerializer =
        new SimpleUrlDeviceJsonSerializer();
    SimpleUrlDevice simpleUrlDevice = simpleUrlDeviceJsonSerializer.deserialize(jsonObject);
    assertEquals(simpleUrlDevice.getId(), ID1);
    assertEquals(simpleUrlDevice.getUrl(), URL1);
  }
}
