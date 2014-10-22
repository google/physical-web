##Physical Web Android Client Walkthrough
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


##FCC Stuff
###FCC Part 15.105(b) Warning Statement
This equipment has been tested and found to comply with the limits for a Class B digital device, pursuant 
to part 15 of the FCC Rules. These limits are designed to provide reasonable protection against harmful 
interference in a residential installation. This equipment generates, uses and can radiate radio frequcny 
energy and, if not installed and used in accordance with the instructions, may cause harful interference to 
radio communications. However, there is no guarantee that interference will not occur in a particular 
installation. If this equipment does cause harmful interference to radio or television reception, which can 
be determined by running the equipment off and on, the user is encouraged to try to correct the 
interference by one or more of the following measures:

1. Reorient or relocate the receiving antenna
2. Increase the separation between the equipment and receiver
3. Connect the equipment into an outlet on a circuit different from that to which the receiver is 
connected
4. Consult the dealer or an experience radio/TV technician for help
