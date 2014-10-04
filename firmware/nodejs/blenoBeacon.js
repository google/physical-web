// Manually create a URI Beacon with bleno

var bleno = require('bleno');

var scanData = new Buffer(0); // maximum 31 bytes
var advertisementData = new Buffer(15); // maximum 31 bytes

advertisementData[0] = 0x03; // Length
advertisementData[1] = 0x03; // Parameter: Service List
advertisementData[2] = 0xD8; // URI Beacon ID
advertisementData[3] = 0xFE; // URI Beacon ID
advertisementData[4] = 0x0A; // Length
advertisementData[5] = 0x16; // Service Data
advertisementData[6] = 0xD8; // URI Beacon ID
advertisementData[7] = 0xFE; // URI Beacon ID
advertisementData[8] = 0x00; // Flags
advertisementData[9] = 0x20; // Power
advertisementData[10] = 0x00; // http://www.
advertisementData[11] = 0x61; // a
advertisementData[12] = 0x62; // b
advertisementData[13] = 0x63; // c
advertisementData[14] = 0x07; // .com

bleno.startAdvertisingWithEIRData(advertisementData, scanData);
