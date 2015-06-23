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

#import "PWMetadataRequest.h"

#import <CoreBluetooth/CoreBluetooth.h>
#import "PWBeacon.h"

#define PHYSICALWEB_SERVER_HOSTNAME @"url-caster.appspot.com"
#define PHYSICALWEB_SERVER_HOSTNAME_DEV @"url-caster-dev.appspot.com"

@interface PWMetadataRequest () <NSURLConnectionDataDelegate,
                                 NSURLConnectionDelegate>

@end

@implementation PWMetadataRequest {
  NSMutableArray *_results;
  NSMutableURLRequest *_request;
  NSURLConnection *_connection;
  NSMutableData *_data;
  NSTimeInterval _delay;
  NSTimeInterval _startTime;
}

@synthesize delay = _delay;

+ (NSString *)hostname {
  if ([[NSUserDefaults standardUserDefaults] boolForKey:@"DebugMode"]) {
    return PHYSICALWEB_SERVER_HOSTNAME_DEV;
  } else {
    return PHYSICALWEB_SERVER_HOSTNAME;
  }
}

- (id)init {
  self = [super init];
  return self;
}

- (void)start {
  _startTime = [NSDate timeIntervalSinceReferenceDate];
  if ([self isDemo]) {
    NSString *urlString = [NSString
        stringWithFormat:@"https://%@/demo", [PWMetadataRequest hostname]];
    NSURL *url = [NSURL URLWithString:urlString];
    _request = [[NSMutableURLRequest alloc] initWithURL:url];
  } else {
    NSMutableArray *jsonPeripherals = [[NSMutableArray alloc] init];
    for (UBUriBeacon *beacon in [self uriBeacons]) {
      NSDictionary *jsonPeripheral = @{
        @"url" : [[beacon URI] absoluteString],
        @"rssi" : [NSNumber numberWithLong:(long)[beacon RSSI]],
        @"txpower" : [NSNumber numberWithLong:(long)[beacon txPowerLevel]]
      };
      [jsonPeripherals addObject:jsonPeripheral];
    }
    NSDictionary *jsonBody = @{ @"objects" : jsonPeripherals };

    NSString *urlString =
        [NSString stringWithFormat:@"https://%@/resolve-scan",
                                   [PWMetadataRequest hostname]];
    NSURL *url = [NSURL URLWithString:urlString];
    _request = [[NSMutableURLRequest alloc] initWithURL:url];
    [_request setHTTPMethod:@"POST"];
    [_request setHTTPBody:[NSJSONSerialization dataWithJSONObject:jsonBody
                                                          options:0
                                                            error:NULL]];
  }
  _connection =
      [[NSURLConnection alloc] initWithRequest:_request delegate:self];
  [_connection start];
  _data = [[NSMutableData alloc] init];
}

- (void)cancel {
  [_connection cancel];
}

- (void)_done {
  if (_error != nil) {
    _results = [[NSMutableArray alloc] init];
    for (UBUriBeacon *uriBeacon in [self uriBeacons]) {
      PWBeacon *beacon =
          [[PWBeacon alloc] initWithUriBeacon:uriBeacon info:nil];
      [_results addObject:beacon];
    }
  }
  [[self delegate] metadataRequest_done:self];
}

- (NSArray *)results {
  return _results;
}

#pragma mark NSURLConnection delegate

- (void)connectionDidFinishLoading:(NSURLConnection *)connection {
  _delay = [NSDate timeIntervalSinceReferenceDate] - _startTime;
  _connection = nil;
  NSError *error;
  NSDictionary *result =
      [NSJSONSerialization JSONObjectWithData:_data options:0 error:&error];
  _error = error;
  if (_error != nil) {
    [self _done];
    return;
  }

  _results = [[NSMutableArray alloc] init];
  NSArray *list = result[@"metadata"];
  for (NSUInteger i = 0; i < [list count]; i++) {
    NSDictionary *item = list[i];
    UBUriBeacon *uriBeacon = nil;
    if ([self uriBeacons] != nil) {
      uriBeacon = [[self uriBeacons] objectAtIndex:i];
    }
    PWBeacon *beacon = [[PWBeacon alloc] initWithUriBeacon:uriBeacon info:item];
    [_results addObject:beacon];
  }

  [self _done];
}

- (void)connection:(NSURLConnection *)connection
    didFailWithError:(NSError *)error {
  _connection = nil;
  _error = error;
  [self _done];
}

- (NSURLRequest *)connection:(NSURLConnection *)connection
             willSendRequest:(NSURLRequest *)request
            redirectResponse:(NSURLResponse *)redirectResponse {
  return request;
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data {
  [_data appendData:data];
}

@end
