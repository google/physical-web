## mDNS Support

The Physical Web is about getting URLs into the physical world. However, this isn't limited to just Bluetooth Low Energy (BLE) beacons. [mDNS](http://en.wikipedia.org/wiki/Multicast_DNS) is a service broadcast technique used in Wi-Fi networks. It has two advantages over BLE: 

1. Only people logged into your Wi-Fi can see the mDNS URLs. This means that in an apartment, your neighbors can't see your devices.
2. It doesn't have the length restrictions of BLE, so a URL can be as long as you'd like (well, at least up to 100 characters).

Below is an example of how to setup a Raspberry Pi to broadcast a Physical Web URL using mDNS. We hope others are willing to contribute and offer more versions. If so, we'll create an mDNS directory for all the alternatives.

Using any zeroconf networking implementation that supports mDNS service registration, register a service as follows: (below our examples are using the dns-sd tool on Mac)

If you want to broadcast a public url:

```shell
dns-sd -R "example_name" _physicalweb._tcp local 80 url="www.example_url.com"
```

"example_name" should be replaced by any name unique to your network. Use `dns-sd -B _physicalweb._tcp local` to find all registered names. Replace www.example_url.com with the url you wish to broadcast.

If you want to broadcast a private url:

```shell
dns-sd -R "example_name" _physicalweb._tcp local 80 url="www.example_local_url.com" title="Example Title" desc="Example Description"
```

Replace the Url, Title, and Description with your local page's metadata.

That should be it. If you have the latest client on your phone, the web page you wish to broadcast will now show up in your list of nearby devices.

Note: if the URL is global, it shows up just like a BLE beacon with Title, Description, and favicon information. If the URL is local, the meta data isn't supported yet, only the URL and given metadata will be displayed.