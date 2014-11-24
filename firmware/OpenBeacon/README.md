## OpenBeacon Physical Web Beacon

To demonstrate usage of the included 3D acceleration sensor the included hello world example transmits the URL only while the beacon is facing forward.

Get the nRF51822 based [hardware design](http://get.openbeacon.org/device.html) and [development environment](http://get.openbeacon.org/source/) for creating your own beacons.

You can find the main loop in [firmware/nRF51/tag-physical-web/entry.c](https://github.com/meriac/openbeacon-ng/blob/master/firmware/nRF51/tag-physical-web/entry.c).

### Quick start
For compiling and flashing the firmware source you need to get the [source code](https://github.com/meriac/openbeacon-ng):
```bash
# get the latest source code
git clone https://github.com/meriac/openbeacon-ng
# change into tag directory
cd openbeacon-ng/firmware/nRF51/tag-physical-web
# compile and flash code
make clean flash
```
Software development is officially supported for OS X and Linux. Please use the latest [gcc-arm-none-eabi](https://launchpad.net/gcc-arm-embedded) toolchain from launchpad. Thanks a lot to ARM for maintaining [GNU Tools for ARM Embedded Processor](https://launchpad.net/gcc-arm-embedded).

As you can see the makefile directly supports flashing from command line (under OSX and Linux) by using a [Segger J-Link debug probe](https://www.segger.com/jlink-software.html) from the [Nordic nRF51822 development kit](http://uk.mouser.com/Search/Refine.aspx?Keyword=949-NRF51822-DK).


### Support ###

Please sign up to our [OpenBeacon Group](https://groups.google.com/forum/#!forum/openbeacon) or contact us at [developer@openbeacon.org](mailto:developer@openbeacon.org?subject=Developer%20Support).

