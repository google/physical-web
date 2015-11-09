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

#import "UriBeacon.h"

#import "PWBeacon.h"
#import "PWMetadataRequest.h"

#define DISCOVERY_DELAY_KEY @"discoveryDelay"

@interface PWBeaconManager ()<PWMetadataRequestDelegate,
                              NSNetServiceBrowserDelegate, NSNetServiceDelegate>

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
  // Whether the beacon manager started.
  BOOL _started;
  // mDNS browser for http.
  NSNetServiceBrowser* _httpServiceBrowser;
  // mDNS browser for https.
  NSNetServiceBrowser* _httpsServiceBrowser;
  // mDNS found that we need to resolve.
  NSMutableArray* _pendingNetServices;
  // Whether a resolution is in progress.
  BOOL _resolving;
  // URL related to a given service.
  NSMutableDictionary* _discoveredNetServicesURLs;
  // Names of the discovered services.
  NSMutableSet* _netServicesNames;
  NSTimeInterval _startTime;
  NSMutableArray* _pendingBeaconsInfos;
}

- (id)init {
  self = [super init];
  if (!self) {
    return nil;
  }
  _beaconsDict = [NSMutableDictionary dictionary];
  _changeBlocks = [NSMutableArray array];
  _configurationChangeBlocks = [NSMutableArray array];
  _requests = [NSMutableArray array];
  _pendingURLRequest = [NSMutableSet set];
  _pendingNetServices = [[NSMutableArray alloc] init];
  _pendingBeaconsInfos = [[NSMutableArray alloc] init];
  _stableMode = YES;
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
  _startTime = [NSDate timeIntervalSinceReferenceDate];
  _started = YES;
  PWBeaconManager* __weak weakSelf = self;
#if TODAY_EXTENSION
  _scanner = [[UBUriBeaconScanner alloc] initWithApplication:nil];
#else
  _scanner = [[UBUriBeaconScanner alloc]
      initWithApplication:[UIApplication sharedApplication]];
#endif
  [_scanner startScanningWithUpdateBlock:^{
    PWBeaconManager* strongSelf = weakSelf;
    [strongSelf _updateBeacons];
  }];
  _httpServiceBrowser = [[NSNetServiceBrowser alloc] init];
  [_httpServiceBrowser setDelegate:self];
  _httpsServiceBrowser = [[NSNetServiceBrowser alloc] init];
  [_httpsServiceBrowser setDelegate:self];
  [_httpServiceBrowser searchForServicesOfType:@"_http._tcp." inDomain:@""];
  [_httpsServiceBrowser searchForServicesOfType:@"_https._tcp." inDomain:@""];
  [_pendingNetServices removeAllObjects];
  _discoveredNetServicesURLs = [NSMutableDictionary dictionary];
  _netServicesNames = [NSMutableSet set];
  _resolving = NO;
}

- (NSURL*)_urlWithNetService:(NSNetService*)service {
  NSDictionary* txtRecords =
      [NSNetService dictionaryFromTXTRecordData:[service TXTRecordData]];
  NSString* path =
      [[NSString alloc] initWithData:[txtRecords objectForKey:@"path"]
                            encoding:NSUTF8StringEncoding];
  NSString* scheme = @"http";
  if ([[service type] isEqualToString:@"_http._tcp."]) {
    scheme = @"http";
  } else if ([[service type] isEqualToString:@"_https._tcp."]) {
    scheme = @"https";
  }
  NSString* hostname = [service hostName];
  NSString* urlString = nil;
  if ([hostname hasSuffix:@"."]) {
    hostname = [hostname substringToIndex:[hostname length] - 1];
  }
  if (([scheme isEqualToString:@"http"] && [service port] == 80) ||
      ([scheme isEqualToString:@"https"] && [service port] == 443)) {
    urlString = [NSString stringWithFormat:@"%@://%@", scheme, hostname];
  } else {
    urlString = [NSString
        stringWithFormat:@"%@://%@:%i", scheme, hostname, (int)[service port]];
  }
  if ([path length] != 0) {
    if ([path hasPrefix:@"/"]) {
      urlString = [urlString stringByAppendingString:path];
    } else {
      urlString = [urlString stringByAppendingFormat:@"/%@", path];
    }
  }
  return [NSURL URLWithString:urlString];
}

- (void)netServiceBrowser:(NSNetServiceBrowser*)netServiceBrowser
           didFindService:(NSNetService*)netService
               moreComing:(BOOL)moreServicesComing {
  [_pendingNetServices addObject:netService];
  NSString* name = [NSString
      stringWithFormat:@"%@:%@", [netService type], [netService name]];
  [_netServicesNames addObject:name];

  if (!moreServicesComing) {
    [self _resolveNextNetService];
  }
}

- (void)netServiceBrowser:(NSNetServiceBrowser*)netServiceBrowser
         didRemoveService:(NSNetService*)netService
               moreComing:(BOOL)moreServicesComing {
  NSString* name = [NSString
      stringWithFormat:@"%@:%@", [netService type], [netService name]];
  [_netServicesNames removeObject:name];
  [_discoveredNetServicesURLs removeObjectForKey:name];
  [self _cleanup];
}

- (void)_resolveNextNetService {
  if (_resolving) {
    return;
  }
  if ([_pendingNetServices count] == 0) {
    return;
  }

  _resolving = YES;

  NSNetService* netService = [_pendingNetServices objectAtIndex:0];
  [netService setDelegate:self];
  [netService resolveWithTimeout:0.5];

  [self performSelector:@selector(_skipResolve) withObject:nil afterDelay:0.5];
}

- (void)_skipResolve {
  _resolving = NO;
  NSNetService* netService = [_pendingNetServices objectAtIndex:0];
  [netService stop];

  [self _resolveNextNetService];
}

- (void)netServiceDidResolveAddress:(NSNetService*)netService {
  [NSObject cancelPreviousPerformRequestsWithTarget:self
                                           selector:@selector(_skipResolve)
                                             object:nil];
  _resolving = NO;
  NSURL* url = [self _urlWithNetService:netService];
  NSString* name = [NSString
      stringWithFormat:@"%@:%@", [netService type], [netService name]];
  [_discoveredNetServicesURLs setObject:url forKey:name];
  [netService stop];
  [_pendingNetServices removeObject:netService];
  [self _resolveNextNetService];

  PWBeacon* beacon = [_beaconsDict objectForKey:url];
  if (beacon == nil) {
    if (![_pendingURLRequest containsObject:url]) {
      // Request metadata of a new beacon.
      UBUriBeacon* uriBeacon =
          [[UBUriBeacon alloc] initWithURI:url txPowerLevel:0];
      NSTimeInterval currentTime = [NSDate timeIntervalSinceReferenceDate];
      NSTimeInterval discoveryDelay = currentTime - _startTime;
      NSMutableDictionary* pendingBeaconInfo =
          [[NSMutableDictionary alloc] init];
      pendingBeaconInfo[DISCOVERY_DELAY_KEY] =
          [NSNumber numberWithDouble:discoveryDelay];
      [_pendingBeaconsInfos addObject:pendingBeaconInfo];
      PWMetadataRequest* request = [[PWMetadataRequest alloc] init];
      [request setUriBeacons:@[ uriBeacon ]];
      [request setDelegate:self];
      [request start];
      [_pendingURLRequest addObject:[uriBeacon URI]];
      [_requests addObject:request];
    }
  }
}

- (void)netService:(NSNetService*)netService
     didNotResolve:(NSDictionary*)errorDict {
  [NSObject cancelPreviousPerformRequestsWithTarget:self
                                           selector:@selector(_skipResolve)
                                             object:nil];
  _resolving = NO;
  [netService stop];
  [_pendingNetServices removeObject:netService];
  [self _resolveNextNetService];
}

- (void)resetBeacons {
  [_beaconsDict removeAllObjects];
  [self _notify];
}

- (void)_updateBeacons {
  [self _notifyConfiguration];

  BOOL hasChange = NO;
  for (UBUriBeacon* uriBeacon in [_scanner beacons]) {
    if ([uriBeacon URI] == nil) {
      continue;
    }
    if ([uriBeacon RSSI] == 127) {
      continue;
    }
    if (!([[[[uriBeacon URI] scheme] lowercaseString]
              isEqualToString:@"http"] ||
          [[[[uriBeacon URI] scheme] lowercaseString]
              isEqualToString:@"https"])) {
      continue;
    }
    PWBeacon* beacon = [_beaconsDict objectForKey:[uriBeacon URI]];
    if (beacon == nil) {
      if (![_pendingURLRequest containsObject:[uriBeacon URI]]) {
        // Request metadata of a new beacon.
        NSTimeInterval currentTime = [NSDate timeIntervalSinceReferenceDate];
        NSTimeInterval discoveryDelay = currentTime - _startTime;
        NSMutableDictionary* pendingBeaconInfo =
            [[NSMutableDictionary alloc] init];
        pendingBeaconInfo[DISCOVERY_DELAY_KEY] =
            [NSNumber numberWithDouble:discoveryDelay];
        [_pendingBeaconsInfos addObject:pendingBeaconInfo];
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

  [_beaconsDict setObject:beacon forKey:[[beacon uriBeacon] URI]];
  NSUInteger idx = [_requests indexOfObject:request];
  if (idx != NSNotFound) {
    NSDictionary* beaconInfo = [_pendingBeaconsInfos objectAtIndex:idx];
    NSTimeInterval discoveryDelay =
        [beaconInfo[DISCOVERY_DELAY_KEY] doubleValue];
    NSTimeInterval requestDelay = [request delay];
    [beacon setDiscoveryDelay:discoveryDelay];
    [beacon setRequestDelay:requestDelay];
    [_pendingURLRequest removeObject:[uriBeacon URI]];
    [_pendingBeaconsInfos removeObjectAtIndex:idx];
    [_requests removeObjectAtIndex:idx];
  }
  [self _notify];
}

// Remove expired beacons.
- (BOOL)_cleanup {
  BOOL hasChange = NO;
  if (![self isStableMode]) {
    NSMutableSet* existingUrls = [NSMutableSet set];
    for (UBUriBeacon* beacon in [_scanner beacons]) {
      if ([beacon URI] != nil) {
        [existingUrls addObject:[beacon URI]];
      }
    }
    [existingUrls addObjectsFromArray:[_discoveredNetServicesURLs allValues]];
    for (NSURL* key in [_beaconsDict allKeys]) {
      if (![existingUrls containsObject:key]) {
        [_beaconsDict removeObjectForKey:key];
        hasChange = YES;
      }
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
  [_httpServiceBrowser stop];
  [_httpsServiceBrowser stop];
  [_scanner stopScanning];
  _started = NO;
}

- (BOOL)isStarted {
  return _started;
}

- (void)serializeBeacons:(NSArray*)beacons {
  NSUserDefaults* shared =
      [[NSUserDefaults alloc] initWithSuiteName:@"group.physical-web.iosapp"];
  NSMutableArray* encoded = [[NSMutableArray alloc] init];
  for (PWBeacon* beacon in beacons) {
    NSMutableDictionary* item = [[NSMutableDictionary alloc] init];
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

- (NSArray*)unserializedBeacons {
  NSUserDefaults* shared =
      [[NSUserDefaults alloc] initWithSuiteName:@"group.physical-web.iosapp"];
  NSArray* encoded = [shared objectForKey:@"LastSeenBeacons"];
  NSMutableArray* beacons = [[NSMutableArray alloc] init];
  for (NSDictionary* item in encoded) {
    PWBeacon* beacon = [[PWBeacon alloc] init];
    if (item[@"url"] != nil) {
      [beacon setURL:[NSURL URLWithString:item[@"url"]]];
    }
    [beacon setTitle:item[@"title"]];
    [beacons addObject:beacon];
  }
  return beacons;
}

- (NSTimeInterval)startTime {
  return _startTime;
}

@end
