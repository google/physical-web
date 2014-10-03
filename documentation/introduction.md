##The Big Idea
The Physical Web extends the web we know into the physical world around us. This involves creating an open ecosystem where smart devices can broadcast URLs into the area around them. Any nearby display such as a phone or tablet can then see these URLs and offer them up to the user. It mirrors the basic behavior we have today with a search engine:

* The user requests a list of what's nearby.
* A ranked list of URLs is shown.
* The user picks one.
* The URL is opened in a full screen browser window.

![Example interaction](https://raw.githubusercontent.com/google/physical-web/master/documentation/images/example.png)

Even though this is a fairly simple idea, it immediately generates lots of questions:

##0. Wait, why are you an app?
This is an early prototype. We are trying get people to experiment with this at an early stage. Of course, this should be built into the operating system of all smartphones (and tablets and anything with a screen really). We are building an app for now that tries to not feel like an app. It works in the background so you don't need to use it actively. It just silently monitors beacons that you can browse when you're interested.

##1. Will you be pestering people with alarms?
A core principle of this system is **no proactive notifications**. The user will only see a list of nearby devices when they ask. If your phone were to be buzzing constantly as you walked through the mall, it would be very frustrating. Push notifications in general are too easily abused. Of course, the user can opt-in to notifications, we are just saying that by default, the user must ask to see anything nearby.

##2. Isn't there going to be a big list to choose from?
At first, the nearby smart devices will be small but if we're successful, there will be many to choose from and that raises an important UX issue. This is where ranking comes in. Today, we are perfectly happy typing "tennis" into a search engine and getting millions of results back, we trust that the first 10 are the best ones. The same applies here. The phone agent can sort by both signal strength as well as personal preference and history, among many other possible factors. Clearly there is lots of work to be done here. We don't want to minimize this task, but we feel that this simple signal strength ranking can get us very far for the first few versions of this project.

##3. Is this secure/private?
URLs broadcast in the clear so anyone can see them. This is by design. That is why we're initially suggesting this to be used in public spaces. This does raise issues for home use where it would be possible for neighbors to intercept beacons. However, one of the big advantages of URLs is that there are so many ways they can be used to increase their security:

* The URL could be obfuscated (e.g. using a non-branded domain)
* The web page could require a login
* A rotating token on the beacon would constantly change the URL
* The URL could reference an IP address that is only accessible when connected to a local network

One of the big values of URLs is that they are so flexible and encourage this further evolution.

##4. What about SPAM?
With any system, there will be people that try to exploit it. There will likely be many solutions around this problem but again, we can start with the web. Search engines today have this issue and are fairly effective and displaying the correct web sites in your search results. That same approach would apply here. Combine that with historical results of who clicks on what and it's possible to build a fairly robust ranking model and only show the proper devices. However, there is likely much more we can do here and we hope to encourage other ideas on how to solve this problem in a more robust way.

##5. Why URLs?
The value of a URL is that it is a known part of the web, very flexible, and most importantly, decentralized. URLs allow anyone to play and no central server to be the bottleneck. This is one of the core principles of the web and critical to keep alive.

That being said, we completely expect others to experiment with a url+ID model that goes through their server (e.g. safeurls.com/?id=12345). That is perfectly acceptable and to be encouraged. Systems like that are likely to provide much better security and vetting of sites. But that is the beauty of URLs, there can be as many of these as you'd like and the system still works seamlessly.

##6. Which platforms will you support?
This is meant to be an extension of the web so it should work on every platform. We expect that each platform will experiment with a different UX to show the nearby devices. For example, the current Android app uses notifications while the iOS app will use lock screen notifications. We hope to see lots of experimentation here on how various platforms choose to show and rank this information.

At this point, we have both an Android and iOS app that is open source. We hope this will be used and ported to other platforms.

##7. Can't the user be tracked?
Our current URL broadcast method involves a bluetooth broadcast from each beacon. The user's phone gathers this info without connecting to the beacon. This ensures the user is invisible to all beacons, meaning a user can't be tracked simply by walking past a broadcasting beacon. This was very much by design to keep users silent passage untrackable. However, once the user does click on a URL, they are then known to that website.

The search agent on the phone may keep track of which devices the user taps on so they can improve the ranking in the future. Of course, this too needs to be discussed and then possibly offered to the user as an option so they are in control of how this information is stored.

##8. Why Bluetooth Low Energy?
There are many possible ways to broadcast a URL. This initial version uses Bluetooth Low Energy (BLE) as it is so ubiquitous on mobile phones and tablets today. This should not be the only wireless solution but it is the easiest to use at the moment so we can experiment and prototype this system.

##Next
The next suggested document to read would be the technical [overview](http://github.com/google/physical-web/blob/master/documentation/technical_overview.md) document
