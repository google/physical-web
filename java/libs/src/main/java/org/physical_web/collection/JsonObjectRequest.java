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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

/**
 * A class that represents an HTTP request for a JSON object.
 * Both the request payload and the response are JSON objects.
 */
class JsonObjectRequest extends Request<JSONObject> {
  private JSONObject mJsonObject;

  /**
   * Construct a JSON object request.
   * @param url The url to make this HTTP request to.
   * @param jsonObject The JSON payload.
   * @param callback The callback run when the HTTP response is received.
   * @throws MalformedURLException on invalid url
   */
  public JsonObjectRequest(String url, JSONObject jsonObject, RequestCallback callback)
      throws MalformedURLException {
    super(url, callback);
    mJsonObject = jsonObject;
  }

  /**
   * The callback that gets run after the request is made.
   */
  public interface RequestCallback extends Request.RequestCallback<JSONObject> {}

  /**
   * Helper method to make an HTTP request.
   * @param urlConnection The HTTP connection.
   */
  public void writeToUrlConnection(HttpURLConnection urlConnection) throws IOException {
    urlConnection.setDoOutput(true);
    urlConnection.setRequestProperty("Content-Type", "application/json");
    urlConnection.setRequestProperty("Accept", "application/json");
    urlConnection.setRequestMethod("POST");
    OutputStream os = urlConnection.getOutputStream();
    os.write(mJsonObject.toString().getBytes("UTF-8"));
    os.close();
  }

  /**
   * Helper method to read an HTTP response.
   * @param is The InputStream.
   * @return An object representing the HTTP response.
   */
  protected JSONObject readInputStream(InputStream is) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
    String line;
    while ((line = in.readLine()) != null) {
        stringBuilder.append(line);
    }
    JSONObject jsonObject;
    try {
        jsonObject = new JSONObject(stringBuilder.toString());
    } catch (JSONException error) {
        throw new IOException(error.toString());
    }
    return jsonObject;
  }
}
