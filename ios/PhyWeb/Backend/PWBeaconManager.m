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

#import "PWBeaconManager.h"

#import <UriBeacon/UriBeacon.h>

#import "PWBeacon.h"
#import "PWMetadataRequest.h"
#import "PWURLShortener.h"

@interface PWBeaconManager ()<PWMetadataRequestDelegate>

@end

@implementation PWBeaconManager {
  // Underlying beacon scanner from UriBeacon library.
  UBUriBeaconScanner* _scanner;
  // Map of URL to augmented beacons.
  NSMutableDictionary* _beaconsDict; /* URL -> PWBeacon */
  // Handler when the list of beacons change.
  NSMutableArray* /* Block */ _changeBlocks;
  // Handler when the list of configurable beacons change.
  NSMutableArray* /* Block */ _configurationChangeBlocks;
  // List of metadata requests.
  NSMutableArray* /* PWMetadataRequest */ _requests;
  // Set of URLs that are being requested.
  NSMutableSet* /* NSURL */ _pendingURLRequest;
  BOOL _started;
}

- (id)init {
  self = [super init];
  if (!self) {
    return nil;
  }
  _beaconsDict = [NSMutableDictionary dictionary];
  _scanner = [[UBUriBeaconScanner alloc] init];
  _changeBlocks = [NSMutableArray array];
  _configurationChangeBlocks = [NSMutableArray array];
  _requests = [NSMutableArray array];
  _pendingURLRequest = [NSMutableSet set];
  return self;
}

+ (instancetype)sharedManager {
  static dispatch_once_t onceToken;
  static PWBeaconManager* instance = nil;
  dispatch_once(&onceToken, ^{
      if (instance == nil) {
        instance = [[PWBeaconManager alloc] init];
      }
  });
  return instance;
}

- (NSArray* /* PWBeacon */)beacons {
  return [_beaconsDict allValues];
}

- (NSArray* /* UBConfigurableUriBeacon */)configurableBeacons {
  return [_scanner configurableBeacons];
}

- (id)registerChangeBlock:(void (^)(void))block {
  void (^blockCopy)(void) = [block copy];
  [_changeBlocks addObject:blockCopy];
  return blockCopy;
}

- (void)unregisterChangeBlock:(id)registeredBlock {
  [_changeBlocks removeObject:registeredBlock];
}

- (id)registerConfigurationChangeBlock:(void (^)(void))block {
  void (^blockCopy)(void) = [block copy];
  [_configurationChangeBlocks addObject:blockCopy];
  return blockCopy;
}

- (void)unregisterConfigurationChangeBlock:(id)registeredBlock {
  [_configurationChangeBlocks removeObject:registeredBlock];
}

- (void)start {
  _started = YES;
  PWBeaconManager* __weak weakSelf = self;
  [_scanner startScanningWithUpdateBlock:^{
      PWBeaconManager* strongSelf = weakSelf;
      [strongSelf _updateBeacons];
  }];
}

- (void)_updateBeacons {
  [self _notifyConfiguration];

  BOOL hasChange = NO;
  for (UBUriBeacon* uriBeacon in [_scanner beacons]) {
    if ([uriBeacon URI] == nil) {
      continue;
    }
      if (!([[[[uriBeacon URI] scheme] lowercaseString] isEqualToString:@"http"] || [[[[uriBeacon URI] scheme] lowercaseString] isEqualToString:@"https"])) {
          continue;
      }
    PWBeacon* beacon = [_beaconsDict objectForKey:[uriBeacon URI]];
    if (beacon == nil) {
      if (![_pendingURLRequest containsObject:[uriBeacon URI]]) {
        // Request metadata of a new beacon.
        PWMetadataRequest* request = [[PWMetadataRequest alloc] init];
        [request setUriBeacons:@[ uriBeacon ]];
        [request setDelegate:self];
        [request start];
        [_pendingURLRequest addObject:[uriBeacon URI]];
        [_requests addObject:request];
      }
    } else {
      // Updated existing beacon.
      [beacon setUriBeacon:uriBeacon];
      hasChange = YES;
    }
  }
  if ([self _cleanup]) {
    hasChange = YES;
  }
  if (hasChange) {
    [self _notify];
  }
}

- (void)metadataRequest_done:(PWMetadataRequest*)request {
  UBUriBeacon* uriBeacon = [[request uriBeacons] objectAtIndex:0];
  PWBeacon* beacon = nil;
  if ([request error] == nil && [[request results] count] > 0) {
    beacon = [[request results] objectAtIndex:0];
  } else {
    beacon = [[PWBeacon alloc] initWithUriBeacon:uriBeacon info:nil];
  }
  [PWURLShortener
       expandURL:[beacon URL]
      completion:^(NSError* error, NSURL* resultURL) {
          // Add beacon to the results.
          [beacon setURL:resultURL];
          [_beaconsDict setObject:beacon forKey:[[beacon uriBeacon] URI]];
          [_pendingURLRequest removeObject:[uriBeacon URI]];
          [_requests removeObject:request];
          [self _notify];
      }];
}

// Remove expired beacons.
- (BOOL)_cleanup {
  BOOL hasChange = NO;
  NSMutableSet* existingUrls = [NSMutableSet set];
  for (UBUriBeacon* beacon in [_scanner beacons]) {
    if ([beacon URI] != nil) {
      [existingUrls addObject:[beacon URI]];
    }
  }
  for (NSURL* key in [_beaconsDict allKeys]) {
    if (![existingUrls containsObject:key]) {
      [_beaconsDict removeObjectForKey:key];
      hasChange = YES;
    }
  }
  return hasChange;
}

- (void)_notify {
  for (void (^block)(void)in [_changeBlocks copy]) {
    block();
  }
}

- (void)_notifyConfiguration {
  for (void (^block)(void)in [_configurationChangeBlocks copy]) {
    block();
  }
}

- (void)stop {
  [_scanner stopScanning];
  _started = NO;
}

- (BOOL)isStarted {
  return _started;
}

- (void)serializeBeacons:(NSArray *)beacons {
  NSUserDefaults *shared =
      [[NSUserDefaults alloc] initWithSuiteName:@"org.physical-web.iosapp"];
  NSMutableArray *encoded = [[NSMutableArray alloc] init];
  for (PWBeacon *beacon in beacons) {
    NSMutableDictionary *item = [[NSMutableDictionary alloc] init];
    if ([beacon URL] != nil) {
      item[@"url"] = [[beacon URL] absoluteString];
    }
    if ([beacon title] != nil) {
      item[@"title"] = [beacon title];
    }
    [encoded addObject:item];
  }
  [shared setObject:encoded forKey:@"LastSeenBeacons"];
  [shared synchronize];
}

- (NSArray *)unserializedBeacons {
  NSUserDefaults *shared =
      [[NSUserDefaults alloc] initWithSuiteName:@"org.physical-web.iosapp"];
  NSArray *encoded = [shared objectForKey:@"LastSeenBeacons"];
  NSMutableArray *beacons = [[NSMutableArray alloc] init];
  for (NSDictionary *item in encoded) {
    PWBeacon *beacon = [[PWBeacon alloc] init];
    if (item[@"url"] != nil) {
      [beacon setURL:[NSURL URLWithString:item[@"url"]]];
    }
    [beacon setTitle:item[@"title"]];
    [beacons addObject:beacon];
  }
  return beacons;
}

@end
