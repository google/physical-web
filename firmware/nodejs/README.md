## NodeJS Physical Web Beacon Example

Requires [NodeJS](http://nodejs.org) and Linux

Install the dependencies

    npm install

Create a beacon

    $ sudo node
    > uriBeacon = require ('uri-beacon')
    > uriBeacon.advertise('http://example.com')

See the examples

 * [simpleBeacon](simpleBeacon.js) - simplest way to create a URI Beacon using [uri-beacon](https://github.com/don/node-uri-beacon)
 * [blenoBeacon](blenoBeacon.js) - manually create a URI Beacon using [bleno](https://github.com/sandeepmistry/bleno)
 * [flexibleBeacon](flexibleBeacon.js) - use uri-beacon to encode URI, manually handle advertising data with bleno

You can only create beacons on Linux. You need to run as sudo. See [bleno](https://github.com/sandeepmistry/bleno#running-on-linux) for more info.

The uriEncoder and tests will run on OS X. You can install on non-Linux platforms with `npm install --force`.

Have an older machine or Raspberry Pi? Add a [Bluetooth 4.0 USB Adapter](http://www.adafruit.com/products/1327).
