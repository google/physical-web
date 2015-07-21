package org.physical_web.physicalweb;

import android.content.Context;
import android.net.wifi.ScanResult;

import org.physical_web.physicalweb.wifi.ContinuousReceiver;
import org.physical_web.physicalweb.wifi.UrlUtil;

import java.util.List;

/**
 * @author Jonas Sevcik
 */
public class SsidPwoDiscoverer extends PwoDiscoverer implements ContinuousReceiver.ScanResultsListener {

  private static final String TAG = SsidPwoDiscoverer.class.getSimpleName();

  private ContinuousReceiver mScanReceiver;

  public SsidPwoDiscoverer(Context context) {
    mScanReceiver = new ContinuousReceiver(context, this);
  }

  @Override
  public void startScanImpl() {
    mScanReceiver.startScanning(false);
  }

  @Override
  public void stopScanImpl() {
    mScanReceiver.stopScanning();
  }

  @Override
  public void onScanResultsReceived(List<ScanResult> results) {
    for (int i = 0, size = results.size(); i < size; i++) {
      ScanResult scanResult = results.get(i);
      if (UrlUtil.containsUrl(scanResult.SSID)) {
        String url = UrlUtil.extractUrl(scanResult.SSID);
        if (url != null) {
          PwoMetadata pwoMetadata = createPwoMetadata(url);
          reportPwo(pwoMetadata);
        }
      }
    }
  }
}
