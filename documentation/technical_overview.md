## Technical Overview
This is a prototype system being used to understand and explore the issues involved in building the Physical Web.

### Broadcast
The current design uses Bluetooth Low Energy (BLE) devices that broadcast the URL in the advertising packet. The sole job of the device is to broadcast the URL to the surrounding area. The reason is to accommodate potentially the worst-case scenario of a large number of devices in an area with many people.  A broadcast only approach avoids an N-squared problem of every user connecting to every device. By making each device broadcast constantly, any number of devices can just pick up the information passively with little conflict.

This has another advantage in that it means that a user can walk through a space and leave no trace: the broadcasting devices have no idea who is listening.

The current prototype broadcasts once every second, striking a balance between user response time and battery life. There is nothing stopping a device from broadcasting faster if it wishes.

### BLE Format
The URL is stored in the advertising packet of a BLE device. The packet identifies itself with a 16-bit Service UUID of 0xFEAA which indicates the beacon is an [Eddystone beacon](https://github.com/google/eddystone). Additionally, the packet sets the frame type field to 0x10 to specify that it is of the [Eddystone-URL](https://github.com/google/eddystone/tree/master/eddystone-url) format, which is designed to carry URLs.

This small size of the advertising packet does not leave a lot of room for the text of the URL. This is one of tradeoffs that comes from avoiding any connections to the beacon (to reduce tracking and avoid congestion). URLs are encoded so common patterns like 'http://www.' and '.com' can be compressed into a single character. This is very similar to NDEF compression in NFC. In addition, we expect initial testers will use either short domains or a URL shortener. Both the Android and iOS apps do this automatically when a URL is typed in that is too long to fit.

### Client
The current client is an application, rather than a system service or integrated part of the operating system, to prove out the technology. If you open the app, it lists the nearby beacons that it can see, sorted by signal strength. Note the signal strength is a very iffy metric as there many reasons why it can vary. However, if you are standing in front of device and the next device is more than five feet away, it tends to work out well in practice. This is the primary reason we include a TX_POWER byte in the advertising packet so it's possible to calculate signal loss and rank different strength beacons.

The client lists the meta information from the URL: TITLE, DESCRIPTION, URL, and FAVICON. These can be pulled at the time the URL is received. Alternately, there is a simple caching proxy server that speeds up this process.

### Server
The server receives a request from the client with all the URLs found and returns JSON listing the URLs' metadata. The current prototype collects no user data, only returning the cached information. However, alternative implementations could keep track of user choice and use that to help change the ranking within the client. The server isn't required as part of Physical Web, but it greatly simplifies the work on the client side and improves responsiveness and quality of results.

### Metadata
The system expects URLs to point to a web page, which offers up the metadata described above. This somewhat limits the URLs, as they must always point to a valid HTML page. This is likely a significant limitation to URLs that want to link to native applications. This is an important use case needing further discussion. Is there an alternative way to offer this metadata but not point to a web page?

### Ranking
As more devices are found, the importance of ranking the devices becomes more valuable. Sorting only by signal strength is a good start, but the server could do a much better job in two ways. The first would be to track which URLs are clicked as that implies value, so more frequently used URLs could be ranked higher. In addition, the server could track personal use, so if you tended to pick the same device at work, it might rank the device higher as well.

### Security
At this point, the URL is broadcast as plain text, so we must be careful recommending this for personal use (as neighbors could know what devices you have in your home). However, there are many possible solutions:

* Turn down the transmit power so the range is quite small
* Use obfuscated URLs
* Web authentication mechanisms, such as cookies
* A rotating key at the end of a dynamic URL
* Use mDNS on a wifi network

The advantage of URLs is that there are several standard, well-understood solutions. We expect discussion on this topic to continue and more robust solutions to be proposed.
