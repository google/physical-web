var UriBeaconScanner = require('uri-beacon-scanner');

UriBeaconScanner.on('discover', function(uriBeacon) {
  console.log('discovered UriBeacon:');
  console.log('  uri      = ' + uriBeacon.uri);
  console.log('  flags    = ' + uriBeacon.flags);
  console.log('  TX power = ' + uriBeacon.txPower);
  console.log('  RSSI     = ' + uriBeacon.rssi);
  console.log();
});

UriBeaconScanner.startScanning();
