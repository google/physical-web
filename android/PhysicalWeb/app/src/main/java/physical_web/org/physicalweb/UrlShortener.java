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

import android.os.AsyncTask;
import android.util.Log;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.urlshortener.Urlshortener;
import com.google.api.services.urlshortener.UrlshortenerRequestInitializer;
import com.google.api.services.urlshortener.model.Url;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * This class shortens urls
 * and aslo expands those short urls
 * to their original url.
 * Currently this class only supports google url shortener
 * TODO: support other url shorteners
 */

public class UrlShortener {

  private static String TAG = "UrlShortener";

  /**
   * Create the shortened form
   * of the given url.
   *
   * @param longUrl
   * @return
   */
  public static String shortenUrl(String longUrl) {
    String shortUrl = null;
    try {
      shortUrl = (String) new ShortenUrlTask().execute(longUrl).get();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
    return shortUrl;
  }

  /**
   * Create a google url shortener interface object
   * and make a request to shorten the given url
   */
  private static class ShortenUrlTask extends AsyncTask {
    @Override
    protected String doInBackground(Object[] params) {
      String longUrl = (String) params[0];
      Urlshortener urlshortener = createGoogleUrlShortener();
      Url url = new Url();
      url.setLongUrl(longUrl);
      try {
        Url response = urlshortener.url().insert(url).execute();
        return response.getId();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return null;
    }
  }

  /**
   * Create the shortened form
   * of the given url.
   * Currently uses google url shortener
   *
   * @param shortUrl
   * @return
   */
  public static String lengthenShortUrl(String shortUrl) {
    String longUrl = null;
    try {
      longUrl = (String) new LengthenShortUrlTask().execute(shortUrl).get();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }
    return longUrl;
  }

  /**
   * Create a google url shortener interface object
   * and make a request to expand the given url
   */

  private static class LengthenShortUrlTask extends AsyncTask {
    @Override
    protected String doInBackground(Object[] params) {
      String shortUrl = (String) params[0];
      Urlshortener urlshortener = createGoogleUrlShortener();
      try {
        Url response = urlshortener.url().get(shortUrl).execute();
        return response.getLongUrl();
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
   * @return
   */
  private static Urlshortener createGoogleUrlShortener() {
    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
    JsonFactory jsonFactory = AndroidJsonFactory.getDefaultInstance();
    UrlshortenerRequestInitializer urlshortenerRequestInitializer = new UrlshortenerRequestInitializer();
    Urlshortener.Builder builder = new Urlshortener.Builder(httpTransport, jsonFactory, null);
    builder.setApplicationName("PhysicalWeb");
    builder.setUrlshortenerRequestInitializer(urlshortenerRequestInitializer).build();
    Urlshortener urlshortener = builder.build();

    return urlshortener;
  }

  /**
   * Check if the given url is a short url.
   *
   * @param url
   * @return
   */
  public static boolean isShortUrl(String url) {
    if (url.startsWith("http://goo.gl/") || url.startsWith("https://goo.gl/")) {
      return true;
    }
    return false;
  }

}
