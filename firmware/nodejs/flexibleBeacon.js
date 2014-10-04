// use uri-beacon library to encode the URI
// manually create the advertising data

var uriBeacon = require('uri-beacon'),
    bleno = require('bleno');

var template = new Buffer(10); // maximum 31 bytes
template[0] = 0x03; // Length
template[1] = 0x03; // Parameter: Service List
template[2] = 0xD8; // URI Beacon ID
template[3] = 0xFE; // URI Beacon ID
template[4] = 0x00; // Length <-- must be updated
template[5] = 0x16; // Service Data
template[6] = 0xD8; // URI Beacon ID
template[7] = 0xFE; // URI Beacon ID
template[8] = 0x00; // Flags
template[9] = 0x20; // Power

var scanData = new Buffer(0); // maximum 31 bytes
var encoded = uriBeacon.encode("http://example.com");
var advertisementData = Buffer.concat([template, encoded], template.length + encoded.length);
advertisementData[4] = encoded.length + 5;

bleno.startAdvertisingWithEIRData(advertisementData, scanData);
