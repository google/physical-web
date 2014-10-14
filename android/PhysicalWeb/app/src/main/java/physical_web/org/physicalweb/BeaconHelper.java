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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * This class contains a variety of methods
 * that are used to parse ble advertising packets,
 * detect beacon existence from that parsing,
 * afford url expansion codes when creating url byte arrays,
 * and create instances of beacons.
 */
public class BeaconHelper {

  private static String TAG = "BeaconHelper";
  private static final String[] EXPANSION_CODES_TO_TEXT_MAP = new String[]{"http://www.", "https://www.", "http://", "https://", "tel:", "mailto:", "geo:", ".com", ".org", ".edu"};
  private static final int MAX_NUM_BYTES_URL = 18;
  private static final byte[] ADVERTISING_PACKET_HEADER = new byte[]{(byte) 0x03, (byte) 0x03, (byte) 0xD8, (byte) 0xFE};
  private static final byte[] URI_SERVICE_DATA_HEADER = new byte[]{(byte) 0x16, (byte) 0xD8, (byte) 0xFE, (byte) 0x00, (byte) 0xC1};

  /**
   * Create a beacon advertising packet
   * that will contain the given url.
   *
   * @param url Url to write to the beacon
   * @return the encoded url
   * @throws IOException
   */
  public static byte[] createAdvertisingPacket(String url) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    byte[] url_bytes = createUrlBytes(url);
    byte length = (byte) (URI_SERVICE_DATA_HEADER.length + url_bytes.length);
    outputStream.write(ADVERTISING_PACKET_HEADER);
    outputStream.write(length);
    outputStream.write(URI_SERVICE_DATA_HEADER);
    outputStream.write(url_bytes);
    return outputStream.toByteArray();
  }

  /**
   * Create the byte array that represents the given url.
   * This process first compresses the url using the expansion codes.
   * Then if the url is still too long, we shorten it
   * with a url shortener.
   * Then we compress that url using the expansion codes again.
   *
   * @param url URL to encode
   * @return encoded URL
   * @throws IOException
   */
  private static byte[] createUrlBytes(String url) throws IOException {
    byte[] url_bytes;
    url_bytes = compressUrlUsingExpansionCodes(url);
    if (url_bytes.length > MAX_NUM_BYTES_URL) {
      String url_shortened = UrlShortener.shortenUrl(url);
      url_bytes = compressUrlUsingExpansionCodes(url_shortened);
    }
    return url_bytes;
  }

  /**
   * Compress the given url by looking for
   * a hardcoded set of substrings (e.g. http://, .edu, etc.)
   * and replacing them with an associated integer.
   *
   * @param url URL to compress
   * @return Compressed URL
   * @throws IOException
   */
  private static byte[] compressUrlUsingExpansionCodes(String url) throws IOException {
    // Specify the characters that will be used
    // to tag where expansion codes are needed and inserted
    String splitChar = ":";
    String codeIndicatorChar = "#";
    // Loop through the expansion substrings
    // to search for in the url
    for (int i = 0; i < EXPANSION_CODES_TO_TEXT_MAP.length; i++) {
      String text = EXPANSION_CODES_TO_TEXT_MAP[i];
      // If the url contains this substring
      if (url.contains(text)) {
        // Replace the substring in the url with the associated integer code,
        // place a code indicator character directly preceding the code,
        // and flank the code indicator and code by split characters
        // so we know which integers are expansion codes
        String replacementText = splitChar + codeIndicatorChar + String.valueOf(i) + splitChar;
        url = url.replace(text, replacementText);
      }
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    // Split the url up by the split character
    String[] url_split = url.split(splitChar);
    // Loop through the substring in the split array
    for (String subString : url_split) {
      // If the given substring contains the
      // code indicator character
      // (i.e. if there is an expansion code)
      if (subString.contains(codeIndicatorChar)) {
        // Remove the indicator and get the integer value of that code
        int code = Integer.valueOf(subString.replace(codeIndicatorChar, ""));
        // Write the code to the output stream
        outputStream.write((byte) code);
        // If the given substring does not contain the
        // code indicator character
        // (i.e. if there is no expansion code)
      } else {
        // Write the substring to the output stream
        outputStream.write(subString.getBytes());
      }
    }
    //get the byte array for the output stream
    byte[] url_bytes = outputStream.toByteArray();

    return url_bytes;
  }

}


