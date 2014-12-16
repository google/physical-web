package org.physical_web.physicalweb;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import android.webkit.URLUtil;

class MdnsUrlDiscoverer {

  private static final String TAG = "MdnsUrlDiscoverer";
  private static final String MDNS_SERVICE_TYPE = "_http._tcp.";
  private NsdManager mNsdManager;
  private NsdManager.DiscoveryListener mDiscoveryListener;
  private Context mContext;
  private MdnsUrlDiscovererCallback mMdnsUrlDiscovererCallback;


  public MdnsUrlDiscoverer(Context context, MdnsUrlDiscovererCallback mdnsUrlDiscovererCallback) {
    mContext = context;
    mMdnsUrlDiscovererCallback = mdnsUrlDiscovererCallback;

    initialize();
  }

  private void initialize() {
    initializeNetworkServiceDiscovery();
  }

  public void initializeNetworkServiceDiscovery() {
    mNsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);

    mDiscoveryListener = new NsdManager.DiscoveryListener() {

      @Override
      public void onDiscoveryStarted(String regType) {
        Log.d(TAG, "Service discovery started");
      }

      @Override
      public void onServiceFound(NsdServiceInfo service) {
        Log.d(TAG, "Service discovery success" + service);
        String name = service.getServiceName();
        if (URLUtil.isNetworkUrl(name)) {
          mMdnsUrlDiscovererCallback.onMdnsUrlFound(name);
        }
      }

      @Override
      public void onServiceLost(NsdServiceInfo service) {
        Log.e(TAG, "service lost" + service);
      }

      @Override
      public void onDiscoveryStopped(String serviceType) {
        Log.i(TAG, "Discovery stopped: " + serviceType);
      }

      @Override
      public void onStartDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "Discovery failed: Error code:" + errorCode);
        mNsdManager.stopServiceDiscovery(this);
      }

      @Override
      public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "Discovery failed: Error code:" + errorCode);
        mNsdManager.stopServiceDiscovery(this);
      }
    };
  }

  public void startScanning() {
    mNsdManager.discoverServices(MDNS_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
  }

  public void stopScanning() {
    mNsdManager.stopServiceDiscovery(mDiscoveryListener);
  }

  public interface MdnsUrlDiscovererCallback {
    public void onMdnsUrlFound(String url);
  }
}