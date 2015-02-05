package org.physical_web.physicalweb;

import android.content.Context;
import android.util.Log;

import org.physical_web.physicalweb.ssdp.Ssdp;
import org.physical_web.physicalweb.ssdp.SsdpMessage;

import java.io.IOException;

/**
 * Created by lba on 03.02.2015.
 */
public class SsdpUrlDiscoverer implements Ssdp.SsdpCallback {
    private static final String TAG = "SSDP";
    private static final String PHYSICAL_WEB_SSDP_TYPE = "urn:physical-web-org:device:Basic:1";
    private Context mContext;
    private SsdpUrlDiscovererCallback mSsdpUrlDiscovererCallback;
    private Thread mThread;
    private Ssdp mSsdp;
    public SsdpUrlDiscoverer(Context context, SsdpUrlDiscovererCallback ssdpUrlDiscovererCallback){
        mContext = context;
        mSsdpUrlDiscovererCallback = ssdpUrlDiscovererCallback;
    }

    public void startScanning() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    getSsdp().start();
                    getSsdp().search(PHYSICAL_WEB_SSDP_TYPE);
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void stopScanning() {
        /*new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    getSsdp().stop();
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();*/
    }

    public synchronized Ssdp getSsdp() throws IOException{
        if (mSsdp == null){
            mSsdp = new Ssdp(this);
        }
        return mSsdp;
    }

    @Override
    public void onSsdpMessageReceived(SsdpMessage ssdpMessage) {
        final String url = ssdpMessage.get("LOCATION");
        final String st = ssdpMessage.get("ST");
        if(url!=null && PHYSICAL_WEB_SSDP_TYPE.equals(st)){
            Log.i(TAG, "ssdp url received "+url);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mSsdpUrlDiscovererCallback.onSsdpUrlFound(url);
                }
            }).start();
        }
    }

    public interface SsdpUrlDiscovererCallback{
        public void onSsdpUrlFound(String url);
    }
}
