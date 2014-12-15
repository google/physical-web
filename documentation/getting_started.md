# Getting started testing the Physical Web

In order to get up and running you need two things:

1. A hardware beacon
2. Software on your phone/tablet to see that beacon.

The software is the easiest thing to take care of. The app is on the [Google Play store](https://play.google.com/store/apps/details?id=physical_web.org.physicalweb) as well as the [Apple App Store](https://itunes.apple.com/us/app/physical-web/id927653608?mt=8). The source code is also located in this repo with the latest list of the [Android releases.](https://github.com/google/physical-web/releases) If you install the Android app from the releases page, just make sure you go to Settings>Security>Unknown Sources and flip the checkbox on before you do. A walkthrough of the app [is here](http://github.com/google/physical-web/blob/master/documentation/android_client_walkthrough.md)

The trickier thing is to get a beacon broadcasting a URL. For most BLE beacons on the market today, this is not very easy to do. We're working on getting a much simpler, maker-friendly device released through an online vendor. This beacon will allow you to set the URL easily through the app and should be available in November 2014.

The simplest way, if you're in a hurry, is to get an [RFDuino](http://www.rfduino.com/) as it is available right now and can be programmed to broadcast a URL.  A good starter kit is [this one](http://www.rfduino.com/product/rfd90101-rfduino-2pc-dev-kit/). Once you have the RFduino installed with its libraries, [this sketch](https://github.com/google/uribeacon/blob/master/beacons/RFduino/physical_web/physical_web.ino) will create a sample beacon that broadcasts "ABC.com".

Once this is up and running, the Physical Web app will be able to see it.

There is also a growing list of other devices that support the broadcasting of URLs in [this directory](https://github.com/google/uribeacon/tree/master/beacons)
