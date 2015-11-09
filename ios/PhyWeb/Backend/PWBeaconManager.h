/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import <Foundation/Foundation.h>
#import "UriBeacon.h"

@interface PWBeaconManager : NSObject

// Singleton.
+ (instancetype)sharedManager;

// Returns the list of nearby augmented beacons.
- (NSArray* /* PWBeacon */)beacons;

// Returns the list of configurable beacons.
- (NSArray* /* UBConfigurableUriBeacon */)configurableBeacons;

- (NSTimeInterval) startTime;

// Register a callback block when there's a change in the list of beacons.
// It returns an object that need to be used to unregister the callback.
// It needs to be passed to -unregisterChangeBlock:.
- (id)registerChangeBlock:(void (^)(void))block;

// Unregister a handler of change in list of beacons.
- (void)unregisterChangeBlock:(id)registeredBlock;

// Registers a callback block when the list of configurable beacons changed.
// It returns an object that need to be used to unregister the callback.
// It needs to be passed to -unregisterConfigurationChangeBlock:.
- (id)registerConfigurationChangeBlock:(void (^)(void))block;

// Unregister a handler of change in list of configurable beacons.
- (void)unregisterConfigurationChangeBlock:(id)registeredBlock;

// Starts scanning for beacons.
- (void)start;

// Stops scanning for beacons.
- (void)stop;

// Empty the list of beacons.
- (void)resetBeacons;

// Returns YES is it's scanning for beacons.
- (BOOL)isStarted;

// Store beacons in defaults shared between the today extension and the app.
// It will save only the URL and the title since it's only what we need to
// show them in the today extension.
- (void)serializeBeacons:(NSArray*)beacons;

// Retrieve the beacons from the defaults.
- (NSArray*)unserializedBeacons;

// List won't reorder if it's set to YES.
@property(nonatomic, assign, getter=isStableMode) BOOL stableMode;

@end
