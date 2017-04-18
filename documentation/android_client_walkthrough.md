## Physical Web Android Client Walkthrough
An Introduction to the Physical Web Android Client

We start with a high level-description of the user flows and then dive into and detail the specifics of each screen.

The overall user flow of the app is fairly simple:

* The app listens for nearby beacons.
* The app then shows the user a notification about any found beacons.
* The user can then tap the notification, which brings up a list of the found beacons.
* The user can then tap one of the items in the list, which then opens the web page for that beacon.

But let’s view an example with screenshots. When you first run the app, you'll be greeted with a screen like this, which is a list of nearby found beacons.

![Figure 1](https://raw.githubusercontent.com/google/physical-web/master/documentation/images/android_walkthrough_1.png)

It’s probable that you don't have any beacons configured at the moment, so you'll likely see an empty list in the above screen. So that brings us to the next flow which is configuration. 

Tap the over flow menu button in the top right corner (the vertical ellipsis thing). This brings up the menu. Tap "Edit Urls".

![Figure 2](https://raw.githubusercontent.com/google/physical-web/master/documentation/images/android_walkthrough_2.png)

This opens the configuration screen that will start searching for beacons nearby that are ready to be configured.

![Figure 3](https://raw.githubusercontent.com/google/physical-web/master/documentation/images/android_walkthrough_3.png)

There are lots of different beacons that are being experimented with. Many are broadcast only. If you have one of beacons that Google has passed out, it has a button in the center face. To make it configurable, just press the button and you should hear a beep. If not, that usually means the battery is dead and you should replace it. If all goes well, the app will have found the configurable beacon and tell you so. This can take up to 3 seconds or so.

![Figure 4](https://raw.githubusercontent.com/google/physical-web/master/documentation/images/android_walkthrough_4.png)

Now you can enter a new url into the text field and press either the keyboard "DONE" button or exit the keyboard and tap "SAVE URL" to write that new url to the beacon.  You should hear a confirmation beep from the beacon and also see a toast onscreen telling you that the url was saved to the beacon. You'll then be taken back to the list of nearby beacons that should update shortly with your newly programmed beacon.

We started the first flow with the list of beacons, but really once the app is loaded it runs a background scanner (that turns off when your screen is off). This scanner creates a notification that indicates how many beacons have been found nearby, and hides the notification if that number is zero.

![Figure 5](https://raw.githubusercontent.com/google/physical-web/master/documentation/images/android_walkthrough_5.png)

If you tap the notification, it will bring you back to the list of the nearby beacons, which you can then use to launch the various beacon urls or configure beacons as mentioned above.

So that’s it! Happy Physical Webbing!

Please note that this app has been targeting Android L release (21) and has been tested primarily on the Nexus 5.


## FCC Stuff

### FCC Part 15.19
This device complies with part 15 of the FCC Rules. Operation is subject to The following two conditions: 

1. This device may not cause harmful interference, and 
2. This device must accept any interference received, including interference that may cause undesired operation.

### FCC Part 15.21
Any changes or modifications (including the antennas) made to this device that are not expressly approved by the manufacturer may void the user's authority to operate the equipment.

### FCC RF Radiation Exposure Statement
This transmitter must not be co-location or operating in conjunction with any other antenna or transmitter.

This equipment complies with FCC RF radiation exposure limits set forth for an uncontrolled environment.