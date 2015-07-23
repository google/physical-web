package org.physical_web.physicalweb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.physical_web.physicalweb.wifi.UrlUtil;

import org.junit.Test;

/**
 * @author Jonas Sevcik
 */
public class UrlUtilTest {

  @Test
  public void testContainsUrl() {
    assertTrue(UrlUtil.containsUrl("http://myurl.com"));
    assertTrue(UrlUtil.containsUrl(" http://myurl.com "));
    assertTrue(UrlUtil.containsUrl("https://myurl.com"));
    assertTrue(UrlUtil.containsUrl(" https://myurl.com "));
    assertTrue(UrlUtil.containsUrl("Some text http://myurl.com"));
    assertTrue(UrlUtil.containsUrl("http://myurl.com some text"));
    assertTrue(UrlUtil.containsUrl("Some text http://myurl.com some other text"));
    assertTrue(UrlUtil.containsUrl("localhost http://127.0.0.1"));
    assertTrue(UrlUtil.containsUrl("port http://127.0.0.1:80"));
    assertTrue(UrlUtil.containsUrl("port http://127.0.0.1:8080"));
    assertTrue(UrlUtil.containsUrl("http://user:password@127.0.0.1:8080"));
    assertTrue(UrlUtil.containsUrl(
        "full URL http://user:password@127.0.0.1:8080/path?query_string#fragment_id"));
    assertTrue(UrlUtil.containsUrl(
        "full URL http://user:password@127.0.0.1:8080/path?query_string#fragment_id suffix"));
    assertTrue(
        UrlUtil.containsUrl("http://user:password@127.0.0.1:8080/path?query_string#fragment_id"));

    assertFalse(UrlUtil.containsUrl("SSID without URL"));
    assertFalse(UrlUtil.containsUrl("SSID with malformed URL http://"));
    assertFalse(UrlUtil.containsUrl("SSID with malformed URL http://almosturl"));
    assertFalse(UrlUtil.containsUrl("SSID with malformed URL http://almostur.l"));
    assertFalse(UrlUtil.containsUrl("SSID with malformed URL http://127.0.0.1.1"));
  }

  @Test
  public void testExtractUrl() {
    assertNull(UrlUtil.extractUrl("SSID without URL"));
    assertNull(UrlUtil.extractUrl("SSID with malformed URL http://"));
    assertNull(UrlUtil.extractUrl("SSID with malformed URL http://almosturl"));
    assertNull(UrlUtil.extractUrl("SSID with malformed URL http://almostur.l"));

    String expected = "http://myurl.com";
    assertEquals(expected, UrlUtil.extractUrl("http://myurl.com"));
    assertEquals(expected, UrlUtil.extractUrl(" http://myurl.com "));
    assertEquals(expected, UrlUtil.extractUrl("    http://myurl.com    "));
    assertEquals(expected, UrlUtil.extractUrl("Some text http://myurl.com"));
    assertEquals(expected, UrlUtil.extractUrl("http://myurl.com some text"));
    assertEquals(expected,
                 UrlUtil.extractUrl("Some text http://myurl.com some other text"));
    assertEquals(expected, UrlUtil.extractUrl("http://myurl.com https://mysecondurl.com"));
    assertEquals("http://user:password@myurl.com:8080/path/",
                 UrlUtil.extractUrl("prefix http://user:password@myurl.com:8080/path/ suffix"));
    assertEquals("http://user:password@myurl.com:8080/",
                 UrlUtil.extractUrl("prefix http://user:password@myurl.com:8080/ suffix"));
    assertEquals("http://user:password@myurl.com:8080",
                 UrlUtil.extractUrl("prefix http://user:password@myurl.com:8080 suffix"));
    assertEquals("http://myurl.com:8080/path/",
                 UrlUtil.extractUrl("prefix http://myurl.com:8080/path/ suffix"));
    assertEquals("http://user:password@myurl.com:8080/path?query_string#fragment_id", UrlUtil
        .extractUrl(
            "full URL http://user:password@myurl.com:8080/path?query_string#fragment_id suffix"));

    expected = "https://myurl.com";
    assertEquals(expected, UrlUtil.extractUrl("https://myurl.com"));
    assertEquals(expected, UrlUtil.extractUrl(" https://myurl.com "));
    assertEquals(expected, UrlUtil.extractUrl("Some text https://myurl.com"));
    assertEquals(expected, UrlUtil.extractUrl("https://myurl.com some text"));
    assertEquals(expected, UrlUtil.extractUrl("Some text https://myurl.com some other text"));
    assertEquals(expected, UrlUtil.extractUrl("https://myurl.com http://mysecondurl.com"));
  }
}
