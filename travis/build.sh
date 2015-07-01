#!/bin/sh

# Browse to the android project
cd android/PhysicalWeb/

# This will run our linters etc.
# NOTE: check depends on assembleDebug
./gradlew check
