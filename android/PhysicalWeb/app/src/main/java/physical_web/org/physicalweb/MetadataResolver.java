/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package physical_web.org.physicalweb;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.uribeacon.scan.compat.ScanResult;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class for resolving metadata.
 * Batches up found device urls,
 * and sends them to the metadata server
 * which then scrapes the given pages
 * at the give nurls for the metadata.
 */

public class MetadataResolver {
  private static String TAG = "MetadataResolver";
  private static Activity mActivity;
  private static String METADATA_URL = "http://url-caster.appspot.com/resolve-scan";
  private static RequestQueue mRequestQueue;
  private static boolean mIsInitialized = false;
  private static boolean mIsQueuing = false;
  private static Handler mQueryHandler;
  private static ArrayList<String> mDeviceBatchList;
  private static int QUERY_PERIOD = 500;
  private static MetadataResolverCallback mMetadataResolverCallback;

  public MetadataResolver(Activity activity) {
    initialize(activity);
  }

  public static void initialize(Context context) {
    if (mRequestQueue == null) {
      mRequestQueue = Volley.newRequestQueue(context);
    }
    mIsInitialized = true;
    if (mQueryHandler == null) {
      mQueryHandler = new Handler();
    }
    if (mDeviceBatchList == null) {
      mDeviceBatchList = new ArrayList<>();
    }
  }


  /////////////////////////////////
  // callbacks
  /////////////////////////////////

  public interface MetadataResolverCallback {
    public void onDeviceMetadataReceived(String url, DeviceMetadata deviceMetadata);
  }

  /**
   * Called when a device's metadata has been fetched and returned.
   *
   * @param url
   * @param deviceMetadata
   */
  private static void onDeviceMetadataReceived(String url, DeviceMetadata deviceMetadata) {
    // Callback to the context that made the request
    mMetadataResolverCallback.onDeviceMetadataReceived(url, deviceMetadata);
  }


  /////////////////////////////////
  // utilities
  /////////////////////////////////

  public static void findDeviceMetadata(final Context context, final MetadataResolverCallback metadataResolverCallback, final String url) {
    // Store the context
    mActivity = (Activity) context;
    // Store the callback so we can call it back later
    mMetadataResolverCallback = metadataResolverCallback;
    mActivity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        initialize(context);
        // If we're not currently queuing up
        // urls to fetch metadata for
        //if (!mIsQueuing) {
        //  mIsQueuing = true;
          // We wait QUERY_PERIOD ms to see if any other devices are discovered so we can batch.
       //   mQueryHandler.postAtTime(mBatchMetadataRunnable, QUERY_PERIOD);
      //  }
        // Add the device to the queue of devices to look for.
        mDeviceBatchList.add(url);
        batchFetchMetaData();
      }
    });
  }

  /**
   * Create placeholder metadata to load immediately
   * while the actual metadata is being fetched.
   * This will trigger a ui update and the user
   * will be able to see the device url in the list view.
   *
   * @param mDeviceBatchList
   */
  private static void loadInitialMetadata(ArrayList<String> mDeviceBatchList) {
    if (!mIsInitialized) {
      Log.e(TAG, "Not initialized.");
      return;
    }

    for (int i = 0; i < mDeviceBatchList.size(); i++) {
      String url = mDeviceBatchList.get(i);
      DeviceMetadata deviceMetadata = new DeviceMetadata();
      deviceMetadata.title = "";
      deviceMetadata.description = "";
      deviceMetadata.siteUrl = url;
      deviceMetadata.iconUrl = "";
      onDeviceMetadataReceived(url, deviceMetadata);
    }
  }

  /**
   * Start the process that will ask
   * the metadata server for metadata
   * for each of the urls in the request.
   * This method creates the request object
   * and adds it to a queue for subsequent processing
   * some time later.
   *
   * @param mDeviceBatchList
   */
  public static void getBatchMetadata(ArrayList<String> mDeviceBatchList) {
    if (!mIsInitialized) {
      Log.e(TAG, "Not initialized.");
      return;
    }

    // Create the json request object
    JSONObject jsonObj = createRequestObject(mDeviceBatchList);

    // Create the metadata request
    // for the given json request object and device map
    JsonObjectRequest jsObjRequest = createMetadataRequest(jsonObj);

    // Queue the request
    mRequestQueue.add(jsObjRequest);
  }

  private static String ensureUrlHasHttpPrefix(String url) {
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
      url = "http://" + url;
    }
    return url;
  }

  /**
   * Create the metadata request, given
   * the json request object and device map
   *
   * @param jsonObj
   * @return
   */
  private static JsonObjectRequest createMetadataRequest(JSONObject jsonObj) {
    return new JsonObjectRequest(
        METADATA_URL,
        jsonObj,
        new Response.Listener<JSONObject>() {
          // called when the server returns a response
          @Override
          public void onResponse(JSONObject jsonResponse) {

            // build the metadata from the response
            try {
              JSONArray foundMetaData = jsonResponse.getJSONArray("metadata");

              int deviceCount = foundMetaData.length();
              for (int i = 0; i < deviceCount; i++) {

                JSONObject deviceData = foundMetaData.getJSONObject(i);

                String title = "Unknown name";
                String url = "Unknown url";
                String description = "Unknown description";
                String iconUrl = "/favicon.ico";
                String id = deviceData.getString("id");

                if (deviceData.has("title")) {
                  title = deviceData.getString("title");
                }
                if (deviceData.has("url")) {
                  url = deviceData.getString("url");
                }
                if (deviceData.has("description")) {
                  description = deviceData.getString("description");
                }
                if (deviceData.has("icon")) {
                  // We might need to do some magic here.
                  iconUrl = deviceData.getString("icon");
                }

                // TODO: Eliminate this fallback since we expect the server to always return an icon.
                // Provisions for a favicon specified as a relative URL.
                if (!iconUrl.startsWith("http")) {
                  // Lets just assume we are dealing with a relative path.
                  Uri fullUri = Uri.parse(url);
                  Uri.Builder builder = fullUri.buildUpon();
                  // Append the default favicon path to the URL.
                  builder.path(iconUrl);
                  iconUrl = builder.toString();
                }

                DeviceMetadata deviceMetadata = new DeviceMetadata();
                deviceMetadata.title = title;
                deviceMetadata.description = description;
                deviceMetadata.siteUrl = url;
                deviceMetadata.iconUrl = iconUrl;
                downloadIcon(deviceMetadata, url);

                if ("https://maps.google.com".equals(url)) {
                  Log.d(TAG, "Log");
                }

                onDeviceMetadataReceived(id, deviceMetadata);
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        },
        new Response.ErrorListener() {

          @Override
          public void onErrorResponse(VolleyError volleyError) {
            Log.i(TAG, "VolleyError: " + volleyError.toString());
          }
        }
    );
  }

  /**
   * Create the json request object
   * that will be sent to the metadata server
   * asking for metadata for each device's url.
   *
   * @param urls
   * @return
   */
  private static JSONObject createRequestObject(ArrayList<String> urls) {
    JSONObject jsonObj = new JSONObject();
    try {
      JSONArray urlArray = new JSONArray();
      for (int i = 0; i < urls.size(); i++) {
        String url = urls.get(i);
        JSONObject urlObject = new JSONObject();
        urlObject.put("url", url);
        urlArray.put(urlObject);
      }
      jsonObj.put("objects", urlArray);
    } catch (JSONException ex) {

    }
    return jsonObj;
  }

  /**
   * Asynchronously download the image for the device.
   *
   * @param deviceMetadata
   * @param url
   */
  private static void downloadIcon(final DeviceMetadata deviceMetadata, final String url) {
    ImageRequest imageRequest = new ImageRequest(deviceMetadata.iconUrl, new Response.Listener<Bitmap>() {
      @Override
      public void onResponse(Bitmap response) {
        deviceMetadata.icon = response;
        onDeviceMetadataReceived(url, deviceMetadata);
      }
    }, 0, 0, null, null);
    mRequestQueue.add(imageRequest);
  }

  private static Runnable mBatchMetadataRunnable = new Runnable() {
    @Override
    public void run() {
      //batchLoadInitialMetadata();
      batchFetchMetaData();
      mIsQueuing = false;
    }
  };

  private static void batchLoadInitialMetadata() {
    if (mDeviceBatchList.size() > 0) {
      loadInitialMetadata(mDeviceBatchList);
    }
  }

  private static void batchFetchMetaData() {
    if (mDeviceBatchList.size() > 0) {
      getBatchMetadata(mDeviceBatchList);
      // Clear out the list
      mDeviceBatchList = new ArrayList<>();
    }
  }


  /**
   * A container class for a url's
   * fetched metadata.
   * The metadata consists of
   * the title, site url, description,
   * iconUrl and the icon (or favicon).
   * This data is scraped via a
   * server that receives a url
   * and returns a json blob.
   */

  public static class DeviceMetadata {
    public String title;
    public String siteUrl;
    public String description;
    public String iconUrl;
    public Bitmap icon;

    public DeviceMetadata() {
    }

  }
}
