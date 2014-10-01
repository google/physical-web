##Technical Overview
This is a prototype system being used to understand and explore the issues involved in building the Physical Web.

###Broadcast
The current design uses Bluetooth LE (BLE) devices that broadcast the URL in the advertising packet. In this broadcast mode, it doesn't do anything else such as respond to requests. The sole job of the device is to broadcast the URL to the surrounding area. 

The reason  is to accommodate potentially the worst case scenario of a large numbers of devices in an area with a large amount of people.  A broadcast only approach avoids an n-squared problem of every user connecting to every device. By making each device constantly broadcast, any number of devices and just pick up the information passively with little conflict.

This has another advantage in that it means that a user can walk through a space and 'leave no trace': the broadcasting devices have no idea who is listening. 

The broadcast is currently done by using the BLE NAME parameter. This is for demo purposes only. There is a proposal to the BLE standards body to have a URL parameter type and once that is agreed to, we'll switch over to that. The current limitation to the URL length is 28 bytes which is unfortunately a bit too short. Switching over to the URL parameter will allow for a small amount of compression but should only allow for URLs to be a bit longer, potentially up to 36 bytes. This is hopefully a temporary issue as the next BLE spec will allow advertising packets to be much longer, up to 100+ bytes which should significantly reduce this constraint.

The current prototype broadcasts every 1 second. This is a somewhat arbitrary choice, chosen to allow users to get a complete list without having to wait too long. This should accommodate up to 100 devices with in a typical receive radius. However, this timing should be discussed further.

###BLE Format
The URL is stored in the advertising packet of a BLE device. The packet identifies itself with a 16 bit Service UUID of 0xFED8 which indicates the beacon is a "URI Device". The exact layout of the packet is:

![Ad Packet layout](https://raw.githubusercontent.com/google/physical-web/master/documentation/images/uribeacon1.png)

The specific fields for this packet are as follows:

| *Name*       | *Offset* | *Size* | *Format*    | *Description*                                                   |
|--------------|----------|--------|-------------|-----------------------------------------------------------------|
| *AD Length*  | 0        | 1      | 8-bit value | 5..23                                                           |
| *AD Type*    | 1        | 1      | 8-bit value | Service Data = 0x16                                             |
| *Service ID* | 2        | 2      | 16-bit UUID | URI = 0xFED8                                                    |
| *Flags*      | 4        | 1      | 8-bit value | See spec                                                        |
| *TX Power*   | 5        | 1      | 8-bit value | See spec                                                        |
| *URI field*  | 6        | 1..18   | octets      | The US-ASCII text of the URI with embedded Text Expansion Codes |

For most devices to find This does not, however, leave a lot of room for the text of the URL. URLs are encoded so common patterns like 
'htttp://www.' can be compressed into a single character. This is very similar to what QRCodes use to encode their URLs. In addition, we expect initial testers will either use short domains or a URL shortener. Both the android and iOS apps do this automatically when a URL is typed in that is too long to fit.

This is just a high level description to show the structure of the ad packet. It is documented in a related GitHub project, the URIBecon specification. This repo will go live shortly.

###Client
The current client is an application to prove out the technology. If you open the app, it lists the nearby beacons that it can see, sorted by signal strength. Note the signal strength is a very iffy metric as it there many reasons why it can vary. However, if you are standing in front of device and the next device is >10 feet away, it tends to work out well in practice. We are currently using BLE boards that all broadcast at exactly the same signal strength. An obvious improvement would be to send the XMT_POWER in the ad packet (a defined BLE spec parameter) so the client could to better sorting by signal strength. 

There are lots of different UX directions to go beyond a simple application and we hope to explore alternatives here. We're keeping with the application for now as it simpler and faster to prototype. The obvious alternatives would be to use silent notifications or a widget.

The client lists the meta information from the URL: TITLE, DESCRIPTION, URL, and FAVICON. These could be pulled at run time but there is a simple server that acts as a cache to speed up this process.

###Server
The server receives a request from the client with all of the URLs found and returns a JSON data structure listing all of the meta information listed about. The current prototype collects no user data, only returning the casched info. However alternative implementations could keep track of user choice and use that to help change the ranking within the client.

Just to be clear, the Client/Server combination will likely for a single service. If someone wants to try a different client, they will most likely want to try a different server.

###Meta Data
In order to use URLs, the system expects those URLs to point to a web page, which offers up the meta information described above. This somewhat limits the URLs as they much then always point to a valid HTML page. This is likely a big limitation to URLs that want to link to native applications. This is an imporant use case and needs to be discussed. Is there an alternative way to offer this meta data but not point to a web page?

###Ranking
As more devices are found, the importance of ranking the devices becomes more valuable. Sorting only be signal strength is a good start but the server could do a much better job in two ways. The first would be to track which URLs are clicked as that implies value, so higher used URLs could be ranked higher. In addition, the server could track personal use so if you tend to pick the same device at work, it would be sure to rank the device higher as well. 

### Security
At this point, the URL is broadcast as plain text so it is not really viable for personal use (as neighbors could know what devices you have in your home) There is nothing stopping a URL service from delivering obfuscated URLs or even a URL that requires login in order to see the information.
