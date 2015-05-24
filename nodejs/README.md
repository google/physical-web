## node.js UriBeacon Scanner Example

Requirements

 * [node.js](http://nodejs.org)
 * OS X or Linux
 * Bluetooth 4.0 adapter

### Install the dependencies

```sh
npm install
```

### Run the example

OS X:
```sh
node basic-scanner.js
```

Linux (requies ```sudo```):
```sh
sudo node basic-scanner.js
```

#### Basic Scanner Example

The basic scanner uses [node-uri-beacon-scanner](https://github.com/sandeepmistry/node-uri-beacon-scanner) to start scanning for UriBeacons.

Then prints out the URI, flags, TX power and RSSI for each discovered beacon.

## Other examples

* [Physical Web Scan](https://github.com/dermike/physical-web-scan) - using the PW proxy to display metadata

## More information

 * [node-uri-beacon-scanner Github repo](https://github.com/sandeepmistry/node-uri-beacon-scanner) - source for node.js scanner
 * [UriBeacon Advertising Packet Specification](https://github.com/google/uribeacon/blob/master/specification/AdvertisingMode.md)
