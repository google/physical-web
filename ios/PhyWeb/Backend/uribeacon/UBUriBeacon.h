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

// Flags of UI Beacons.
typedef NSUInteger UBUriBeaconFlags;

typedef enum UBUriBeaconRegion {
  UBUriBeaconRegionNear, /* between 0 and 0.5 meter */
  UBUriBeaconRegionMid, /* between 0.5 and 2 meters */
  UBUriBeaconRegionFar, /* larger than 2 meters */
  UBUriBeaconRegionUnknown, /* distance could not be determined */
} UBUriBeaconRegion;

/**
 * UBUriBeacon holds the decoded content of a UriBeacon and some information
 * related to the physical device such as the identifier and the received signal
 * strength indicator.
 */
@interface UBUriBeacon : NSObject

/**
 * Initializes a UBUriBeacon with a URI and a transmit power.
 *
 * @param URI URI of the beacon.
 * @param txPowerLevel Transmit power level of the beacon.
 */
- (id)initWithURI:(NSURL*)URI txPowerLevel:(int)txPowerLevel;

/**
 * Initializes a UBUriBeacon with a URI, a transmit power and flags.
 *
 * @param URI URI of the beacon.
 * @param txPowerLevel Transmit power level of the beacon.
 * @param flags Flags of the beacon. It should be set to 0.
 */
- (id)initWithURI:(NSURL*)URI
     txPowerLevel:(int)txPowerLevel
            flags:(UBUriBeaconFlags)flags;

/** iOS bluetooth device identifier. */
@property(nonatomic, copy) NSUUID* identifier;

/**
 * When UBUriBeacon is returned from `-[UBUriBeaconScanner beacons]`,
 * the value is the current received signal strength indicator of the
 * related bluetooth device, in decibels.
 */
@property(nonatomic, assign) NSInteger RSSI;

/** URI of the beacon. */
@property(nonatomic, copy) NSURL* URI;

/** Transmit power level. */
@property(nonatomic, assign) int txPowerLevel;

/** Flags. The flags need to be set to 0. */
@property(nonatomic, assign) UBUriBeaconFlags flags;

/** returns YES if it's going to fit in a bluetooth LE advertisement packet. */
- (BOOL)isValid;

/** returns the region of the beacon */
- (UBUriBeaconRegion)region;

@end
