##Technical Overview
This is a prototype system being used to understand and explore the issues involved in building the Physical Web.

###Broadcast
The current design uses Bluetooth Low Energy (BLE) devices that broadcast the URL in the advertising packet. In this broadcast mode, it doesn't do anything else such as respond to requests. The sole job of the device is to broadcast the URL to the surrounding area.

The reason  is to accommodate potentially the worst case scenario of a large numbers of devices in an area with a large amount of people.  A broadcast only approach avoids an n-squared problem of every user connecting to every device. By making each device constantly broadcast, any number of devices can just pick up the information passively with little conflict.

This has another advantage in that it means that a user can walk through a space and leave no trace: the broadcasting devices have no idea who is listening.

The current prototype broadcasts once every second, striking a balance between user response time and battery life. There is nothing stopping a device from broadcasting faster if they wish.

###BLE Format
The URL is stored in the advertising packet of a BLE device. The packet identifies itself with a 16 bit Service UUID of 0xFED8 which indicates the beacon is a "URI Device". The exact layout of the packet is:

![Ad Packet layout](https://raw.githubusercontent.com/google/physical-web/master/documentation/images/uribeacon1.png)

The specific fields for this packet are as follows:

| FieldName      | Offset   | Size   | Format      | Description                                                     |
|----------------|----------|--------|-------------|-----------------------------------------------------------------|
| **AD Length**  | 0        | 1      | 8-bit value | 5..23                                                           |
| **AD Type**    | 1        | 1      | 8-bit value | Service Data = 0x16                                             |
| **Service ID** | 2        | 2      | 16-bit UUID | URI = 0xFED8                                                    |
| **Flags**      | 4        | 1      | 8-bit value | See spec                                                        |
| **TX Power**   | 5        | 1      | 8-bit value | See spec                                                        |
| **URI field**  | 6        | 1..18  | octets      | The US-ASCII text of the URI with embedded Text Expansion Codes |

This does not, however, leave a lot of room for the text of the URL. This is one of tradeoffs that comes from avoiding any connections to the beacon (to ensure no user tracking can occur) URLs are encoded so common patterns like 'http://www.' and '.com' can be compressed into a single character. This is very similar to what QRCodes use to encode their URLs. In addition, we expect initial testers will either use short domains or a URL shortener. Both the android and iOS apps do this automatically when a URL is typed in that is too long to fit.

A GATT service that would allow the URL to be of any arbitrary length, is under consideration. This will be posted shortly for further community discussion.

This is just a quick description to show the structure of the ad packet. It is documented in a related GitHub project, the URIBecon specification. This repo will go live shortly.

###Client
The current client is an application to prove out the technology. If you open the app, it lists the nearby beacons that it can see, sorted by signal strength. Note the signal strength is a very iffy metric as it there many reasons why it can vary. However, if you are standing in front of device and the next device is >5 feet away, it tends to work out well in practice. This is the primary reason we include a TX_POWER byte in the ad packet so it's possible to calculate signal loss and rank different strength beacons.

The client lists the meta information from the URL: TITLE, DESCRIPTION, URL, and FAVICON. These could be pulled at run time but there is a simple proxy server that acts as a cache to speed up this process.

###Server
The server receives a request from the client with all of the URLs found and returns a JSON data structure listing all of the meta information listed about. The current prototype collects no user data, only returning the casched info. However alternative implementations could keep track of user choice and use that to help change the ranking within the client. The server isn't required but greatly simplifies the work on the client side.

###Meta Data
In order to use URLs, the system expects those URLs to point to a web page, which offers up the meta information described above. This somewhat limits the URLs as they much then always point to a valid HTML page. This is likely a big limitation to URLs that want to link to native applications. This is an imporant use case and needs to be discussed. Is there an alternative way to offer this meta data but not point to a web page?

###Ranking
As more devices are found, the importance of ranking the devices becomes more valuable. Sorting only be signal strength is a good start but the server could do a much better job in two ways. The first would be to track which URLs are clicked as that implies value, so higher used URLs could be ranked higher. In addition, the server could track personal use so if you tend to pick the same device at work, it would be sure to rank the device higher as well.

### Security
At this point, the URL is broadcast as plain text so it is not really viable for personal use (as neighbors could know what devices you have in your home) However, the advantage of URLs is that there several potential solutions that would help this. There is nothing stopping a beacon from delivering an obfuscated URL or even a URL that requires login in order to see the information.
