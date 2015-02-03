package org.physical_web.physicalweb;

import android.content.Context;
import android.util.Log;

/**
 * Created by lba on 03.02.2015.
 */
public class SsdpUrlDiscoverer {
    private static final String TAG = "SsdpUrlDiscoverer";
    private Context mContext;
    private SsdpUrlDiscovererCallback mSsdpUrlDiscovererCallback;
    private Thread mThread;
    public SsdpUrlDiscoverer(Context context, SsdpUrlDiscovererCallback ssdpUrlDiscovererCallback){
        mContext = context;
        mSsdpUrlDiscovererCallback = ssdpUrlDiscovererCallback;
    }

    public void startScanning() {
        stopScanning();
        mThread = new Thread(new Runnable() {
            @Override
            public synchronized void run() {
                try {
                    wait(1000);
                    mSsdpUrlDiscovererCallback.onSsdpUrlFound("http://www.fokus.fraunhofer.de");
                    wait(500);
                    mSsdpUrlDiscovererCallback.onSsdpUrlFound("http://www.fokus.fraunhofer.de/fame");
                } catch (InterruptedException e) {
                    Log.i(TAG,"SSDP Discoverer Thread Interrupted");
                }
            }
        });
        mThread.start();
    }

    public void stopScanning() {
        if (mThread != null){
            mThread.interrupt();
            mThread = null;
        }
    }
    
    public interface SsdpUrlDiscovererCallback{
        public void onSsdpUrlFound(String url);
    }
}
