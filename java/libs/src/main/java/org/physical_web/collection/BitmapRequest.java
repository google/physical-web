/*
 * Copyright 2016 Google Inc. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

/**
 * A class that represents an HTTP request for an image.
 * The response is a byte array of bitmap data.
 */
class BitmapRequest extends Request<byte[]> {
  /**
   * Construct a bitmap HTTP request.
   * @param url The url to make this HTTP request to.
   * @param callback The callback run when the HTTP response is received.
   *     The callback can be called with a null bitmap if the image
   *     couldn't be decoded.
   * @throws MalformedURLException on invalid url
   */
  public BitmapRequest(String url, RequestCallback callback) throws MalformedURLException {
    super(url, callback);
  }

  /**
   * The callback that gets run after the request is made.
   */
  public interface RequestCallback extends Request.RequestCallback<byte[]> {}

  /**
   * Helper method to make an HTTP request.
   * @param urlConnection The HTTP connection.
   */
  public void writeToUrlConnection(HttpURLConnection urlConnection) throws IOException {}

  /**
   * Helper method to read an HTTP response.
   * @param is The InputStream.
   * @return The decoded image.
   */
  protected byte[] readInputStream(InputStream is) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int len;
    while ((len = is.read(buffer)) != -1) {
      os.write(buffer, 0, len);
    }
    return os.toByteArray();
  }
}
