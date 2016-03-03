# Physical Web Service

This is an App Engine project which implements a sample Physical Web metadata
service.  The use of a Physical Web Service is not necessary, but remarkably
useful.  It helps clients resolve URLs in a safer and more efficient manner.  
Read below for more details.

This particular implementation is made to work with the sample Physical Web
applications in this repo.


## Why do we use a Physical Web Service?

[Eddystone-URL](https://github.com/google/eddystone/tree/master/eddystone-url) beacons have an open but very specific protocol which must be
followed.  Anyone is free to do so.  This is the only requirement for
participation and interaction with the Physical Web.

Any app/device can read these Eddystone-URL packets and do with them whatever they
wish.  We have provided some example apps (and published them to app stores) to
make it easier for you to try out the Physical Web.

Our app of course wants to provide a beautiful user experience, with rich 
information about the URLs it finds. Even more so, it wants to protect the 
user and his/her privacy from the many potential misuses of the physical web.  
We hope every other app will do so, also.

One really nice property of the Physical Web is that Eddystone-URL beacons cannot
physically detect when clients scan them and so cannot track passers-by.
However, the web servers backing these URLs can track all requests to them 
(as is usual on the web).  So our app must be extremely careful with how it 
uses the URLs it finds.

Yet, showing raw URLs alone is not descriptive enough for most users, and is
basically useless to anyone when those URLs are obfuscated strings coming from 
a URL shortener (which is common due to the URL length limit).

We would like to grab a nice Title / Icon from the actual content of the page.  
But that requires making a request for the page, which may potentially be
abused by malicious parties.  This is the crux of the issue, which we have
solved by going through a trusted intermediary.  We call this a Physical Web
Service.


## What does a Physical Web Service do?

At the very simplest, it fetches, parses, and presents the content of Eddystone-URL packets on behalf of a client, but without using the client's identity in any way.
It's a middleman added for safety and efficiency.

Unlike an Eddystone-URL beacon, a Physical Web Service is not a core part of the Physical Web, is not mandatory, and does not have a specific protocol that must be
followed (though perhaps an ad-hoc format will arise one day).  It is an
auxiliary solution to solve a fundamental problem for Physical Web client
software (see above).

Over time, we've found this to be an elegant way to solve a bunch of other
problems for us as well by making the Physical Web:
* Faster, because we offload the heavy task of content parsing to the server.
* Cheaper, because we offload the network request costs to the server.
* Safer, because we can introduce safe-search filtering of inappropriate
  content.
* Better, because we can rank results based on various metrics, much as a
  search engine does.


## How to run your own Physical Web Service

You can run your own Service by taking a look at the
[source code](https://github.com/google/physical-web/tree/master/web-service)
and following typical
[App Engine deployment documents](https://cloud.google.com/appengine/docs/python/gettingstartedpython27/uploading),
or try it locally first by using a
[development server](https://cloud.google.com/appengine/docs/python/tools/devserver).
Eventually, you will want to update the `app.yaml` to create a new application
ID, and take a look at `config.SAMPLE.json`.

If you are building one of our sample apps and would like to use your own
Physical Web Service, you can change the endpoint by modifying:

* Android: Update `PROD_URL` and `DEV_URL` in
  [`PwsClient.java`](https://github.com/google/physical-web/blob/7c7b5c00f2e6eb08c9730be36a98954334a2e8c6/android/PhysicalWeb/app/src/main/java/org/physical_web/physicalweb/MetadataResolver.java#L48)
  and `proxy_go_link_base_url` in
  [`strings.xml`](https://github.com/google/physical-web/blob/7c7b5c00f2e6eb08c9730be36a98954334a2e8c6/android/PhysicalWeb/app/src/main/res/values/strings.xml#L42)
* iOS: Update `PHYSICALWEB_SERVER_HOSTNAME` in
  [`PWMetadataRequest.m`](https://github.com/google/physical-web/blob/7c7b5c00f2e6eb08c9730be36a98954334a2e8c6/ios/PhyWeb/Backend/PWMetadataRequest.m#L22)


## How to run the Physical Web Service tests

1. Make sure you have created config.SECRET.json from the config.SAMPLE.json
   file.
2. Install nose `pip install nose`
3. Run ./tests.py -h to see help
4. Run ./tests.py to test the development server
