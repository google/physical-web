#!/bin/sh
folder="`pwd`"
version=`defaults read "$folder/Info.plist" CFBundleShortVersionString`
version=`echo $version|sed -e 's/\(.*\)-.*/\1/'`
defaults write "$folder/Info.plist" CFBundleShortVersionString "${version}-dev"
