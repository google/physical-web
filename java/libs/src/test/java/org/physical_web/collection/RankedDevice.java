package org.physical_web.collection;

/**
 * A mock UrlDevice with a configurable rank value.
 */
public class RankedDevice extends SimpleUrlDevice {
  private double mRank;

  public RankedDevice(String id, String url, double rank) {
    super(id, url);
    mRank = rank;
  }

  @Override
  public double getRank(PwsResult pwsResult) {
    return mRank;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    return super.equals(other);
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
