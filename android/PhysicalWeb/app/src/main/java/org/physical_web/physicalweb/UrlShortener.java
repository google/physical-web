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
 * TODO: support other url shorteners
 */
class UrlShortener {

  private static final String TAG = "UrlShortener";

  /**
   * Create the shortened form
   * of the given url.
   *
   * @param longUrl The url that will be shortened
   * @return The short url for the given longUrl
   */
  // TODO: make sure this network operation is off the ui thread
  public static String shortenUrl(String longUrl) {
    String shortUrl = null;
    try {
      shortUrl = new ShortenUrlTask().execute(longUrl).get();
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
    return shortUrl;
  }

  /**
   * Create a google url shortener interface object
   * and make a request to shorten the given url
   */
  private static class ShortenUrlTask extends AsyncTask<String, Void, String> {
    @Override
    protected String doInBackground(String[] params) {
      String longUrl = params[0];
      Urlshortener urlshortener = createGoogleUrlShortener();
      Url url = new Url();
      url.setLongUrl(longUrl);
      try {
        Url response = urlshortener.url().insert(url).execute();
        //avoid possible NPE
        if (response != null) {
          return response.getId();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      return null;
    }
  }

  /**
   * Create an instance of the google url shortener object
   * and return it.
   *
   * @return The created shortener object
   */
  private static Urlshortener createGoogleUrlShortener() {
    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
    JsonFactory jsonFactory = AndroidJsonFactory.getDefaultInstance();
    UrlshortenerRequestInitializer urlshortenerRequestInitializer = new UrlshortenerRequestInitializer();
    return new Urlshortener.Builder(httpTransport, jsonFactory, null)
        .setApplicationName("PhysicalWeb")
        .setUrlshortenerRequestInitializer(urlshortenerRequestInitializer)
        .build();
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
   *
   * @param shortUrl The short url that will be lengthened
   * @return The lengthened url for the given short url
   */
  // TODO: make sure this network operation is off the ui thread
  public static String lengthenShortUrl(String shortUrl) {
    String longUrl = null;
    try {
      longUrl = new LengthenShortUrlTask().execute(shortUrl).get();
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
    return longUrl;
  }

  private static class LengthenShortUrlTask extends AsyncTask<String, Void, String> {
    @Override
    protected String doInBackground(String[] params) {
      String shortUrl = params[0];
      try {
        URL url = new URL(shortUrl);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setInstanceFollowRedirects(false);
        String longUrl = httpURLConnection.getHeaderField("location");
        return (longUrl != null) ? longUrl : shortUrl;
      } catch (MalformedURLException e) {
        Log.w(TAG, "Malformed URL: " + shortUrl);
      } catch (IOException e) {
        e.printStackTrace();
      }
      return null;
    }
  }
}
