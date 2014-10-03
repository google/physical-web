// Requires bluetooth module on port 'A'
require('tesselate')(['ble-ble113a'], function(tessel, m) {
  var ad = buildAdvertisement('vsco');

  m.ble.setAdvertisingData(ad, function() {
    m.ble.startAdvertising(function() {
      console.log('Now advertising', ad);
    });
  });
});

/**
 * Builds an ad buffer using the domain name with a .com
 * @param  {String} domain
 * @return {Buffer}
 */
function buildAdvertisement(domain) {
  var prefix = new Buffer([
    0x03, // Length
    0x03, // Parameter: Service List
    0xD8, // URI Beacon ID
    0xFE  // URI Beacon ID
  ], 'hex');

  var suffix = new Buffer([
    0x16, // Service Data
    0xD8, // URI Beacon ID
    0xFE, // URI Beacon ID
    0x00, // Flags
    0x20  // Power
  ], 'hex');

  var message = Buffer.concat([
    new Buffer([ 0x00 ], 'hex'), // Shortcut for http://www.
    new Buffer(domain),
    new Buffer([ 0x07 ], 'hex'), // Shortcut for .com
  ]);

  var messageLength = new Buffer([ suffix.length + message.length ]); 

  return Buffer.concat([prefix, messageLength, suffix, message]);
}
