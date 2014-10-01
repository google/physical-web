# Getting started testing the Physical Web

In order to get up and running you need two things:

1. A hardware beacon
2. Software on your phone/tablet to see that beacon.

The software is the easiest thing to take care of. The source code is located in this repo. However, you can get the latest build of the android APK  [here.](https://drive.google.com/a/google.com/folderview?id=0BzdAbb5PggFeWXE1S3dJcTRBZk0&usp=sharing&tid=0BzdAbb5PggFeWG5ZOThWUlRsYmM#list) Just make sure you go to Settings>Security>Unknown Sources and flip the checkbox on before you do. 

The trickier thing is to get a beacon broadcasting a URL. For most BLE beacons on the market today, this is not very easy to do. We're working on getting a much simpler, maker-friendly device released through an online vendor. This should be ready in November 2014.

The simplest way, at the moment, is to get an [RFDuino](http://www.rfduino.com/) as it is very easy to program and is also available right now.  Once you have the RFduino installed with it's libraries, this  sketch will create a sample beacon, that broadcasts "ABC.com":

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

Once this is up and running, the Physical Web android app will be able to see it.

If there are other maker-ish devices out there that can setup easily, please let us know so we can add them to this list.
