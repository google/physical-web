#/usr/bin/env/sh

#browse to the android project
cd android/PhysicalWeb/

#grant file execute permissions
chmod +x ./gradlew

#build a specific gradle task
./gradlew assembleRelease
