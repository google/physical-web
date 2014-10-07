package physical_web.org.physicalweb;

import android.bluetooth.BluetoothDevice;
import org.uribeacon.beacon.UriBeacon;
import java.util.ArrayList;

public class Device {

  private BluetoothDevice mBluetoothDevice;
  private UriBeacon mUriBeacon;
  private MetadataResolver.DeviceMetadata mDeviceMetadata;
  public ArrayList<Integer> mRssiHistory;
  private int MAX_LENGTH_RSSI_HISTORY = 6;
  private String mLongUrl;
  private String mShortUrl;

  public Device(UriBeacon uriBeacon, BluetoothDevice bluetoothDevice, int rssi) {
    mUriBeacon = uriBeacon;
    mBluetoothDevice = bluetoothDevice;
    mRssiHistory = new ArrayList<>();
    initializeUrl();
  }

  private void initializeUrl() {
    if (mUriBeacon == null) {
      return;
    }

    String longUrl;
    String shortUrl = null;
    String url = mUriBeacon.getUriString();
    // If this is a shortened url
    if (UrlShortener.isShortUrl(url)) {
      shortUrl = url;
      // Expand the url to it's original url
      longUrl = UrlShortener.lengthenShortUrl(url);
    }
    else {
      longUrl = url;
    }
    mShortUrl = shortUrl;
    mLongUrl = longUrl;
  }

  public BluetoothDevice getBluetoothDevice() { return mBluetoothDevice; }
  public UriBeacon getUriBeacon() { return mUriBeacon; }
  public MetadataResolver.DeviceMetadata getMetadata() { return mDeviceMetadata; }
  public String getLongUrl() { return mLongUrl; }
  public void setMetadata(MetadataResolver.DeviceMetadata deviceMetadata) { mDeviceMetadata = deviceMetadata; }
  private ArrayList<Integer> getRssiHistory() { return mRssiHistory; }

  public void updateRssiHistory(int rssi) {
    getRssiHistory().add(rssi);
    if (getRssiHistory().size() > MAX_LENGTH_RSSI_HISTORY) {
      getRssiHistory().remove(0);
    }
  }

  public int calculateAverageRssi() {
    if (getRssiHistory().size() == 0) {
      return 0;
    }
    int rssiSum = 0;
    for (int rssi : getRssiHistory()) {
      rssiSum += rssi;
    }
    return (rssiSum / getRssiHistory().size());
  }
}
