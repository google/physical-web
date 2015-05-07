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

import android.os.AsyncTask;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.urlshortener.Urlshortener;
import com.google.api.services.urlshortener.UrlshortenerRequestInitializer;
import com.google.api.services.urlshortener.model.Url;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import android.util.Log;

/**
 * This class shortens urls and also expands those short urls
 * to their original url.
 * Currently this class only supports google url shortener
 *
 * TODO: Rename this class since url shortening has been refactored to the
 *       PwsClient.
 */
class UrlShortener {

  private static final String TAG = "UrlShortener";

  /**
   * A callback to be invoked when done lengthening or shortening a url
   */
  interface ModifiedUrlCallback {
    void onNewUrl(String newUrl);
    void onError(String oldUrl);
  }

  /**
   * Check if the given url is a short url.
   *
   * @param url The url that will be tested to see if it is short
   * @return The value that indicates if the given url is short
   */
  public static boolean isShortUrl(String url) {
    return url.startsWith("http://goo.gl/") || url.startsWith("https://goo.gl/");
  }

  /**
   * Takes any short url and converts it to the long url that is being pointed to.
   * Note: this method will work for all types of shortened urls as it inspect the
   * returned headers for the location.
   */
  public static class LengthenShortUrlTask extends AsyncTask<String, Void, String> {
    private ModifiedUrlCallback mCallback;
    private String mShortUrl;

    LengthenShortUrlTask(ModifiedUrlCallback callback) {
      mCallback = callback;
    }

    @Override
    protected String doInBackground(String[] params) {
      mShortUrl = params[0];
      try {
        URL url = new URL(mShortUrl);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setInstanceFollowRedirects(false);
        String longUrl = httpURLConnection.getHeaderField("location");
        return (longUrl != null) ? longUrl : mShortUrl;
      } catch (MalformedURLException e) {
        Log.w(TAG, "Malformed URL: " + mShortUrl);
      } catch (IOException e) {
        e.printStackTrace();
      }
      return null;
    }

    @Override
    protected void onPostExecute(String result) {
      if (result == null) {
        mCallback.onError(mShortUrl);
      } else {
        mCallback.onNewUrl(result);
      }
    }
  }
}
