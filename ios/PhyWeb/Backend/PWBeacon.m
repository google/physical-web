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

#import "PWBeacon.h"

@implementation PWBeacon {
  NSMutableArray * _rssiHistory;
}

- (id)initWithUriBeacon:(UBUriBeacon *)beacon info:(NSDictionary *)info {
  self = [self init];

  _rssiHistory = [[NSMutableArray alloc] init];
  NSString *title = info[@"title"];
  if (title == (NSString *)[NSNull null]) {
    title = nil;
  }
  NSString *desc = info[@"description"];
  if (desc == (NSString *)[NSNull null]) {
    desc = nil;
  }
  NSString *icon = info[@"icon"];
  if (icon == (NSString *)[NSNull null]) {
    icon = nil;
  }
  NSString *urlString = info[@"url"];
  if (urlString == (NSString *)[NSNull null]) {
    urlString = nil;
  }
  NSString *displayUrlString = info[@"displayUrl"];
  if (displayUrlString == (NSString *)[NSNull null]) {
    displayUrlString = nil;
  }
  NSNumber *rankNumber = info[@"rank"];
  if (rankNumber == (NSNumber *)[NSNull null]) {
    rankNumber = nil;
  }

  if ([title length] == 0) {
    title = nil;
  }
  if ([desc length] == 0) {
    desc = nil;
  }
  if ([icon length] == 0) {
    icon = nil;
  }

  [self setUriBeacon:beacon];
  if (beacon != nil) {
    [self setURL:[beacon URI]];
  } else if (urlString != nil) {
    [self setURL:[NSURL URLWithString:urlString]];
  }
  [self setHasDisplayURL:displayUrlString != nil];
  [self setDisplayURL:displayUrlString != nil
                          ? [NSURL URLWithString:displayUrlString]
                          : [self URL]];
  [self setTitle:title];
  [self setSnippet:desc];
  [self setIconURL:[NSURL URLWithString:icon]];
  if (rankNumber != nil) {
    [self setHasRank:YES];
    [self setRank:[rankNumber doubleValue]];
  }

  return self;
}

static NSString *regionName(UBUriBeaconRegion region) {
  switch (region) {
    case UBUriBeaconRegionNear:
      return @"near";
    case UBUriBeaconRegionMid:
      return @"mid";
    case UBUriBeaconRegionFar:
      return @"far";
    case UBUriBeaconRegionUnknown:
    default:
      return @"unk";
  }
}

- (NSString *)debugRegionName {
  return regionName([self region]);
}

- (NSString *)debugUriRegionName {
  return regionName([[self uriBeacon] region]);
}

- (NSArray *) rssiHistory {
  return _rssiHistory;
}

- (void) setUriBeacon:(UBUriBeacon *)uriBeacon {
  NSTimeInterval timestamp = [NSDate timeIntervalSinceReferenceDate];
  if ([[NSUserDefaults standardUserDefaults] boolForKey:@"DebugMode"]) {
    [_rssiHistory addObject:@[[NSNumber numberWithInt:(int) [uriBeacon RSSI]], [NSNumber numberWithDouble:timestamp]]];
  }
  _uriBeacon = uriBeacon;
}

@end
