package org.physical_web.physicalweb.wifi;

import android.support.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jonas Sevcik
 */
public final class UrlUtil {

    private static final String URL_REGEX = "(http|https)://([a-zA-Z0-9_]+:[a-zA-Z0-9_]+@)?"
            + "(([a-zA-Z0-9.-]+\\.[A-Za-z]{2,4})|([0-9]+\\.){3}[0-9]{1})(:[0-9]+)?(/\\S*)?";
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);

    private UrlUtil() {
        //don't instantiate
    }

    public static boolean containsUrl(String ssid) {
        return ssid.matches("(.+\\s|\\s)*" + URL_REGEX + "(\\s.+|\\s)*");
    }

    /**
     * Extracts URL from a String.
     *
     * @param ssid containing URL
     * @return first found url; if {@code ssid} doesn't contain URL, returns null
     */
    @Nullable
    public static String extractUrl(String ssid) {
        Matcher matcher = URL_PATTERN.matcher(ssid);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
