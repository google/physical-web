FatBeacon on Raspberry Pi
=========================

This is a nodejs implementation of a Fatbeacon developed on a Raspberry Pi 3.
It will advertise a connectable Fatbeacon over Bluetooth Low Energy (BLE). 
When the central (client) connects to the peripheral (Fatbeacon/server), the
central will attempt to read the webpageCharacteristic, which serves a HTML.
There is also the option to have this data be compressed with gzip.

Instructions for Raspbian:
--------------------------
1. Clone repository onto Pi. If you're using a Pi2 or earlier, make sure you
have a bluetooth chip installed.
2. Make sure you have nodejs

   ```$ node -v```

If not, then run

    ```$ sudo apt install nodejs```

3. For bleno make sure bluetooth, bluez, libbluetooth-dev, and libudev-dev 
   are installed

   ```$ sudo apt-get install bluetooth bluez libbluetooth-dev libudev-dev```

4. Navigate to the repository directory and run

    ```$ sudo npm install```

This should download all required nodejs libraries.
5. To run

    ```$ sudo node FatBeacon.js```
