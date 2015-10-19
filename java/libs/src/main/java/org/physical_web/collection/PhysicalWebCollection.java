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
import java.util.Collection;
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
  private static final String GROUPID_KEY = "groupid";
  private static final String DEFAULT_PWS_ENDPOINT = "https://url-caster.appspot.com";
  private PwsClient mPwsClient;
  private Map<String, UrlDevice> mDeviceIdToUrlDeviceMap;
  private Map<Class, UrlDeviceJsonSerializer> mUrlDeviceTypeToUrlDeviceJsonSerializer;
  private Map<String, PwsResult> mBroadcastUrlToPwsResultMap;
  private Set<String> mPendingBroadcastUrls;

  /**
   * Construct a PhysicalWebCollection.
   */
  public PhysicalWebCollection() {
    mPwsClient = new PwsClient(DEFAULT_PWS_ENDPOINT);
    mDeviceIdToUrlDeviceMap = new HashMap<>();
    mUrlDeviceTypeToUrlDeviceJsonSerializer = new HashMap<>();
    mBroadcastUrlToPwsResultMap = new HashMap<>();
    mPendingBroadcastUrls = new HashSet<>();
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
   * @param <T> the subclass of UrlDevice that the serializer will deserialize to.
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

    String groupId = pwsResult.getGroupId();
    if (groupId != null && !groupId.equals("")) {
      jsonObject.put(GROUPID_KEY, groupId);
    }

    return jsonObject;
  }

  /**
   * Create a JSON object that represents this data structure.
   * @return a JSON serialization of this data structure.
   * @throws PhysicalWebCollectionException on invalid or unrecognized input
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
    String groupId = null;
    if (jsonObject.has(GROUPID_KEY)) {
      groupId = jsonObject.getString(GROUPID_KEY);
    }
    return new PwsResult(requestUrl, siteUrl, groupId);
  }

  /**
   * Populate this data structure with UrlDevices represented by a given JSON object.
   * @param jsonObject a serialized PhysicalWebCollection.
   * @throws PhysicalWebCollectionException on invalid or unrecognized input
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
    List<PwPair> allPwPairs = getPwPairs();

    // Sort the list in descending order.
    Collections.sort(allPwPairs, Collections.reverseOrder());

    // Filter the list.
    return removeDuplicateSiteUrls(allPwPairs);
  }

  /**
   * Return a list of PwPairs sorted by rank in descending order, including only the top-ranked
   * pair from each group.
   * @return a sorted list of PwPairs.
   */
  public List<PwPair> getGroupedPwPairsSortedByRank() {
    // Get all valid PwPairs.
    List<PwPair> allPwPairs = getPwPairs();

    // Group pairs with the same groupId, keeping only the top-ranked PwPair.
    List<PwPair> groupedPwPairs = removeDuplicateGroupIds(allPwPairs, null);

    // Sort by descending rank.
    Collections.sort(groupedPwPairs, Collections.reverseOrder());

    // Remove duplicate site URLs.
    return removeDuplicateSiteUrls(groupedPwPairs);
  }

  /**
   * Return a list of all pairs of valid URL devices and corresponding URL metadata.
   * @return list of PwPairs.
   */
  private List<PwPair> getPwPairs() {
    List<PwPair> allPwPairs = new ArrayList<>();
    for (UrlDevice urlDevice : mDeviceIdToUrlDeviceMap.values()) {
      PwsResult pwsResult = mBroadcastUrlToPwsResultMap.get(urlDevice.getUrl());
      if (pwsResult != null) {
        allPwPairs.add(new PwPair(urlDevice, pwsResult));
      }
    }
    return allPwPairs;
  }

  /**
   * If a site URL appears multiple times in the pairs list, keep only the first example.
   * @param allPwPairs input PwPairs list.
   * @return filtered PwPairs list with all duplicated site URLs removed.
   */
  private static List<PwPair> removeDuplicateSiteUrls(List<PwPair> allPwPairs) {
    List<PwPair> filteredPwPairs = new ArrayList<>();
    Set<String> siteUrls = new HashSet<>();
    for (PwPair pwPair : allPwPairs) {
      String siteUrl = pwPair.getPwsResult().getSiteUrl();
      if (!siteUrls.contains(siteUrl)) {
        siteUrls.add(siteUrl);
        filteredPwPairs.add(pwPair);
      }
    }
    return filteredPwPairs;
  }

  /**
   * Given a list of PwPairs, return a filtered list such that only one PwPair from each group
   * is included.
   * @param allPairs Input PwPairs list.
   * @param outGroupMap Optional output map from discovered group IDs to UrlGroups, may be null.
   * @return Filtered PwPairs list.
   */
  private static List<PwPair> removeDuplicateGroupIds(List<PwPair> allPairs,
                                                      Map<String, UrlGroup> outGroupMap) {
    List<PwPair> filteredPairs = new ArrayList<>();
    Map<String, UrlGroup> groupMap = outGroupMap;
    if (groupMap == null) {
      groupMap = new HashMap<>();
    } else {
      groupMap.clear();
    }

    for (PwPair pwPair : allPairs) {
      PwsResult pwsResult = pwPair.getPwsResult();
      String groupId = pwsResult.getGroupId();
      if (groupId == null || groupId.equals("")) {
        // Pairs without a group are always included
        filteredPairs.add(pwPair);
      } else {
        // Create the group if it doesn't exist
        UrlGroup urlGroup = groupMap.get(groupId);
        if (urlGroup == null) {
          urlGroup = new UrlGroup(groupId);
          groupMap.put(groupId, urlGroup);
        }
        urlGroup.addPair(pwPair);
      }
    }

    for (UrlGroup urlGroup : groupMap.values()) {
      filteredPairs.add(urlGroup.getTopPair());
    }

    return filteredPairs;
  }

  /**
   * Set the URL for making PWS requests.
   * @param pwsEndpoint The new PWS endpoint.
   */
  public void setPwsEndpoint(String pwsEndpoint) {
    mPwsClient.setPwsEndpoint(pwsEndpoint);
  }

  /**
   * Triggers an HTTP request to be made to the PWS.
   * This method fetches a PwsResult for all broadcast URLs that do not have a
   * PwsResult.
   * @param pwsResultCallback The callback to run when we get an HTTPResponse.
   */
  public void fetchPwsResults(final PwsResultCallback pwsResultCallback) {
    // Get new URLs to fetch.
    Set<String> newUrls = new HashSet<>();
    for (UrlDevice urlDevice : mDeviceIdToUrlDeviceMap.values()) {
      String url = urlDevice.getUrl();
      if (!mPendingBroadcastUrls.contains(url)
          && !mBroadcastUrlToPwsResultMap.containsKey(url)) {
        newUrls.add(url);
        mPendingBroadcastUrls.add(url);
      }
    }

    // Make the request.
    PwsResultCallback augmentedCallback = new PwsResultCallback() {
      public void onPwsResult(PwsResult pwsResult) {
        mPendingBroadcastUrls.remove(pwsResult.getRequestUrl());
        addMetadata(pwsResult);
        pwsResultCallback.onPwsResult(pwsResult);
      }

      public void onPwsResultAbsent(String url) {
        mPendingBroadcastUrls.remove(url);
        pwsResultCallback.onPwsResultAbsent(url);
      }

      public void onPwsResultError(Collection<String> urls, int httpResponseCode, Exception e) {
        for (String url : urls) {
          mPendingBroadcastUrls.remove(url);
        }
        pwsResultCallback.onPwsResultError(urls, httpResponseCode, e);
      }
    };
    if (newUrls.size() > 0) {
      mPwsClient.resolve(newUrls, augmentedCallback);
    }
  }


  /**
   * Cancel all current HTTP requests.
   */
  public void cancelAllRequests() {
    mPwsClient.cancelAllRequests();
  }
}
