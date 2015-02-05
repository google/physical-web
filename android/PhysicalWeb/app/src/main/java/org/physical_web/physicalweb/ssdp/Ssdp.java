package org.physical_web.physicalweb.ssdp;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;

/**
 * Created by lba on 29.01.2015.
 */
public class Ssdp implements Runnable {
    public static String TAG = "SSDP";
    public static String SSDP_ADDRESS = "239.255.255.250";
    public static int SSDP_PORT = 1900;
    public static int SSDP_SOURCE_PORT = 1901;
    public static String SSDP_HOST = SSDP_ADDRESS + ":" + SSDP_PORT;
    public static String MAX_AGE = "max-age=1800";
    public static int TTL = 128;
    public static int MX = 2;
    public static String ALIVE = "ssdp:alive";
    public static String BYEBYE = "ssdp:byebye";
    public static String UPDATE = "ssdp:update";
    public static String TYPE_M_SEARCH = "M-SEARCH";
    public static String TYPE_NOTIFY = "NOTIFY";
    public static String TYPE_200_OK = "200 OK";

    private SsdpCallback mSsdpCallback;
    private SocketAddress mMulticastGroup;
    //private MulticastSocket mMulticastSocket;
    private DatagramSocket mMulticastSocket;
    private Thread mThread;
    public Ssdp(SsdpCallback ssdpCallback) throws IOException{
        mSsdpCallback = ssdpCallback;
        mMulticastGroup = new InetSocketAddress(SSDP_ADDRESS,SSDP_PORT);
        mMulticastSocket = new MulticastSocket(SSDP_PORT);
        //mMulticastSocket = new DatagramSocket(SSDP_PORT);
        //mMulticastSocket.setSoTimeout((MX+1)*1000);
    }

    public synchronized void start() throws IOException{
        //stop();
        if (mThread == null){
            mThread = new Thread(this);
            mThread.start();
        }
    }

    public synchronized void stop() throws IOException{
        if(mThread != null){
            mThread.interrupt();
            mThread = null;
        }
    }

    public void search(SsdpMessage msg) throws IOException{
        if(mMulticastSocket != null){
            byte bytes[] = msg.toString().getBytes();
            DatagramPacket dp = new DatagramPacket(bytes, bytes.length, this.mMulticastGroup);
            mMulticastSocket.send(dp);
        }
    }

    public SsdpMessage search(String ST) throws IOException{
        SsdpMessage msg = new SsdpMessage(SsdpMessage.TYPE_SEARCH);
        msg.getHeaders().put("ST",ST);
        msg.getHeaders().put("HOST",SSDP_HOST);
        msg.getHeaders().put("MAN","\"ssdp:discover\"");
        msg.getHeaders().put("MX",MX+"");
        search(msg);
        return msg;
    }

    @Override
    public void run() {
        Thread currentThread = Thread.currentThread();
        byte[] buf = new byte[1024];
        Log.i(TAG,"SSDP Thread started");
        while (!currentThread.isInterrupted()){
            try {
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                mMulticastSocket.receive(dp);
                String txt = new String(dp.getData());
                SsdpMessage msg = new SsdpMessage(txt);
                mSsdpCallback.onSsdpMessageReceived(msg);
            }
            catch (IOException e){
                Log.e(TAG,"SSDP Thread socket timeout");
            }
        }
        Log.i(TAG,"SSDP Thread terminated");

    }

    public interface SsdpCallback {
        public void onSsdpMessageReceived(SsdpMessage ssdpMessage);
        
    }
}
