package org.physical_web.physicalweb.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.List;

/**
 * @author Jonas Sevcik
 */
public class ContinuousReceiver extends BroadcastReceiver {

  private static final String TAG = ContinuousReceiver.class.getSimpleName();

  public static final int INTERVAL_IMMEDIATE = 0;
  private Handler mScanHandler = new Handler();
  @NonNull private Context mContext;
  private WifiManager mManager;
  @NonNull private ScanResultsListener mListener;
  private boolean mContinueScanning = false;

  private long mLastScanTime = 0;
  private int mScanInterval; //millis
  private Runnable mScanRunnable = new Runnable() {
    @Override
    public void run() {
      initiateScan();
    }
  };

  /**
   * Default constructor for delivering results immediately.
   *
   * @param context  cannot be null
   * @param listener for delivering results to. Cannot be null
   */
  public ContinuousReceiver(@NonNull Context context, @NonNull ScanResultsListener listener) {
    this(context, listener, INTERVAL_IMMEDIATE);
  }

  /**
   * Constructor used when needed to specify ScanResult delivery rate.
   *
   * Note that the {@link android.net.wifi.ScanResult} updates may be faster than this rate if
   * another app is receiving updates at a faster rate, or slower than this rate, or there may be no
   * updates at all (if the device has no connectivity, for example).
   *
   * @param context      cannot be null
   * @param listener     for delivering results to. Cannot be null
   * @param scanInterval preferred delay between scans [millis]. Cannot be negative
   * @throws NullPointerException     if {@code context} or {@code listener} is null
   * @throws IllegalArgumentException if {@code scanInterval} is negative
   */
  public ContinuousReceiver(@NonNull Context context, @NonNull ScanResultsListener listener, int scanInterval) {
    if (context == null) {
      throw new NullPointerException("mContext cannot be null");
    }
    if (listener == null) {
      throw new NullPointerException("mListener cannot be null");
    }
    if (scanInterval < 0) {
      throw new IllegalArgumentException("mScanInterval cannot be negative");
    }
    mContext = context;
    mListener = listener;
    mScanInterval = scanInterval;
    mManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
  }

  @Override
  public final void onReceive(Context context, Intent intent) {
    if (!WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
      throw new IllegalStateException("ContinuousReceiver registered for wrong action: " + intent.getAction());
    }

    if (mContinueScanning) {
      initiateScan();
      mListener.onScanResultsReceived(mManager.getScanResults());
    }
  }

  private void initiateScan() {
    long scanTime = System.currentTimeMillis();

    if (mContinueScanning) {
      long scanDelay = scanTime - mLastScanTime;
      if (mScanInterval == INTERVAL_IMMEDIATE || scanDelay >= mScanInterval) {
        mManager.startScan();
        mLastScanTime = scanTime;
      } else {
        mScanHandler.removeCallbacks(mScanRunnable);
        mScanHandler.postDelayed(mScanRunnable, mScanInterval - scanDelay);
      }
    }
  }

  /**
   * This method sets the rate in milliseconds at which your app prefers to receive ScanResult
   * updates.
   *
   * Note that the {@link android.net.wifi.ScanResult} updates may be faster than this rate if
   * another app is receiving updates at a faster rate, or slower than this rate, or there may be no
   * updates at all (if the device has no connectivity, for example).
   *
   * @param scanInterval preferred delay between scans [millis]. Cannot be negative
   * @throws IllegalArgumentException if {@code scanInterval} is negative
   */
  public void changeScanInterval(int scanInterval) {
    if (scanInterval < 0) {
      throw new IllegalArgumentException("mScanInterval cannot be negative");
    }
    mScanInterval = scanInterval;
  }

  /**
   * Initiate WiFi scan. Don't forget to unregister from receiving scan updates by calling {@link
   * #stopScanning()}
   *
   * @param publishCachedResultsInstantly publish ScanResults from the most recent scan immediately
   */
  public void startScanning(boolean publishCachedResultsInstantly) {
    if (mContinueScanning) {
      Log.w(TAG, "Scanning already in progress");
      return;
    }
    mContext.registerReceiver(this, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    mContinueScanning = true;
    mManager.startScan();
    mLastScanTime = System.currentTimeMillis();
    if (publishCachedResultsInstantly) {
      mListener.onScanResultsReceived(mManager.getScanResults()); //instantly publish old values
    }
  }

  /**
   * Stop previously initiated WiFi scan.
   */
  public void stopScanning() {
    if (mContinueScanning) {
      mScanHandler.removeCallbacks(mScanRunnable);
      mContinueScanning = false;
      mContext.unregisterReceiver(this);
    }
  }

  /**
   * Interface used for delivering ScanResults
   */
  public interface ScanResultsListener {
    void onScanResultsReceived(List<ScanResult> results);
  }
}
