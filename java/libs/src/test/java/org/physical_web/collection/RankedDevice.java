package org.physical_web.collection;

/**
 * A mock UrlDevice with a configurable rank value.
 */
class RankedDevice implements UrlDevice {
  private String mId;
  private String mUrl;
  private double mRank;

  RankedDevice(String id, String url, double rank) {
    mId = id;
    mUrl = url;
    mRank = rank;
  }

  public String getId() {
    return mId;
  }

  public String getUrl() {
    return mUrl;
  }

  public double getRank(PwsResult pwsResult) {
    return mRank;
  }

  /**
   * Utility method for constructing a PwPair with a RankedDevice.
   * @param id URL device ID
   * @param url Broadcast URL
   * @param groupId URL group ID
   * @param rank Rank value for this device
   * @return New PwPair
   */
  public static PwPair createRankedPair(String id, String url, String groupId, double rank) {
    UrlDevice urlDevice = new RankedDevice(id, url, rank);
    PwsResult pwsResult = new PwsResult(url, url, groupId);
    return new PwPair(urlDevice, pwsResult);
  }
}
