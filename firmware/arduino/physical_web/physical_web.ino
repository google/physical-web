// Import libraries (BLEPeripheral depends on SPI)
#include <SPI.h>
#include <BLEPeripheral.h>
#include <URIBeacon.h>

// define pins (varies per shield/board)
//
//   Adafruit Bluefruit LE   10, 2, 9
//   Blend                    9, 8, UNUSED
//   Blend Micro              6, 7, 4
//   RBL BLE Shield           9, 8, UNUSED

#define URI_BEACON_REQ   10
#define URI_BEACON_RDY   2
#define URI_BEACON_RST   9

URIBeacon uriBeacon(URI_BEACON_REQ, URI_BEACON_RDY, URI_BEACON_RST);

void setup() {
  Serial.begin(9600);

#if defined (__AVR_ATmega32U4__)
  //Wait until the serial port is available (useful only for the Leonardo)
  //As the Leonardo board is not reseted every time you open the Serial Monitor
  while(!Serial) {
  }
  delay(5000);  //5 seconds delay for enabling to see the start up comments on the serial board
#endif

  uriBeacon.begin(0x00, 0x20, "http://example.com"); // flags, power, URI

  Serial.println(F("URI Beacon"));
}

void loop() {
  uriBeacon.loop();
}
