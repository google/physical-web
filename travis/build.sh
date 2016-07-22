#!/bin/sh

# Test the android project
(
    cd android/PhysicalWeb/
    # This will run our linters etc.
    # NOTE: check depends on assembleDebug
    ./gradlew check
    ./gradlew test
)

# Test the Physical Web java libraries
(
    cd java/libs
    ./gradlew check
    ./gradlew test
)
