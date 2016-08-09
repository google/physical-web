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

package org.physical_web.physicalweb;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class sends requests to the physical web service to shorten URLs.
 */
class UrlShortenerClient {
  private static final String TAG = UrlShortenerClient.class.getSimpleName();
  private static final String RESOLVE_SCAN_PATH = "resolve-scan";
  private static final String SHORTEN_URL_PATH = "shorten-url";
  private static final int UNDEFINED_SCORE = -1;
  private RequestQueue mRequestQueue;
  private Context mContext;
  private String mEndpointUrl;

  private static UrlShortenerClient mInstance;

  private UrlShortenerClient(Context context) {
    mContext = context.getApplicationContext();
    mRequestQueue = Volley.newRequestQueue(mContext);
    mEndpointUrl = Utils.PROD_ENDPOINT;
  }

  public static UrlShortenerClient getInstance(Context context) {
    if (mInstance == null) {
        mInstance = new UrlShortenerClient(context);
    }
    return mInstance;
  }

  public interface ShortenUrlCallback {
    public void onUrlShortened(String shortUrl);
    public void onError(String longUrl);
  }

  private String constructUrlStr(final String path) {
    return mEndpointUrl + "/" + path;
  }

  public void shortenUrl(final String longUrl,
                         final ShortenUrlCallback shortenUrlCallback,
                         final String tag) {
    // Create the json payload
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("longUrl", longUrl);
    } catch (JSONException e) {
      Log.e(TAG, "JSONException: " + e.toString());
      shortenUrlCallback.onError(longUrl);
      return;
    }

    // Create the http request
    JsonObjectRequest request = new JsonObjectRequest(
        constructUrlStr(SHORTEN_URL_PATH),
        jsonObject,
        new Response.Listener<JSONObject>() {
          @Override
          public void onResponse(JSONObject jsonResponse) {
            String shortUrl;
            try {
              shortUrl = jsonResponse.getString("id");
            } catch (JSONException e) {
              Log.e(TAG, "JSONException: " + e.toString());
              shortenUrlCallback.onError(longUrl);
              return;
            }
            shortenUrlCallback.onUrlShortened(shortUrl);
          }
        },
        new Response.ErrorListener() {
          @Override
          public void onErrorResponse(VolleyError volleyError) {
            Log.i(TAG, "VolleyError: " + volleyError.toString());
            shortenUrlCallback.onError(longUrl);
          }
        }
    );
    request.setTag(tag);

    // Send off the request
    mRequestQueue.add(request);
  }

  public void cancelAllRequests(final String tag) {
    mRequestQueue.cancelAll(tag);
  }

  public void setEndpoint(String endpointUrl) {
    mEndpointUrl = endpointUrl;
  }
}
