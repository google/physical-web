var bleno = require('bleno');
var eddystoneBeacon = require('eddystone-beacon');
var webpageCharacteristic = require('./webpageCharacteristic');

var SERVICE_UUID = 'ae5946d4-e587-4ba8-b6a5-a97cca6affd3';

/***********************Altering Library Functions**************************/
/**
 * In order to broadcast in the fatbeacon format, we override several
 * functions in the advertisement-data module of the eddystone-beacon library.
 */
var AdvertisementData =
        require('eddystone-beacon/lib/util/advertisement-data');
var Eir = require('eddystone-beacon/lib/util/eir');

var FAT_BEACON_FRAME_TYPE = 0x0e;
var MAX_URL_LENGTH = 18;
var ADVERTISING_HEADER_UUID = 'feaa';

/**
 * this patch ensures that the correct Fatbeacon eir flag (0x06) is added
 * to the packet instead of the library standard flag.
 */
AdvertisementData.makeEirData = function(serviceData) {
  var eir = new Eir();
  eir.addFlags(0x06);
  eir.add16BitCompleteServiceList([ADVERTISING_HEADER_UUID]);
  eir.addServiceData(ADVERTISING_HEADER_UUID, serviceData);
  return eir.buffer();
}

/**
 * This patch alters the method signature to accept a name instead of a url.
 * It also encodes the name in hex instead of the libraries url encoding
 * format. Lastly, it adds the Fatbeacon header to the packet.
 */
AdvertisementData.makeUrlBuffer = function(name) {
  console.log(`AdvertisementData.makeUrlBuffer called with: ${name}`);

  var encodedName = Buffer.from(name);
  if (encodedName.length > MAX_URL_LENGTH) {
    throw new Error(`Encoded Name must be less than ${MAX_URL_LENGTH} bytes.` +
                    ` It is currently ${encodedName.length} bytes.`);
  }

  var serviceData = Buffer.concat([
    Buffer.from([0x10, 0xba, FAT_BEACON_FRAME_TYPE]), //FatBeacon Header
    encodedName
  ]);

  return AdvertisementData.makeEirData(serviceData);
}

/*********************************************************/

var characteristic = new webpageCharacteristic();
var data = new Buffer.from('Hello World of FatBeacon!');
characteristic.onWriteRequest(data, 0, null, null);

var service = new bleno.PrimaryService({
  uuid: SERVICE_UUID,
  characteristics: [
    characteristic
  ]
});

bleno.once('advertisingStart', function(err) {
  
  if(err) {
    throw err;
  }

  console.log('on - advertisingStart');
  bleno.setServices([
    service
  ]);
});

// Start Advertising name.
eddystoneBeacon.advertiseUrl('New Fatbeacon');
