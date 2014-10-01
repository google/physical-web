##Physical Web Android Client Walkthrough
An Introduction to the Physical Web Android Client

We start with a high level-description of the user flows and then dive into and detail the specifics of each screen.

The overall user flow of the app is fairly simple:
*The app listens for nearby beacons.
*The app then shows the user a notification about any found beacons.
*The user can then tap the notification, which brings up a list of the found beacons.
*The user can then tap one of the items in the list, which then opens the web page for that beacon.

But let’s view an example with screenshots. When you first run the app, you'll be greeted with a screen like this, which is a list of nearby found beacons.

(https://raw.githubusercontent.com/google/physical-web/master/documentation/images/android_walkthrough_1.png)

It’s probable that you don't have any beacons configured at the moment, so you'll likely see an empty list in the above screen. So that brings us to the next flow which is configuration. 

Tap the over flow menu button in the top right corner (the vertical ellipsis thing). This brings up the menu. Tap "Edit Urls".

(https://raw.githubusercontent.com/google/physical-web/master/documentation/images/android_walkthrough_2.png)

This opens the configuration screen that will start searching for beacons nearby that are ready to be configured.

(https://raw.githubusercontent.com/google/physical-web/master/documentation/images/android_walkthrough_3.png)

To make a beacon configurable, press its button. It should beep. If all goes well, the app will have found the configurable beacon and tell you so. Sometimes this can be a little slow (but any longer than 3 seconds or so, just press the back button and then re-tap the "Edit Urls" menu button).

(https://raw.githubusercontent.com/google/physical-web/master/documentation/images/android_walkthrough_4.png)

Now you can enter a new url into the text field and press either the keyboard "DONE" button or exit the keyboard and tap "SAVE URL" to write that new url to the beacon.  You should hear a confirmation beep from the beacon and also see a toast onscreen telling you that the url was saved to the beacon. You'll then be taken back to the list of nearby beacons that should update shortly with your newly programmed beacon.

We started the first flow with the list of beacons, but really once the app is loaded it runs a background scanner (that turns off when your screen is off). This scanner creates a notification that indicates how many beacons have been found nearby, and hides the notification if that number is zero.

(https://raw.githubusercontent.com/google/physical-web/master/documentation/images/android_walkthrough_5.png)

If you tap the notification, it will bring you back to the list of the nearby beacons, which you can then use to launch the various beacon urls or configure beacons as mentioned above.

So that’s it! Happy Physical Webbing!

Please note that this app has been targeting Android L release (21) and has been tested primarily on the Nexus 5.
