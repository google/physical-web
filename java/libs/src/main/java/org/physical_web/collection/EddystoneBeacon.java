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


import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

/**
 * Eddystone beacon class.
 * This class represents the Eddystone broadcasting format.
 */
public class EddystoneBeacon {
  private static final byte URL_FRAME_TYPE = 0x10;
  private static final byte TITLE_TYPE = 0x0e;
  private static final String URN_UUID = "urn:uuid:";

  private static final HashMap<Byte, String> URI_SCHEMES = new HashMap<Byte, String>() {{
    put((byte) 0, "http://www.");
    put((byte) 1, "https://www.");
    put((byte) 2, "http://");
    put((byte) 3, "https://");
    put((byte) 4, URN_UUID);
  }};

  private static final HashMap<Byte, String> URL_CODES = new HashMap<Byte, String>() {{
    put((byte) 0, ".com/");
    put((byte) 1, ".org/");
    put((byte) 2, ".edu/");
    put((byte) 3, ".net/");
    put((byte) 4, ".info/");
    put((byte) 5, ".biz/");
    put((byte) 6, ".gov/");
    put((byte) 7, ".com");
    put((byte) 8, ".org");
    put((byte) 9, ".edu");
    put((byte) 10, ".net");
    put((byte) 11, ".info");
    put((byte) 12, ".biz");
    put((byte) 13, ".gov");
  }};

  private final byte mFlags;
  private final byte mTxPower;
  private final String mUrl;


  private EddystoneBeacon(byte flags, byte txPower, String url) {
    mFlags = flags;
    mTxPower = txPower;
    mUrl = url;
  }

  /**
   * Reads the title of a fat beacon broadcast.
   * @param serviceData The ble advertised Eddystone URL Service UUID service data
   * @return Title encoded in the broadcast
   */
  public static String getFatBeaconTitle(byte[] serviceData) {
    if (serviceData.length > 2) {
      byte[] bytes = Arrays.copyOfRange(serviceData, 3, serviceData.length);
      String title = new String(bytes, Charset.forName("UTF-8")).trim();
      return  title.indexOf('\uFFFD') == -1 ? title : "";
    }
    return "";
  }

  /**
   * Checks if the broadcast is a fat beacon.
   * @param serviceData The ble advertised Eddystone URL Service UUID service data
   * @return true if it is a fat beacon, false otherwise
   */
  public static boolean isFatBeacon(byte[] serviceData) {
    return (serviceData != null && serviceData.length > 3 && isUrlFrame(serviceData) &&
            serviceData[2] == TITLE_TYPE);
  }

  /**
   * Checks if the broadcast is a Eddystone URL.
   * @param serviceData The ble advertised Eddystone URL Service UUID service data
   * @return true if it is a URL, false otherwise
   */
  public static boolean isUrlFrame(byte[] serviceData) {
    return serviceData != null && serviceData.length > 0 &&
        (serviceData[0] & 0xf0) == URL_FRAME_TYPE;
  }

  /**
   * Parses the service data for URLs or URIs.
   * @param urlServiceData The ble advertised Eddystone URL Service UUID service data
   * @param uriServiceData The ble advertised URI Beacon Service UUID service data
   * @return EddystoneBeacon with flags, tx Power level and url parsed from the service data
   */
  public static EddystoneBeacon parseFromServiceData(byte[] urlServiceData, byte[] uriServiceData) {
    if (urlServiceData != null && urlServiceData.length > 2) {
      byte flags = (byte) (urlServiceData[0] & 0x0f);
      byte txPowerLevel = urlServiceData[1];
      return eddystoneBeaconBuilder(flags, txPowerLevel, decode(urlServiceData));
    }
    if (uriServiceData != null && uriServiceData.length > 2) {
      byte flags = uriServiceData[0];
      byte txPowerLevel = uriServiceData[1];
      return eddystoneBeaconBuilder(flags, txPowerLevel, decode(uriServiceData));
    }
    return null;
  }

  private static EddystoneBeacon eddystoneBeaconBuilder(byte flags, byte txPower, String url) {
    if (url == null || url.isEmpty()) {
      return null;
    }
    return new EddystoneBeacon(flags, txPower, url);
  }

  private static String decode(byte[] serviceData) {
    StringBuilder urlBuilder = new StringBuilder();
    String scheme = URI_SCHEMES.get(serviceData[2]);
    if (scheme != null) {
      urlBuilder.append(scheme);
      if (scheme.equals(URN_UUID)) {
        return decodeUrnUuid(serviceData, urlBuilder);
      }
      return decodeUrl(serviceData, urlBuilder);
    }

    return null;
  }

  private static String decodeUrl(byte[] serviceData, StringBuilder urlBuilder) {
    for (int i = 3; i < serviceData.length; i++) {
      byte b = serviceData[i];
      String expansion = URL_CODES.get(b);
      if (expansion == null) {
        urlBuilder.append((char) b);
      } else {
        urlBuilder.append(expansion);
      }
    }
    return urlBuilder.toString();
  }

  private static String decodeUrnUuid(byte[] serviceData, StringBuilder urnBuilder) {
    ByteBuffer buf = ByteBuffer.wrap(serviceData);
    buf.order(ByteOrder.BIG_ENDIAN);
    long mostSignificantBytes, leastSignificantBytes;
    try {
      buf.position(3);
      mostSignificantBytes = buf.getLong();
      leastSignificantBytes = buf.getLong();
    } catch (BufferUnderflowException e){
      return "";
    }
    UUID uuid = new UUID(mostSignificantBytes, leastSignificantBytes);
    urnBuilder.append(uuid.toString());
    return urnBuilder.toString();
  }


  /**
   * Getter for the Eddystone URL.
   * @return Eddystone URL
   */
  public String getUrl() {
    return mUrl;
  }

  /**
   * Getter for the Tx Power Level.
   * @return Tx Power Level
   */
  public byte getTxPowerLevel() {
    return mTxPower;
  }

  /**
   * Getter for the flags.
   * @return flags
   */
  public byte getFlags() {
    return mFlags;
  }
}
