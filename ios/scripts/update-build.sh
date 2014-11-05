#!/bin/sh
folder="`pwd`"
build_version=`defaults read "$folder/Info.plist" CFBundleVersion`
build_version=$(($build_version+1))
defaults write "$folder/Info.plist" CFBundleVersion $build_version
version=`defaults read "$folder/Info.plist" CFBundleShortVersionString`
version=`echo $version|sed -e 's/\(.*\)-.*/\1/'`
defaults write "$folder/Info.plist" CFBundleShortVersionString $version
