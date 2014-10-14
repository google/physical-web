package physical_web.org.physicalweb;

import android.bluetooth.BluetoothDevice;
import org.uribeacon.beacon.UriBeacon;
import java.util.ArrayList;

public class Device {

  private BluetoothDevice mBluetoothDevice;
  private UriBeacon mUriBeacon;
  public ArrayList<Integer> mRssiHistory;
  private int MAX_LENGTH_RSSI_HISTORY = 6;

  public Device(UriBeacon uriBeacon, BluetoothDevice bluetoothDevice, int rssi) {
    mUriBeacon = uriBeacon;
    mBluetoothDevice = bluetoothDevice;
    mRssiHistory = new ArrayList<>();
    mRssiHistory.add(rssi);
  }

  public BluetoothDevice getBluetoothDevice() { return mBluetoothDevice; }

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
