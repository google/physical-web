package org.physical_web.physicalweb;

import org.physical_web.collection.PhysicalWebCollection;
import org.physical_web.collection.PwPair;
import org.physical_web.collection.PwsClient;
import org.physical_web.collection.PwsResult;
import org.physical_web.collection.UrlDevice;

import android.content.Context;
import android.content.res.Resources;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


/**
 * Unit tests for Utils.
 */
public class UtilsTest {

  private static final String GOOGLE_PWS_URL = "https://physicalweb.googleapis.com";
  private static final int GOOGLE_PWS_VERSION = 2;
  private static final String PROD_PWS_URL = "https://url-caster.appspot.com";
  private static final int PROD_PWS_VERSION = 1;
  public static final String BLE_DEVICE_TYPE = "ble";
  private static final String SCAN_TIME_KEY = "scantime";
  private static final String PUBLIC_KEY = "public";
  private static final String RSSI_KEY = "rssi";
  private static final String TYPE_KEY = "type";
  private static final String TXPOWER_KEY = "tx";
  private static final String PWS_TRIP_TIME_KEY = "pwstriptime";
  private static final String GOOGLE_API_KEY_RESOURCE_KEY = "google_api_key";
  private static final String GOOGLE_API_KEY_RESOURCE_TYPE = "string";
  private static final String SAMPLE_API_KEY = "key";
  private static final String SAMPLE_PACKAGE_NAME = "test";
  List<String> siteUrls = Arrays.asList((new String[]{"www.google.com", "www.google.com/maps"}));

  @Before
  public void setUp() throws Exception {
    for (String url : siteUrls) {
      if (Utils.isFavorite(url)) {
        Utils.toggleFavorite(url);
      }
    }
  }

  /*
   * Tests containsFavorite with empty list.
   *
   * Prerequisites:
   *  - Set of favorites is empty
   * Procedure:
   *  1. Verify list does not contain favorite
   */
  @Test
  public void testContainsFavoriteEmptyList() throws Exception {
    List<PwPair> pairs = new ArrayList<>();
    Assert.assertFalse(Utils.containsFavorite(pairs));
  }

  /*
   * Tests containsFavorite with a one element list. Also exercises isFavorite and toggleFavorite.
   *
   * Prerequisites:
   *  - Set of favorites is empty
   * Procedure:
   *  1. Add 1 PwPair to list
   *  2. Verify list does not contain favorite
   *  3. Add site used for PwPair to favorites
   *  4. Verify list contains favorite
   */
  @Test
  public void testContainsFavoriteOneItem() throws Exception {
    String siteUrl = siteUrls.get(0);
    List<PwPair> pairs = new ArrayList<>();
    pairs.add(new PwPair(null, new PwsResult("", siteUrl)));
    Assert.assertFalse(Utils.containsFavorite(pairs));
    Utils.toggleFavorite(siteUrl);
    Assert.assertTrue(Utils.containsFavorite(pairs));
  }

  /*
   * Tests containsFavorite with a two element list. Also exercises isFavorite and toggleFavorite.
   *
   * Prerequisites:
   *  - Set of favorites is empty
   * Procedure:
   *  1. Add 1 PwPair to list
   *  2. Verify list does not contain favorite
   *  3. Add site used for PwPair to favorites
   *  4. Verify list contains favorite
   */
  @Test
  public void testContainsFavoriteTwoItems() throws Exception {
    String siteUrl = siteUrls.get(0);
    List<PwPair> pairs = new ArrayList<>();
    pairs.add(new PwPair(null, new PwsResult("", siteUrl)));
    pairs.add(new PwPair(null, new PwsResult("", siteUrls.get(1))));
    Assert.assertFalse(Utils.containsFavorite(pairs));
    Utils.toggleFavorite(siteUrl);
    Assert.assertTrue(Utils.containsFavorite(pairs));
  }

  /*
   * Tests formatEndpointForSharedPreferences.
   *
   * Procedure:
   *  1. Verify string with null values and 0
   *  2. Verify string with empty values and the minimum integer value
   *  3. Verify string with non empty values and the maximum integer value
   */
  @Test
  public void testFormatEndpointForSharedPreferences() throws Exception {
    String url = null;
    int version = 0;
    String key = null;
    Assert.assertArrayEquals("Null values", new String[]{"", Integer.toString(version)},
        Utils.formatEndpointForSharedPrefernces(url, version, key).split("\0"));
    url = "";
    version = Integer.MIN_VALUE;
    key = "";
    Assert.assertArrayEquals("Empty Strings and min", new String[]{url, Integer.toString(version)},
        Utils.formatEndpointForSharedPrefernces(url, version, key).split("\0"));
    url = "url";
    version = Integer.MAX_VALUE;
    key = SAMPLE_API_KEY;
    Assert.assertArrayEquals("Non empty strings and max",
        new String[]{url, Integer.toString(version), key},
        Utils.formatEndpointForSharedPrefernces(url, version, key).split("\0"));
  }

  /*
   * Tests getDefaultPwsEndpointPreferenceString. Also exercises isGoogleApiKeyAvailable and
   * formatEndpointForSharedPreferences.
   *
   * Procedure:
   *  1. Configure there to not be an api key
   *  2. Verify default is the prod pws
   *  3. Configure there to be an api key
   *  4. Verify default is the Google pws
   */
  @Test
  public void testGetDefaultPwsEndpointPreferenceString() throws Exception {
    int resId = 1;
    String apiKey = SAMPLE_API_KEY;
    String packageName = SAMPLE_PACKAGE_NAME;
    Resources mockResources = Mockito.mock(Resources.class);
    Mockito.when(mockResources.getIdentifier(GOOGLE_API_KEY_RESOURCE_KEY,
        GOOGLE_API_KEY_RESOURCE_TYPE, packageName)).thenReturn(0);
    Context mockContext = Mockito.mock(Context.class);
    Mockito.when(mockContext.getResources()).thenReturn(mockResources);
    Mockito.when(mockContext.getPackageName()).thenReturn(packageName);
    Assert.assertEquals(PROD_PWS_URL + "\0" + PROD_PWS_VERSION + "\0",
        Utils.getDefaultPwsEndpointPreferenceString(mockContext));

    Mockito.when(mockResources.getIdentifier(GOOGLE_API_KEY_RESOURCE_KEY,
        GOOGLE_API_KEY_RESOURCE_TYPE, packageName)).thenReturn(resId);
    Mockito.when(mockContext.getString(resId)).thenReturn(apiKey);
    Assert.assertEquals(GOOGLE_PWS_URL + "\0" + GOOGLE_PWS_VERSION + "\0" + apiKey,
        Utils.getDefaultPwsEndpointPreferenceString(mockContext));
  }

  /*
    * Tests getDefaultPwsEndpointPreferenceString. Also exercises isGoogleApiKeyAvailable and
    * formatEndpointForSharedPreferences.
    *
    * Procedure:
    *  1. Configure there to be an api key
    *  2. Verify setEnpoint is called with Google pws
    */
  @Test
  public void testSetPwsEndPointToGoogle() throws Exception {
    int resId = 1;
    String apiKey = SAMPLE_API_KEY;
    String packageName = SAMPLE_PACKAGE_NAME;
    Resources mockResources = Mockito.mock(Resources.class);
    Mockito.when(mockResources.getIdentifier(GOOGLE_API_KEY_RESOURCE_KEY,
        GOOGLE_API_KEY_RESOURCE_TYPE, packageName)).thenReturn(resId);
    Context mockContext = Mockito.mock(Context.class);
    Mockito.when(mockContext.getResources()).thenReturn(mockResources);
    Mockito.when(mockContext.getString(resId)).thenReturn(apiKey);
    Mockito.when(mockContext.getPackageName()).thenReturn(packageName);
    PwsClient client = Mockito.mock(PwsClient.class);
    Utils.setPwsEndPointToGoogle(mockContext, client);
    Mockito.verify(client, Mockito.times(1)).setEndpoint(GOOGLE_PWS_URL, GOOGLE_PWS_VERSION,
        apiKey);
  }

  /*
  * Tests isGoogleApiKeyAvailable.
  *
  * Procedure:
  *  1. Configure there to not be an api key
  *  2. Verify that there is no api key
  *  3. Configure there to be an api key
  *  4. Verify that there is an api key
  */
  @Test
  public void testIsGoogleApiKeyAvailable() throws Exception {
    String packageName = "test";
    Resources resources = Mockito.mock(Resources.class);
    Mockito.when(resources.getIdentifier(GOOGLE_API_KEY_RESOURCE_KEY,
        GOOGLE_API_KEY_RESOURCE_TYPE, packageName)).thenReturn(0);
    Context context = Mockito.mock(Context.class);
    Mockito.when(context.getResources()).thenReturn(resources);
    Mockito.when(context.getPackageName()).thenReturn(packageName);
    Assert.assertFalse(Utils.isGoogleApiKeyAvailable(context));

    Mockito.when(resources.getIdentifier(GOOGLE_API_KEY_RESOURCE_KEY,
        GOOGLE_API_KEY_RESOURCE_TYPE, packageName)).thenReturn(1);
    Assert.assertTrue(Utils.isGoogleApiKeyAvailable(context));
  }

  /*
  * Tests getGoogleApiKey. Also exercises isGoogleApiKeyAvailable.
  *
  * Procedure:
  *  1. Configure there to be an api key
  *  2. Verify api key is equal to apiKey
  *  3. Configure there to not be an api key
  *  4. Verify api key is empty
  */
  @Test
  public void testGetGoogleApiKey() throws Exception {
    int resId = 1;
    String apiKey = SAMPLE_API_KEY;
    String packageName = SAMPLE_PACKAGE_NAME;
    Resources resources = Mockito.mock(Resources.class);
    Mockito.when(resources.getIdentifier(GOOGLE_API_KEY_RESOURCE_KEY, GOOGLE_API_KEY_RESOURCE_TYPE,
        packageName)).thenReturn(resId);
    Context context = Mockito.mock(Context.class);
    Mockito.when(context.getResources()).thenReturn(resources);
    Mockito.when(context.getString(resId)).thenReturn(apiKey);
    Mockito.when(context.getPackageName()).thenReturn(packageName);
    Assert.assertEquals(apiKey, Utils.getGoogleApiKey(context));

    resId = 0;
    Mockito.when(resources.getIdentifier(GOOGLE_API_KEY_RESOURCE_KEY, GOOGLE_API_KEY_RESOURCE_TYPE,
        packageName)).thenReturn(resId);
    Mockito.when(context.getString(resId)).thenReturn(apiKey);
    Assert.assertEquals("", Utils.getGoogleApiKey(context));
  }

 /*
  * Tests getTopRankedPwPairByGroupId with an empty collection. Also exercises getGroupId.
  *
  * Procedure:
  *  1. Configure there to be an empty list of PwPairs to be sorted
  *  2. Verify top ranked PwPair is null
  */
  @Test
  public void testGetTopRankedPwPairByGroupIdEmptyCollection() throws Exception {
    String groupId = "";
    PhysicalWebCollection mockCollection = Mockito.mock(PhysicalWebCollection.class);
    List<PwPair> pairs = Arrays.asList(new PwPair[]{});
    Mockito.when(mockCollection.getGroupedPwPairsSortedByRank(
        (Comparator<PwPair>) Matchers.anyObject())).thenReturn(pairs);
    Assert.assertNull(Utils.getTopRankedPwPairByGroupId(mockCollection, groupId));
  }

  /*
  * Tests getTopRankedPwPairByGroupId with one item in the collection. Also exercises getGroupId.
  *
  * Procedure:
  *  1. Configure there to be an list of 1 PwPair to be sorted
  *  2. Verify top ranked PwPair for groupId that is in list is the expected pair
  *  3. Verify top ranked PwPair for groupId that is not in list is null
  */
  @Test
  public void testGetTopRankedPwPairByGroupIdOneItemCollection() throws Exception {
    String groupId = "group";
    PwsResult mockResult = Mockito.mock(PwsResult.class);
    Mockito.when(mockResult.getGroupId()).thenReturn(groupId);
    PwPair mockPair = Mockito.mock(PwPair.class);
    Mockito.when(mockPair.getPwsResult()).thenReturn(mockResult);
    PhysicalWebCollection mockCollection = Mockito.mock(PhysicalWebCollection.class);
    List<PwPair> pairs = Arrays.asList(new PwPair[]{mockPair});
    Mockito.when(mockCollection.getGroupedPwPairsSortedByRank(
        (Comparator<PwPair>) Matchers.anyObject())).thenReturn(pairs);
    Assert.assertEquals(mockPair, Utils.getTopRankedPwPairByGroupId(mockCollection, groupId));

    // Check with groupId that is not in the list of pairs.
    Assert.assertNull(Utils.getTopRankedPwPairByGroupId(mockCollection, "not in list"));
  }

  /*
  * Tests getTopRankedPwPairByGroupId with two items in the collection. Also exercises getGroupId.
  *
  * Procedure:
  *  1. Configure there to be an list of 2 PwPairs with the same ids to be sorted
  *  2. Verify top ranked PwPair for groupId that is in list is the expected pair
  *  3. Verify top ranked PwPair for groupId that is not in list is null
  */
  @Test
  public void testGetTopRankedPwPairByGroupIdTwoItemCollection() throws Exception {
    String groupId = "group";
    PwsResult mockResult1 = Mockito.mock(PwsResult.class);
    Mockito.when(mockResult1.getGroupId()).thenReturn(groupId);
    PwsResult mockResult2 = Mockito.mock(PwsResult.class);
    Mockito.when(mockResult2.getGroupId()).thenReturn(groupId);
    PwPair mockPair1 = Mockito.mock(PwPair.class);
    Mockito.when(mockPair1.getPwsResult()).thenReturn(mockResult1);
    PwPair mockPair2 = Mockito.mock(PwPair.class);
    Mockito.when(mockPair2.getPwsResult()).thenReturn(mockResult2);
    PhysicalWebCollection mockCollection = Mockito.mock(PhysicalWebCollection.class);
    List<PwPair> pairs = Arrays.asList(new PwPair[]{mockPair1, mockPair2});
    Mockito.when(mockCollection.getGroupedPwPairsSortedByRank(
        (Comparator<PwPair>) Matchers.anyObject())).thenReturn(pairs);
    Assert.assertEquals(mockPair1, Utils.getTopRankedPwPairByGroupId(mockCollection, groupId));

    // Check with groupId that is not in the list of pairs.
    Assert.assertNull(Utils.getTopRankedPwPairByGroupId(mockCollection, "not in list"));
  }

  /*
  * Tests getBitmapIcon.
  *
  * Procedure:
  *  1. Configure there to be a null icon
  *  2. Verify null is returned
  */
  @Test
  public void testGetBitmapIcon() throws Exception {
    String iconUrl = "icon";
    PwsResult mockResult = Mockito.mock(PwsResult.class);
    Mockito.when(mockResult.getIconUrl()).thenReturn(iconUrl);
    PhysicalWebCollection mockCollection = Mockito.mock(PhysicalWebCollection.class);
    Mockito.when(mockCollection.getIcon(iconUrl)).thenReturn(null);
    Assert.assertNull(Utils.getBitmapIcon(mockCollection, mockResult));
  }

  /*
   * Tests getScanTimeMillis.
   *
   * Procedure:
   *  1. Configure mock device to return scanTime
   *  2. Verify scanTime is returned
   */
  @Test
  public void testGetScanTimeMillis() throws Exception {
    long scanTime = 0L;
    UrlDevice mockDevice = Mockito.mock(UrlDevice.class);
    Mockito.when(mockDevice.getExtraLong(SCAN_TIME_KEY)).thenReturn(scanTime);
    Assert.assertEquals(scanTime, Utils.getScanTimeMillis(mockDevice));
  }

  /*
   * Tests getScanTimeMillis.
   *
   * Procedure:
   *  1. Configure mock device to throw JSONException
   *  2. Verify RuntimeException is thrown
   */
  @Test(expected = RuntimeException.class)
  public void testGetScanTimeMillisWithException() throws Exception {
    UrlDevice mockDevice = Mockito.mock(UrlDevice.class);
    Mockito.when(mockDevice.getExtraLong(SCAN_TIME_KEY)).thenThrow(new JSONException(""));
    Utils.getScanTimeMillis(mockDevice);
  }

  /*
   * Tests isPublic.
   *
   * Procedure:
   *  1. Configure mock device to return true
   *  2. Verify true is returned
   *  3. Configure mock device to return false
   *  4. Verify false is returned
   */
  @Test
  public void testIsPublic() throws Exception {
    UrlDevice mockDevice = Mockito.mock(UrlDevice.class);
    Mockito.when(mockDevice.optExtraBoolean(PUBLIC_KEY, true)).thenReturn(true);
    Assert.assertTrue(Utils.isPublic(mockDevice));

    Mockito.when(mockDevice.optExtraBoolean(PUBLIC_KEY, true)).thenReturn(false);
    Assert.assertFalse(Utils.isPublic(mockDevice));
  }

  /*
   * Tests isBleUrlDevice.
   *
   * Procedure:
   *  1. Configure mock device return RSSI
   *  2. Verify true is returned
   *  3. Configure mock device to throw JSONException
   *  4. Verify false is returned
   */
  @Test
  public void testIsBleUrlDevice() throws Exception {
    UrlDevice mockDevice = Mockito.mock(UrlDevice.class);
    Mockito.when(mockDevice.optExtraString(TYPE_KEY, "")).thenReturn(BLE_DEVICE_TYPE);
    Assert.assertTrue(Utils.isBleUrlDevice(mockDevice));

    Mockito.when(mockDevice.optExtraString(TYPE_KEY, "")).thenReturn("not ble");
    Assert.assertFalse(Utils.isBleUrlDevice(mockDevice));
  }

  /*
   * Tests getRssi.
   *
   * Procedure:
   *  1. Configure mock device to return RSSI
   *  2. Verify RSSI is returned
   */
  @Test
  public void testGetRssi() throws Exception {
    int rssi = Integer.MAX_VALUE;
    UrlDevice mockDevice = Mockito.mock(UrlDevice.class);
    Mockito.when(mockDevice.getExtraInt(RSSI_KEY)).thenReturn(rssi);
    Assert.assertEquals(rssi, Utils.getRssi(mockDevice));
  }

  /*
   * Tests getRssi.
   *
   * Procedure:
   *  1. Configure mock device to throw JSONException
   *  2. Verify RuntimeException is thrown
   */
  @Test(expected = RuntimeException.class)
  public void testGetRssiWithException() throws Exception {
    UrlDevice mockDevice = Mockito.mock(UrlDevice.class);
    Mockito.when(mockDevice.getExtraInt(RSSI_KEY)).thenThrow(new JSONException(""));
    Utils.getRssi(mockDevice);
  }

  /*
   * Tests getTxPower.
   *
   * Procedure:
   *  1. Configure mock device to return Tx Power
   *  2. Verify Tx Power is returned
   */
  @Test
  public void testGetTxPower() throws Exception {
    int txPower = Integer.MIN_VALUE;
    UrlDevice mockDevice = Mockito.mock(UrlDevice.class);
    Mockito.when(mockDevice.getExtraInt(TXPOWER_KEY)).thenReturn(txPower);
    Assert.assertEquals(txPower, Utils.getTxPower(mockDevice));
  }

  /*
   * Tests getTxPower.
   *
   * Procedure:
   *  1. Configure mock device to throw JSONException
   *  2. Verify RuntimeException is thrown
   */
  @Test(expected = RuntimeException.class)
  public void testGetTxPowerWithException() throws Exception {
    UrlDevice mockDevice = Mockito.mock(UrlDevice.class);
    Mockito.when(mockDevice.getExtraInt(TXPOWER_KEY)).thenThrow(new JSONException(""));
    Utils.getTxPower(mockDevice);
  }

  /*
   * Tests getPwsTripTimeMillis.
   *
   * Procedure:
   *  1. Configure mock result to return trip time
   *  2. Verify trip time is returned
   */
  @Test
  public void testGetPwsTripTimeMillis() throws Exception {
    long tripTime = Long.MAX_VALUE;
    PwsResult mockResult = Mockito.mock(PwsResult.class);
    Mockito.when(mockResult.getExtraLong(PWS_TRIP_TIME_KEY)).thenReturn(tripTime);
    Assert.assertEquals(tripTime, Utils.getPwsTripTimeMillis(mockResult));
  }

  /*
   * Tests getPwsTripTimeMillis.
   *
   * Procedure:
   *  1. Configure mock result to throw JSONException
   *  2. Verify RuntimeException is thrown
   */
  @Test(expected = RuntimeException.class)
  public void testGetPwsTripTimeMillisWithException() throws Exception {
    PwsResult mockResult = Mockito.mock(PwsResult.class);
    Mockito.when(mockResult.getExtraLong(PWS_TRIP_TIME_KEY)).thenThrow(new JSONException(""));
    Utils.getPwsTripTimeMillis(mockResult);
  }

  /*
   * Tests getGroupId.
   *
   * Procedure:
   *  1. Configure mock result to return groupId
   *  2. Verify groupId is returned
   */
  @Test
  public void testGetGroupId() throws Exception {
    String groupId = "group";
    PwsResult mockResult = Mockito.mock(PwsResult.class);
    Mockito.when(mockResult.getGroupId()).thenReturn(groupId);
    Assert.assertEquals(groupId, Utils.getGroupId(mockResult));
  }

  /*
   * Tests getGroupId.
   *
   * Procedure:
   *  1. Configure mock result to return: {groupId:null, title:non Empty, siteUrl:https://url}
   *  2. Verify url + title is returned
   *  3. Configure mock result to return: {groupId:empty string, title:non Empty,
   *  siteUrl:url containing '\'}
   *  4. Verify url is returned
   */
  @Test
  public void testGetGroupIdNotDefined() throws Exception {
    String url = siteUrls.get(0);
    String title = "Google";
    PwsResult mockResult = Mockito.mock(PwsResult.class);

    // Test null groupId and use site url that URI can parse
    Mockito.when(mockResult.getGroupId()).thenReturn(null);
    Mockito.when(mockResult.getSiteUrl()).thenReturn("https://" + url);
    Mockito.when(mockResult.getTitle()).thenReturn(title);
    Assert.assertEquals(url + title, Utils.getGroupId(mockResult));

    // Test empty groupId and use site url that causes URISyntaxException
    url = "\\invalid";
    Mockito.when(mockResult.getSiteUrl()).thenReturn(url);
    Mockito.when(mockResult.getGroupId()).thenReturn("");
    Assert.assertEquals(url, Utils.getGroupId(mockResult));
  }
}
