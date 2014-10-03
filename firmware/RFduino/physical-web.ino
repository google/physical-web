# Physical-Web example for RFDigital RFduino

#include <RFduinoBLE.h>

uint8_t advdata[] =
{
  0x03,  // length
  0x03,  // Param: Service List
  0xD8, 0xFE,  // URI Beacon ID
  0x0A,  // length
  0x16,  // Service Data
  0xD8, 0xFE, // URI Beacon ID
  0x00,  // flags
  0x20,  // power
  0x00,  // http://www.
  0x41,  // 'A'
  0x42,  // 'B'
  0x43,  // 'C'
  0x07,  // .".com"
};

void setup() {
  RFduinoBLE_advdata = advdata;
  RFduinoBLE_advdata_len = sizeof(advdata);
  RFduinoBLE.advertisementInterval = 1000; // advertise every 1000ms
  RFduinoBLE.begin();
}

void loop() {
  RFduino_ULPDelay(INFINITE);   // switch to lower power mode
}
