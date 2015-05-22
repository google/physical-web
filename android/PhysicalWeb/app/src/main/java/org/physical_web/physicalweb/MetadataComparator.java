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

package org.physical_web.physicalweb;

import java.util.Comparator;
import java.util.HashMap;

/**
 * A comparator that sorts urls based on their metadata.
 *
 * This comparator needs to be initialized with a map from urls to metadata.
 * It will use them to perform the comparing.
 */
public class MetadataComparator implements Comparator<String> {
  private final HashMap<String, PwsClient.UrlMetadata> mUrlToUrlMetadata;

  public MetadataComparator(final HashMap<String, PwsClient.UrlMetadata> urlToUrlMetadata) {
    mUrlToUrlMetadata = urlToUrlMetadata;
  }

  /**
   * Compare two urls.
   */
  @Override
  public int compare(String urlA, String urlB) {
    PwsClient.UrlMetadata urlMetadataA = mUrlToUrlMetadata.get(urlA);
    PwsClient.UrlMetadata urlMetadataB = mUrlToUrlMetadata.get(urlB);

    // Return the best rank given by proxy server
    // If a url lacks metedata, the other should come first.
    if (urlMetadataA == null) {
      return 1;
    }
    if (urlMetadataB == null) {
      return -1;
    }
    int rankCompare = ((Float) urlMetadataA.rank).compareTo(urlMetadataB.rank);
    if (rankCompare != 0) {
      return rankCompare;
    }

    // If ranks are equal, compare based on title
    return urlMetadataA.title.compareTo(urlMetadataB.title);
  }
}
