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


package org.physical_web.physicalweb.ble;

import android.annotation.TargetApi;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.SparseArray;
import android.webkit.URLUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.UUID;

 /**
 *Helper class to simplify Eddystone URL encoding.
**/
public class AdvertiseDataUtils {
    private static final String TAG = "AdvertiseDataUtils";
    private static final ParcelUuid EDDYSTONE_BEACON_UUID = ParcelUuid.fromString(
        "0000FEAA-0000-1000-8000-00805F9B34FB");
    /**
     * URI Scheme maps a byte code into the scheme and an optional scheme specific prefix.
     */
    private static final SparseArray<String> URI_SCHEMES = new SparseArray<String>() {{
        put((byte) 0, "http://www.");
        put((byte) 1, "https://www.");
        put((byte) 2, "http://");
        put((byte) 3, "https://");
        put((byte) 4, "urn:uuid:");    // RFC 2141 and RFC 4122};
    }};

    /**
     * Expansion strings for "http" and "https" schemes. These contain strings appearing
     * anywhere in a URL. Restricted to Generic TLDs. <p/> Note: this is a scheme specific encoding.
     */
    private static final SparseArray<String> URL_CODES = new SparseArray<String>() {{
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
   private static final byte URL_FRAME_TYPE = 0x10;
   private static final byte FAT_BEACON = 0x0e;


   /**
     * Creates the Uri string with embedded expansion codes.
     *
     * @param uri to be encoded
     * @return the Uri string with expansion codes.
     */
    public static byte[] encodeUri(String uri) {
        if (uri == null || uri.length() == 0) {
            Log.i(TAG, "null or empty uri");
            return new byte[0];
        }
        ByteBuffer bb = ByteBuffer.allocate(uri.length());
        // UUIDs are ordered as byte array, which means most significant first
        bb.order(ByteOrder.BIG_ENDIAN);
        int position = 0;

        // Add the byte code for the scheme or return null if none
        Byte schemeCode = encodeUriScheme(uri);
        if (schemeCode == null) {
            Log.i(TAG, "null scheme code");
            return null;
        }
        String scheme = URI_SCHEMES.get(schemeCode);
        bb.put(schemeCode);
        position += scheme.length();

        if (URLUtil.isNetworkUrl(scheme)) {
            Log.i(TAG, "is network URL");
            return encodeUrl(uri, position, bb);
        } else if ("urn:uuid:".equals(scheme)) {
            Log.i(TAG, "is UUID");
            return encodeUrnUuid(uri, position, bb);
        }
        return null;
    }

    /**
     * Finds the longest expansion from the uri at the current position.
     *
     * @param uriString the Uri
     * @param pos start position
     * @return an index in URI_MAP or 0 if none.
     */
    private static byte findLongestExpansion(String uriString, int pos) {
        byte expansion = -1;
        int expansionLength = 0;
        for (int i = 0; i < URL_CODES.size(); i++) {
            // get the key and value.
            int key = URL_CODES.keyAt(i);
            String value = URL_CODES.valueAt(i);
            if (value.length() > expansionLength && uriString.startsWith(value, pos)) {
                expansion = (byte) key;
                expansionLength = value.length();
            }
        }
        return expansion;
    }

    private static Byte encodeUriScheme(String uri) {
        String lowerCaseUri = uri.toLowerCase(Locale.ENGLISH);
        for (int i = 0; i < URI_SCHEMES.size(); i++) {
            // get the key and value.
            int key = URI_SCHEMES.keyAt(i);
            String value = URI_SCHEMES.valueAt(i);
            if (lowerCaseUri.startsWith(value)) {
                return (byte) key;
            }
        }
        return null;
    }

    private static byte[] encodeUrl(String url, int position, ByteBuffer bb) {
        while (position < url.length()) {
            byte expansion = findLongestExpansion(url, position);
            if (expansion >= 0) {
                bb.put(expansion);
                position += URL_CODES.get(expansion).length();
            } else {
                bb.put((byte) url.charAt(position++));
            }
        }
        return byteBufferToArray(bb);
    }

    private static byte[] encodeUrnUuid(String urn, int position, ByteBuffer bb) {
        String uuidString = urn.substring(position, urn.length());
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            //Log.w(TAG, "encodeUrnUuid invalid urn:uuid format - " + urn);
            return null;
        }
        // UUIDs are ordered as byte array, which means most significant first
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return byteBufferToArray(bb);
    }

    private static byte[] byteBufferToArray(ByteBuffer bb) {
        byte[] bytes = new byte[bb.position()];
        bb.rewind();
        bb.get(bytes, 0, bytes.length);
        return bytes;
    }

    // Generate the advertising bytes for the given URL
    @TargetApi(21)
    public static AdvertiseData getAdvertisementData(byte[] urlData) {
      AdvertiseData.Builder builder = new AdvertiseData.Builder();
      builder.setIncludeTxPowerLevel(false); // reserve advertising space for URI

      // Manually build the advertising info
      // See https://github.com/google/eddystone/tree/master/eddystone-url
      if (urlData == null || urlData.length == 0) {
        return null;
      }

      byte[] beaconData = new byte[urlData.length + 2];
      System.arraycopy(urlData, 0, beaconData, 2, urlData.length);
      beaconData[0] = URL_FRAME_TYPE; // frame type: url
      beaconData[1] = (byte) 0xBA; // calibrated tx power at 0 m

      builder.addServiceData(EDDYSTONE_BEACON_UUID, beaconData);

      // Adding 0xFEAA to the "Service Complete List UUID 16" (0x3) for iOS compatibility
      builder.addServiceUuid(EDDYSTONE_BEACON_UUID);

      return builder.build();
    }

   // Build and return the advertising bytes for the given FatBeacon advertisement
   @TargetApi(21)
   public static AdvertiseData getFatBeaconAdvertisementData(byte[] fatBeaconAdvertisement) {

     // Manually build the advertising info
     int length = Math.min(fatBeaconAdvertisement.length, 17);
     byte[] beaconData = new byte[length + 3];
     System.arraycopy(fatBeaconAdvertisement, 0, beaconData, 3, length);
     beaconData[0] = URL_FRAME_TYPE;
     beaconData[1] = (byte) 0xBA;
     beaconData[2] = FAT_BEACON;
     return new AdvertiseData.Builder()
         .setIncludeTxPowerLevel(false) // reserve advertising space for URI
         .addServiceData(EDDYSTONE_BEACON_UUID, beaconData)
         // Adding 0xFEAA to the "Service Complete List UUID 16" (0x3) for iOS compatibility
         .addServiceUuid(EDDYSTONE_BEACON_UUID)
         .build();
   }

    // Build and return the ble advertising settings
    @TargetApi(21)
    public static AdvertiseSettings getAdvertiseSettings(boolean connectable) {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);
        builder.setConnectable(connectable);

        return builder.build();
    }
}
