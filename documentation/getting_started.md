# Seeing the Physical Web for yourself

In order to get up and running you need two things:

1. A hardware beacon
2. Software on your phone/tablet to see that beacon.

The software is the easiest thing to take care of as there is an android app (and source) here in this repo. We also have an iOS app but it's not yet on the appstore. Here is the link to the [android app](https://github.com/scottjenson/physical-web/tree/master/android/PhysicalWeb/build/apk)

The trickier thing is to get a beacon broadcasting your URL. For most BLE beacons, this is not very easy to do. We're working on a much simple maker-friendly device. We're working on getting it offered through an online vendor. 

The simplest way, at the moment, is to get an [RFDuino](http://www.rfduino.com/) as it is very easy to program. However, it's not pretty or very small, but it's pretty easy to get started with it.

This Arduino sketch will create a sample beacon, that broadcasts 
"ABC.com":

    void setup() {
      RFduinoBLE.deviceName = "cnn.com";
      RFduinoBLE.begin();
    }

Once this is up and running, the Physical Web android app will be able to see it.

If there are other devices out there that can setup easily, please let us know so we can add them to this list.
