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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Collection of Physical Web URL devices and related metadata.
 */
public class PhysicalWebCollection {
  private final int SCHEMA_VERSION = 1;
  private final String SCHEMA_VERSION_KEY = "schema";
  private final String DEVICES_KEY = "devices";
  private final String TYPE_KEY = "type";
  private final String DATA_KEY = "data";
  private Map<String, UrlDevice> mDeviceIdToUrlDeviceMap;
  private Map<Class, UrlDeviceJsonSerializer> mUrlDeviceTypeToUrlDeviceJsonSerializer;

  /**
   * Construct a PhysicalWebCollection.
   */
  public PhysicalWebCollection() {
    mDeviceIdToUrlDeviceMap = new HashMap<>();
    mUrlDeviceTypeToUrlDeviceJsonSerializer = new HashMap<>();
  }

  /**
   * Add a UrlDevice to the collection.
   * @param urlDevice The UrlDevice to add.
   */
  public void addUrlDevice(UrlDevice urlDevice) {
    mDeviceIdToUrlDeviceMap.put(urlDevice.getId(), urlDevice);
  }

  /**
   * Fetches a UrlDevice by its ID.
   * @param id The ID of the UrlDevice.
   * @return the UrlDevice with the given ID.
   */
  public UrlDevice getUrlDeviceById(String id) {
    return mDeviceIdToUrlDeviceMap.get(id);
  }

  /**
   * Add a UrlDeviceJsonSerializer to be associated with a particular class.
   * @param urlDeviceType the class UrlDevices to serialize and deserialize with this
   *        serializer.
   * @param urlDeviceJsonSerializer the serializer to use in serializing UrlDevices.
   */
  public <T extends UrlDevice> void addUrlDeviceJsonSerializer(
      Class<? extends T> urlDeviceType,
      UrlDeviceJsonSerializer<T> urlDeviceJsonSerializer) {
    mUrlDeviceTypeToUrlDeviceJsonSerializer.put(urlDeviceType, urlDeviceJsonSerializer);
  }

  @SuppressWarnings("unchecked")
  private JSONObject jsonSerializeUrlDevice(UrlDevice urlDevice)
      throws PhysicalWebCollectionException {
    // Loop through the superclasses of urlDevice to see which serializer we should use.
    for (Class urlDeviceType = urlDevice.getClass();
         UrlDevice.class.isAssignableFrom(urlDeviceType);
         urlDeviceType = urlDeviceType.getSuperclass()) {
      UrlDeviceJsonSerializer urlDeviceJsonSerializer =
          mUrlDeviceTypeToUrlDeviceJsonSerializer.get(urlDeviceType);
      UrlDeviceJsonSerializer<UrlDevice> specificUrlDeviceJsonSerializer =
          (UrlDeviceJsonSerializer<UrlDevice>) urlDeviceJsonSerializer;
      if (urlDeviceJsonSerializer != null) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(TYPE_KEY, urlDevice.getClass().getName());
        jsonObject.put(DATA_KEY, specificUrlDeviceJsonSerializer.serialize(urlDevice));
        return jsonObject;
      }
    }
    throw new PhysicalWebCollectionException(
        "No suitable UrlDeviceJsonSerializer found for " + urlDevice.getClass().getName());
  }

  /**
   * Create a JSON object that represents this data structure.
   * @return a JSON serialization of this data structure.
   */
  public JSONObject jsonSerialize() throws PhysicalWebCollectionException {
    JSONObject jsonObject = new JSONObject();
    JSONArray urlDevices = new JSONArray();
    for (UrlDevice urlDevice : mDeviceIdToUrlDeviceMap.values()) {
      urlDevices.put(jsonSerializeUrlDevice(urlDevice));
    }
    jsonObject.put(DEVICES_KEY, urlDevices);
    jsonObject.put(SCHEMA_VERSION_KEY, SCHEMA_VERSION);
    return jsonObject;
  }

  @SuppressWarnings("unchecked")
  private UrlDevice jsonDeserializeUrlDevice(JSONObject jsonObject)
      throws PhysicalWebCollectionException {
    // Get the type and raw data out of the json object.
    JSONObject dataJson = jsonObject.getJSONObject(DATA_KEY);
    String typeName = jsonObject.getString(TYPE_KEY);
    Class urlDeviceType = null;
    try {
      urlDeviceType = Class.forName(typeName);
    } catch (ClassNotFoundException e) {
      throw new PhysicalWebCollectionException(
          "No suitable UrlDeviceJsonSerializer found for " + typeName);
    }

    // Loop through the superclasses of urlDevice to see which serializer we should use.
    for (;
         UrlDevice.class.isAssignableFrom(urlDeviceType);
         urlDeviceType = urlDeviceType.getSuperclass()) {
      UrlDeviceJsonSerializer urlDeviceJsonSerializer =
          mUrlDeviceTypeToUrlDeviceJsonSerializer.get(urlDeviceType);
      UrlDeviceJsonSerializer<UrlDevice> specificUrlDeviceJsonSerializer =
          (UrlDeviceJsonSerializer<UrlDevice>) urlDeviceJsonSerializer;
      if (urlDeviceJsonSerializer != null) {
        return specificUrlDeviceJsonSerializer.deserialize(dataJson);
      }
    }
    throw new PhysicalWebCollectionException(
        "No suitable UrlDeviceJsonSerializer found for " + typeName);
  }

  /**
   * Populate this data structure with UrlDevices represented by a given JSON object.
   * @param jsonObject a serialized PhysicalWebCollection.
   */
  public void jsonDeserialize(JSONObject jsonObject) throws PhysicalWebCollectionException {
    // Check the schema version
    int schemaVersion = jsonObject.getInt(SCHEMA_VERSION_KEY);
    if (schemaVersion > SCHEMA_VERSION) {
      throw new PhysicalWebCollectionException(
          "Cannot handle schema version " + schemaVersion + ".  "
          + "This library only knows of schema version " + SCHEMA_VERSION);
    }

    // Deserialize the UrlDevices
    JSONArray urlDevices = jsonObject.getJSONArray(DEVICES_KEY);
    for (int i = 0; i < urlDevices.length(); i++) {
      JSONObject urlDeviceJson = urlDevices.getJSONObject(i);
      UrlDevice urlDevice = jsonDeserializeUrlDevice(urlDeviceJson);
      addUrlDevice(urlDevice);
    }
  }
}
