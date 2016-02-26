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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A class that represents an http request.
 * This is to be used as a base class for more specific request classes.
 * @param <T> The type representing the request payload.
 */
abstract class Request<T> extends Thread {
  private URL mUrl;
  private RequestCallback<T> mCallback;

  /**
   * Construct a Request object.
   * @param url The url to make an HTTP request to.
   * @param callback The callback run when the HTTP response is received.
   * @throws MalformedURLException on invalid url
   */
  public Request(String url, RequestCallback<T> callback) throws MalformedURLException {
    mUrl = new URL(url);
    mCallback = callback;
  }

  /**
   * The callback that gets run after the request is made.
   */
  public interface RequestCallback<T> {
    /**
     * The callback run on a valid response.
     * @param result The result object.
     */
    void onResponse(T result);

    /**
     * The callback run on an Exception.
     * @param httpResponseCode The HTTP response code.  This will be 0 if no
     *        response was received.
     * @param e The encountered Exception.
     */
    void onError(int httpResponseCode, Exception e);
  }

  /**
   * Make the HTTP request and parse the HTTP response.
   */
  @Override
  public void run() {
    // Setup some values
    HttpURLConnection urlConnection = null;
    T result = null;
    InputStream inputStream = null;
    int responseCode = 0;
    IOException ioException = null;

    // Make the request
    try {
      urlConnection = (HttpURLConnection) mUrl.openConnection();
      writeToUrlConnection(urlConnection);
      responseCode = urlConnection.getResponseCode();
      inputStream = new BufferedInputStream(urlConnection.getInputStream());
      result = readInputStream(inputStream);
    } catch (IOException e) {
      ioException = e;
    } finally {
      if (urlConnection != null) {
        urlConnection.disconnect();
      }
    }

    // Call the callback
    if (ioException == null) {
      mCallback.onResponse(result);
    } else {
      mCallback.onError(responseCode, ioException);
    }
  }

  /**
   * Helper method to make an HTTP request.
   * @param urlConnection The HTTP connection.
   * @throws IOException on error
   */
  protected abstract void writeToUrlConnection(HttpURLConnection urlConnection) throws IOException;

  /**
   * Helper method to read an HTTP response.
   * @param is The InputStream.
   * @return An object representing the HTTP response.
   * @throws IOException on error
   */
  protected abstract T readInputStream(InputStream is) throws IOException;
}
