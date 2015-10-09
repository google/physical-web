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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collection of Physical Web URL devices and related metadata.
 */
public class PhysicalWebCollection {
  private static final int SCHEMA_VERSION = 1;
  private static final String SCHEMA_VERSION_KEY = "schema";
  private static final String DEVICES_KEY = "devices";
  private static final String TYPE_KEY = "type";
  private static final String DATA_KEY = "data";
  private static final String METADATA_KEY = "metadata";
  private static final String REQUESTURL_KEY = "requesturl";
  private static final String SITEURL_KEY = "siteurl";
  private Map<String, UrlDevice> mDeviceIdToUrlDeviceMap;
  private Map<Class, UrlDeviceJsonSerializer> mUrlDeviceTypeToUrlDeviceJsonSerializer;
  private Map<String, PwsResult> mBroadcastUrlToPwsResultMap;

  /**
   * Construct a PhysicalWebCollection.
   */
  public PhysicalWebCollection() {
    mDeviceIdToUrlDeviceMap = new HashMap<>();
    mUrlDeviceTypeToUrlDeviceJsonSerializer = new HashMap<>();
    mBroadcastUrlToPwsResultMap = new HashMap<>();
  }

  /**
   * Add a UrlDevice to the collection.
   * @param urlDevice The UrlDevice to add.
   */
  public void addUrlDevice(UrlDevice urlDevice) {
    mDeviceIdToUrlDeviceMap.put(urlDevice.getId(), urlDevice);
  }

  /**
   * Add URL metadata to the collection.
   * @param pwsResult The PwsResult to add.
   */
  public void addMetadata(PwsResult pwsResult) {
    mBroadcastUrlToPwsResultMap.put(pwsResult.getRequestUrl(), pwsResult);
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
   * Fetches cached URL metadata using the URL broadcasted by the Physical Web device.
   * @param broadcastUrl The URL broadcasted by the device.
   * @return Cached metadata relevant to the given URL.
   */
  public PwsResult getMetadataByBroadcastUrl(String broadcastUrl) {
    return mBroadcastUrlToPwsResultMap.get(broadcastUrl);
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

  private JSONObject jsonSerializePwsResult(PwsResult pwsResult) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(REQUESTURL_KEY, pwsResult.getRequestUrl());
    jsonObject.put(SITEURL_KEY, pwsResult.getSiteUrl());
    return jsonObject;
  }

  /**
   * Create a JSON object that represents this data structure.
   * @return a JSON serialization of this data structure.
   */
  public JSONObject jsonSerialize() throws PhysicalWebCollectionException {
    JSONObject jsonObject = new JSONObject();

    // Serialize the UrlDevices
    JSONArray urlDevices = new JSONArray();
    for (UrlDevice urlDevice : mDeviceIdToUrlDeviceMap.values()) {
      urlDevices.put(jsonSerializeUrlDevice(urlDevice));
    }
    jsonObject.put(DEVICES_KEY, urlDevices);

    // Serialize the URL metadata
    JSONArray metadata = new JSONArray();
    for (PwsResult pwsResult : mBroadcastUrlToPwsResultMap.values()) {
      metadata.put(jsonSerializePwsResult(pwsResult));
    }
    jsonObject.put(METADATA_KEY, metadata);

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

  private PwsResult jsonDeserializePwsResult(JSONObject jsonObject) {
    String requestUrl = jsonObject.getString(REQUESTURL_KEY);
    String siteUrl = jsonObject.getString(SITEURL_KEY);
    return new PwsResult(requestUrl, siteUrl);
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

    // Deserialize the URL metadata
    JSONArray metadata = jsonObject.getJSONArray(METADATA_KEY);
    for (int i = 0; i < metadata.length(); i++) {
      JSONObject metadataPair = metadata.getJSONObject(i);
      PwsResult pwsResult = jsonDeserializePwsResult(metadataPair);
      addMetadata(pwsResult);
    }
  }

  /**
   * Return a list of PwPairs sorted by rank in descending order.
   * These PwPairs will be deduplicated by siteUrls (favoring the PwPair with
   * the highest rank).
   * @return a sorted list of PwPairs.
   */
  public List<PwPair> getPwPairsSortedByRank() {
    // Get all valid PwPairs.
    List<PwPair> allPwPairs = new ArrayList<>();
    for (UrlDevice urlDevice : mDeviceIdToUrlDeviceMap.values()) {
      PwsResult pwsResult = mBroadcastUrlToPwsResultMap.get(urlDevice.getUrl());
      if (pwsResult != null) {
        allPwPairs.add(new PwPair(urlDevice, pwsResult));
      }
    }

    // Sort the list in descending order.
    Collections.sort(allPwPairs, Collections.reverseOrder());

    // Filter the list.
    List<PwPair> ret = new ArrayList<>();
    Set<String> siteUrls = new HashSet<>();
    for (PwPair pwPair : allPwPairs) {
      String siteUrl = pwPair.getPwsResult().getSiteUrl();
      if (!siteUrls.contains(siteUrl)) {
        siteUrls.add(siteUrl);
        ret.add(pwPair);
      }
    }

    return ret;
  }
}
