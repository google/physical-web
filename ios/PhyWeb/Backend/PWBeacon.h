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

@interface PWBeacon : NSObject

- (id)initWithUriBeacon:(UBUriBeacon *)beacon info:(NSDictionary *)info;

@property(nonatomic, retain) UBUriBeacon *uriBeacon;

// URL of the page stored on the beacon.
@property(nonatomic, copy) NSURL *URL;

// URL of the page to display to the user.
@property(nonatomic, copy) NSURL *displayURL;

// Whether the display URL has been set by the server.
@property(nonatomic, assign) BOOL hasDisplayURL;

// Title of the page.
@property(nonatomic, copy) NSString *title;

// Snippet of the page.
@property(nonatomic, copy) NSString *snippet;

// Favicon.
@property(nonatomic, copy) NSURL *iconURL;

// Date of discovery of the beacon.
@property(nonatomic, retain) NSDate *date;

// The beacon is in the first batch. We should sort it by region.
@property(nonatomic, assign) BOOL sortByRegion;

// Region of the beacon.
@property(nonatomic, assign) UBUriBeaconRegion region;

// Rank of the beacon, computed by the metadata server.
@property(nonatomic, assign) double rank;

// Returns YES if the rank has been computed by the metadata server.
@property(nonatomic, assign) BOOL hasRank;

// Returns the delay to discover the beacon via bluetooth.
@property(nonatomic, assign) NSTimeInterval discoveryDelay;

// Returns the delay to get the metadata of the beacon via The Physical Web
// Server.
@property(nonatomic, assign) NSTimeInterval requestDelay;

@property (nonatomic, retain, readonly) NSArray * rssiHistory;

// Returns the region name of the beacon when it was created.
- (NSString *)debugRegionName;

// Returns the updated region name of the beacon.
- (NSString *)debugUriRegionName;

@end
