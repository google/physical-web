// /*
//  * Copyright 2014 Google Inc. All rights reserved.
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *      http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */

// package org.physical_web.physicalweb;

// import android.content.Context;
// import android.net.nsd.NsdManager;
// import android.net.nsd.NsdServiceInfo;
// import android.util.Log;
// import android.webkit.URLUtil;

// import java.util.HashMap;
// import java.util.Map;
// import android.content.IntentFilter;
// import android.net.wifi.p2p.WifiP2pConfig;
// import android.net.wifi.p2p.WifiP2pDevice;
// import android.net.wifi.p2p.WifiP2pManager;
// import android.net.wifi.p2p.WifiP2pManager.ActionListener;
// import android.net.wifi.p2p.WifiP2pManager.Channel;
// import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
// import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
// import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;

// class WifiUrlDeviceDiscoverer extends UrlDeviceDiscoverer {
//   private static final String TAG = "WifiDirect";
//   private WifiP2pManager mManager;
//   private Channel mChannel;
//   // private WiFiDirectBroadcastReceiver mReceiver;

//   public WifiUrlDeviceDiscoverer(Context context) {
//     mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
//     mChannel = mManager.initialize(context, context.getMainLooper(), null);
//     discoverService();
//   }

//   @Override
//   public synchronized void startScanImpl() {
//     WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
//     mManager.addServiceRequest(mChannel,
//             serviceRequest,
//             new ActionListener() {
//                 @Override
//                 public void onSuccess() {
//                     Log.d(TAG, "addServiceRequest");
//                 }

//                 @Override
//                 public void onFailure(int code) {
//                     // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
//                 }
//             });
//     mManager.discoverServices(mChannel, new ActionListener() {

//       @Override
//       public void onSuccess() {
//           Log.d(TAG, "discoverServices");
//       }

//       @Override
//       public void onFailure(int code) {
//           // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
//           if (code == WifiP2pManager.P2P_UNSUPPORTED) {
//               Log.d(TAG, "P2P isn't supported on this device.");

//             }
//       }
//   });  
//   }

//   @Override 
//   public synchronized void stopScanImpl() {
//     mManager.clearServiceRequests(mChannel,
//             new ActionListener() {
//                 @Override
//                 public void onSuccess() {
//                     // Success!
//                 }

//                 @Override
//                 public void onFailure(int code) {
//                     // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
//                 }
//             });
//   }

//   final HashMap<String, String> buddies = new HashMap<String, String>();

//   private void discoverService() {
//     WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
//     @Override
//     /* Callback includes:
//      * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
//      * record: TXT record dta as a map of key/value pairs.
//      * device: The device running the advertised service.
//      */

//     public void onDnsSdTxtRecordAvailable(
//             String fullDomain, Map record, WifiP2pDevice device) {
//             Log.d(TAG, "DnsSdTxtRecord available -" + record.toString());
//             buddies.put(device.deviceAddress, (String) record.get("Title"));
//             reportUrlDevice(createUrlDeviceBuilder((String)record.get("URL")+(String)record.get("Title"), (String)record.get("URL"))
//                 .setPrivate()
//                 .setTitle((String)record.get("Title"))
//                 .setDescription((String)record.get("Desc"))
//                 .build());

//         }
//     };

//     WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
//       @Override
//       public void onDnsSdServiceAvailable(String instanceName, String registrationType,
//               WifiP2pDevice resourceType) {
//               Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
//       }
//     };

//     mManager.setDnsSdResponseListeners(mChannel, servListener, txtListener);
//   }
// }
