/*
 * Copyright 2014 Fraunhofer FOKUS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * AUTHOR: Louay Bassbouss <louay.bassbouss@fokus.fraunhofer.de>
 *
 */
package org.physical_web.physicalweb.ssdp;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

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
    }

    public synchronized boolean start(Integer timeout) throws IOException{
        if (mThread == null){
            mMulticastSocket = new DatagramSocket(SSDP_PORT);
            if (timeout != null && timeout>0){
                mMulticastSocket.setSoTimeout(timeout);
            }
            mThread = new Thread(this);
            mThread.start();
            return true;
        }
        return false;
    }

    public synchronized boolean stop() throws IOException{
        if(mThread != null){
            mThread.interrupt();
            mMulticastSocket.close();
            mThread = null;
            mMulticastSocket = null;
            return true;
        }
        return false;
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
        Log.i(TAG,"SSDP Scan Thread started");
        while (!currentThread.isInterrupted() && mMulticastSocket!=null){
            try {
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                mMulticastSocket.receive(dp);
                String txt = new String(dp.getData());
                SsdpMessage msg = new SsdpMessage(txt);
                mSsdpCallback.onSsdpMessageReceived(msg);
            }
            catch (SocketTimeoutException e){
                // leave loop on socket timeout
                break;
            }
            catch (IOException e){
                Log.e(TAG,"SSDP Scan Thread: Socket timeout or closed manually");
            }
        }
        // cleanup if needed
        synchronized (this){
            if (mThread == currentThread){
                mThread = null;
                if (mMulticastSocket != null){
                    mMulticastSocket.close();
                    mMulticastSocket = null;
                }
            }
        }
        Log.i(TAG,"SSDP Scan Thread terminated");

    }

    public interface SsdpCallback {
        public void onSsdpMessageReceived(SsdpMessage ssdpMessage);
        
    }
}
