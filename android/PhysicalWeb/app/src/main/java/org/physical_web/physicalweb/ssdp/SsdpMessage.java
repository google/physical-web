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

package org.physical_web.physicalweb.ssdp;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is the Java representation of SSDP messages.
 * It creates SsdpMessage instances from a text representation
 * of a SSDP message or converts a SsdpMessage instance to string
 */

public class SsdpMessage {
  public static final int TYPE_SEARCH = 0;
  public static final int TYPE_NOTIFY = 1;
  public static final int TYPE_FOUND = 2;
  private static final String NL = "\r\n";
  private static final String FIRST_LINE[] = {
      Ssdp.TYPE_M_SEARCH + " * HTTP/1.1",
      Ssdp.TYPE_NOTIFY + " * HTTP/1.1",
      "HTTP/1.1 " + Ssdp.TYPE_200_OK
  };
  private int mType;
  private Map<String, String> mHeaders;

  public SsdpMessage(int type) {
    this.mType = type;
  }

  public SsdpMessage(String txt) {
    String lines[] = txt.split(NL);
    String line = lines[0].trim();
    if (line.startsWith(Ssdp.TYPE_M_SEARCH)) {
      this.mType = TYPE_SEARCH;
    } else if (line.startsWith(Ssdp.TYPE_NOTIFY)) {
      this.mType = TYPE_NOTIFY;
    } else {
      this.mType = TYPE_FOUND;
    }
    for (int i = 1; i < lines.length; i++) {
      line = lines[i].trim();
      int index = line.indexOf(':');
      if (index > 0) {
        String key = line.substring(0, index).trim();
        String value = line.substring(index + 1).trim();
        getHeaders().put(key, value);
      }
    }
  }

  public Map<String, String> getHeaders() {
    if (mHeaders == null) {
      mHeaders = new HashMap<>();
    }
    return mHeaders;
  }

  public int getType() {
    return mType;
  }

  public String get(String key) {
    return getHeaders().get(key);
  }

  public String put(String key, String value) {
    return getHeaders().put(key, value);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(FIRST_LINE[this.mType]).append(NL);
    for (Map.Entry<String, String> entry: getHeaders().entrySet()) {
      builder.append(entry.getKey())
          .append(": ")
          .append(entry.getValue())
          .append(NL);
    }
    builder.append(NL);
    return builder.toString();
  }
}
