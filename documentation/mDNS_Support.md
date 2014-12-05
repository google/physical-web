## mDNS Support

The Physical Web is about getting URLs into the physical world. However, this isn't limited to just Bluetooth Low Energy (BLE) beacons. [mDNS](http://en.wikipedia.org/wiki/Multicast_DNS) is a service broadcast technique used in Wifi networks. It has a two advantages over BLE: 

1. Only people logged into your wifi can see the mDNS URLs. This means that in an appartment, your neighbors can't see your devices.
2. It doesn't have the length restrictions of BLE has so a URL can be along as you'd like (well, at least up to 100 characters).

Below is an example of how to setup a Raspberry Pi to broadcast a Physical Web URL using mDNS. We hope others are willing to contribute and offer more versions. If so, we'll create an mDNS directory for all the alternatives.

You'll first need a mDNS service on your RPi. Avahi is the one we use here:
    $ sudo apt-get install avahi-daemon

You'll then place a '.service' file into the /etc/avahi/services directory. Our sample file 'physical-web-url.service' looks like this:

    <?xml version="1.0" standalone='no'?><!--*-nxml-*-->
    <!DOCTYPE service-group SYSTEM "avahi-service.dtd">

    <service-group>
      <name replace-wildcards="yes">http://www.mycompany.com/xyz.html</name>
      <service>
        <host-name>www.mycompany.com</host-name>
        <type>_http._tcp</type>
        <port>80</port>
        <txt-record>path=/xyz.html</txt-record>
      </service>
    </service-group>

Then `<name>` tag must be unique on your network. We are suggesting that you use the URL as your name to be safe. That should be it. If you have the latest client on your phone, the web page http://www.mycompany.com/xyz.html will now show up in your list of nearby devices. Note: the iOS app supports mDNS, but not the Android app yet. It will be updated within a few days for Android 5.0 devices. If people are stuck on older versions, please let us know (or feel free add it yourself ;-)
