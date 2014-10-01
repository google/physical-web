##The Big Idea
The Physical Web extends the  web we know into the physical world around us. This involves creating an open ecosystem where smart devices can broadcast URLs into the area around them. Any nearby display such as a phone or tablet can then see these URLs and offer them up to the user. It mirrors the basic behavior we have today with a search engine:

* The user requests a list of what's nearby
* A ranked list of URLs is shown
* The user picks one
* The URL is opened in a full screen browser window.

![Example interaction](https://raw.githubusercontent.com/google/physical-web/master/documentation/images/example.png)

Even though this is a fairly simple idea, it immediately generates lots of questions:

##1. What's wrong with native apps?
Nothing! Native apps are great but they're just not mathematically practical. If we believe in Moore's Law at all, there will be 1000s of these smart devices in each of our lives and the native app approach just breaks down. Are you really going to download an app when you pass a vending machine? Yahoo used to be a fixed hierarchy of links for the web and finally just had give it up once the web exploded. The same thing will happen with smart hardware and apps.

**When you're experiencing exponential growth, you need a system that grows automatically.** 


##2. Sounds like you'll be pestering people with notifications
A core principle of this system is **no proactive notifications**. The user will only see a list of nearby devices when they ask. If things were to buzz, it would generate immediate frustration. Push notifications, in general are too easily abused. Of course, the user can opt-in to noticiations, we are just saying the default is none as this can easily become overwhelming.

##3. Isn't there going to be a big list to choose from?
At first, the nearby smart devices will be small but if we're successful, there will be many to choose from and that does raise an important UX issue. This is where ranking comes in. Today, we are perfectly happy typing "tennis" into a search engine and getting millions of results back, we trust that the first 10 are the best ones. The same applies here. The phone agent can sort by both signal strength as well as personal preference and history. Clearly there is lots of work to be done here, we don't want to minimize this task but we feel that ranking can get us very far for the first few versions of this project.

##4. Is this secure?
This first version is broadcasting URLs in the clear, there is no encryption so in it's current form, it's not yet secure. That is why we're initially suggesting this to be used in public spaces. However, that being said, there are many ways you could imaging making a URL secure, e.g. a rotating token that requires a login/cookie to decode. One of the huge values of URLs is that they are so flexible and encourage this further evolution.

##5. What about SPAM?
With any system, there will be people that try to exploit it. There will likely be many solutions around this problem but again, we can start with the web. Search engines today have this issue and are fairly effective and displaying the correct web sites in your search results. That same approach would apply here. Combine that with historical results of who clicks on what and it's possible to build a fairly robust ranking model and only show the proper devices. However, there is likely much more we can do here and we hope to encourage other ideas on how to solve this problem in a more robust way.

##6. Why URLs?
The value of a URL is that it is a known part of the web, very flexible, and most importantly, decentralized. URLs allow anyone to play and no central server to be the bottleneck. This is one of the core principles of the web and critical to keep alive.

That being said, we completely expect others to do just that: build a url+ID model that goes through a central server. That is perfectly acceptable and to be encouraged. Systems like that are likely to provide much better security and vetting of sites. But that is the beauty of URLs, there can be as many of these as you'd like and the system still works seamlessly.  

##7. Which platforms will you support?
This is meant to be an extension of the web so it should work on every platform. We expect that each platform will experiment with a different UX to show the nearby devices. For example, Android might use a widget or even integrate with Google Now. iOS could use their notification manager (silently!) or even just have an app that you can open. We hope to see lots of experimentation here on how various platforms choose to show and rank this information.

At this point, we have an android application and an AppEngine Server app that is open source. We hope this will be used and ported to other platforms.

##8. Can't the user be tracked?
Our current URL broadcast method involves a bluetooth broadcast from each device. The user's phone gathers this info without contacting the device so the user is invisible to each one. This means a user can't be tracked simply by walking past a broadcasting device. This was very much by design to keep users silent passage untrackable. However, once the user does click on a URL, they are then known to that website. 

The search agent on the phone may keep track of which devices the user taps on so they can improve the ranking in the future. Of course, this too needs to be discussed and the possibly offered to the user as an option so they are in control of how this information is stored.

##9. Why Bluetooth Low Energy?
There are many possible ways to broadcast a URL. This initial version uses Bluetooth Low Energy (BLE) as it is so ubiquitous on mobile phones/tablets today. This should not be the only wireless solution but it is the easiest to use at the moment so we can experiment and prototype this system.

##Next
The next document to read would be the technical [overview](http://github.com/scottjenson/physical-web/blob/master/technical_overview.md) document
