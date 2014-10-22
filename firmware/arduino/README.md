## Arduino Physical Web Beacon Example

Create a URI Beacon using your [Arduino](http://arduino.cc).

### Supported Hardware (boards/shields)

 * [Nordic Semiconductor nRF8001](http://www.nordicsemi.com/eng/Products/Bluetooth-R-low-energy/nRF8001) based
   * [Adafruit](http://www.adafruit.com)
     * [Bluefruit LE - nRF8001 Breakout](http://www.adafruit.com/products/1697)
   * [RedBearLab](http://redbearlab.com)
     * [BLE Shield](http://redbearlab.com/bleshield/)
     * [Blend Micro](http://redbearlab.com/blendmicro/)
     * [Blend](http://redbearlab.com/blend/)
 * [Nordic Semiconductor nRF51822](http://www.nordicsemi.com/eng/Products/Bluetooth-R-low-energy/nRF51822) based
   * [RedBearLab](http://redbearlab.com)
     * [nRF51822](http://redbearlab.com/redbearlab-nrf51822)
     * [Blend Nano](http://redbearlab.com/blenano)

See [Arduino BLEPeripheral's Compatible Hardware](https://github.com/sandeepmistry/arduino-BLEPeripheral#compatible-hardware) for more info.

[RedBearLab](http://redbearlab.com) products can be purchased from [Seeed Studio](http://www.seeedstudio.com/depot/RedBearLab-m-52.html).

## Using Arduino IDE

#### Setup

Install the [Arduino BLEPeripheral](https://github.com/sandeepmistry/arduino-BLEPeripheral) library. See [Installing Additional Arduino Libraries](http://arduino.cc/en/Guide/Libraries) guide for more info.

##### OS X and Linux
```
cd ~/Documents/Arduino/libraries/
git clone https://github.com/sandeepmistry/arduino-BLEPeripheral BLEPeripheral
```

#### Windows

```
cd "My Documents\Arduino\libraries\"
git clone https://github.com/sandeepmistry/arduino-BLEPeripheral BLEPeripheral
```

#### Running

 1. Open [physical_web.ino](physical_web/physical_web.ino) sketch
 1. Update pin outs in sketch based on hardware setup (```URI_BEACON_REQ```, ```URI_BEACON_RDY```, ```URI_BEACON_RST```)
 1. If needed, update flags, power and URI parameters to ```uriBeacon.begin``` in sketch
 1. Build and upload to board

__Notes__
  * only 15 bytes (instead of 18) are available for the URI on nRF8001 based boards/shields, because GAP flags (3 bytes) are in advertisement data

## Using [codebender](https://codebender.cc)

 1. Open [physical_web_beacon](https://codebender.cc/example/BLEPeripheral/physical_web_beacon) in codebender
 1. Update pin outs in sketch based on hardware setup (```URI_BEACON_REQ```, ```URI_BEACON_RDY```, ```URI_BEACON_RST```)
 1. If needed, update flags, power and URI parameters to ```uriBeacon.begin``` in sketch
 1. Run




