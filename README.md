# The Physical Web

<img align="left" src="https://raw.githubusercontent.com/google/physical-web/master/documentation/images/logo/logo-black.png" hspace="15" width="70px" style="float: left">

The Physical Web is an effort to extend the superpower of the web - the URL - to everyday physical objects. Our premise is that you should be able to walk up to any “smart” physical object (e.g. a vending machine, a poster, a toy, a bus stop, a rental car) and interact with it without first downloading an app. The user experience of smart objects should be much like links in a web browser, just tap and use.

At its base, the Physical Web is a discovery service: a smart object broadcasts relevant URLs that any nearby device can receive. This simple capability can unlock exciting new ways to interact with the Web.

<a href="https://youtu.be/1yaLPRgtlR0" target="_blank"><img src="http://img.youtube.com/vi/1yaLPRgtlR0/0.jpg" alt="Introduction to the Physical Web" width="240" height="180" border="10" /></a>

[![Build Status](https://travis-ci.org/google/physical-web.svg?branch=master)](https://travis-ci.org/google/physical-web)

## Why URLs?

The URL is the fundamental building block of the web, giving remarkable flexibility of expression. It can be:

* a web page with just a tiny paragraph of info
* a fully interactive web page
* a deep link into a native application

## Why We're Doing This
The number of smart objects is going to explode, both in our homes and in public spaces. Much like the web, there is going to be a long tail of interactivity for smart objects. But the overhead of installing an app for each one just doesn’t  scale. We need a system that lets you walk up and use a device with just a tap. The Physical Web isn’t about replacing native apps; it’s about allowing interaction for the times when native apps just aren’t practical.

## Open Design
The Physical Web must be an open standard that everyone can use. This can’t be a product that is locked down by a single company. Like many web specifications, this is an open source design that is being released early so everyone can experiment and comment on it. There is much to discuss and add to this specification.

## Contents
* [Introduction/FAQ](http://github.com/google/physical-web/blob/master/documentation/introduction.md) - Most common questions
* [Getting started guide](http://github.com/google/physical-web/blob/master/documentation/getting_started.md) - How you can try it out
* [Technical overview](https://github.com/google/physical-web/blob/master/documentation/technical_overview.md)
* [Eddystone](https://github.com/google/eddystone) - Repo with the specification for the beacons themselves
* [mDNS support](https://github.com/google/physical-web/blob/master/documentation/mDNS_Support.md) - How to use mDNS instead of Bluetooth to broadcast public URLs and local data
* [Wi-Fi Direct support](https://github.com/google/physical-web/blob/master/documentation/WiFi_Direct_Support.md) - How to use Wi-Fi Direct instead of Bluetooth to broadcast local data
* [SSDP support](https://github.com/google/physical-web/blob/master/documentation/ssdp_support.md) - How to use SSDP instead of Bluetooth to broadcast URLs
* [Branding guidelines](https://github.com/google/physical-web/blob/master/documentation/branding_guidelines.md)
* Different Physical Web clients
    * [Android](android)  
    * [IOS](ios)  
    * [Node](nodejs)  This is a minimal client meant for building utilities
