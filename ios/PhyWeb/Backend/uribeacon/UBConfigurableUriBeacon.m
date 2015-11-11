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

#import "UBConfigurableUriBeacon.h"
#import "UBConfigurableUriBeaconPrivate.h"

#import "UBUriBeaconScanner.h"
#import "UBUriBeaconScannerPrivate.h"
#import "UBUriBeacon.h"
#import "UBUriBeaconPrivate.h"
#import "NSURL+UB.h"

enum {
  URIBEACON_VERSION_NONE = 0,
  URIBEACON_V1 = 1,
  URIBEACON_V2 = 2,
};

@implementation UBConfigurableUriBeacon {
  UBUriBeaconScanner *_scanner;
  CBPeripheral *_peripheral;
  int _version;
}

- (id)initWithPeripheral:(CBPeripheral *)peripheral
       advertisementData:(NSDictionary *)advertisementData
                    RSSI:(NSNumber *)RSSI {
  self = [super init];
  if (!self) {
    return nil;
  }
  NSArray *serviceUUIDS =
      [advertisementData objectForKey:CBAdvertisementDataServiceUUIDsKey];
  if ([serviceUUIDS containsObject:[CBUUID UUIDWithString:CONFIG_V2_SERVICE]]) {
    _version = URIBEACON_V2;
  } else if ([serviceUUIDS
                 containsObject:[CBUUID UUIDWithString:CONFIG_V1_SERVICE]]) {
    _version = URIBEACON_V1;
  } else {
    return nil;
  }
  _peripheral = peripheral;
  [self setIdentifier:[peripheral identifier]];
  [self setRSSI:[RSSI intValue]];
  return self;
}

- (void)setScanner:(UBUriBeaconScanner *)scanner {
  _scanner = scanner;
}

- (UBUriBeaconScanner *)scanner {
  return _scanner;
}

- (BOOL)isEqual:(id)object {
  UBConfigurableUriBeacon *otherBeacon = object;
  return [[self identifier] isEqual:[otherBeacon identifier]] &&
         ([self RSSI] == [otherBeacon RSSI]);
}

- (void)connect:(void (^)(NSError *error))block {
  [[self scanner] _connectBeaconWithPeripheral:_peripheral
                               completionBlock:block];
}

- (void)disconnect:(void (^)(NSError *error))block {
  [[self scanner] _disconnectBeaconWithPeripheral:_peripheral
                                  completionBlock:block];
}

- (void)writeBeacon:(UBUriBeacon *)beacon
    completionBlock:(void (^)(NSError *error))block {
  if (_version == URIBEACON_V1) {
    [[self scanner] _writeBeaconWithPeripheral:_peripheral
                             advertisementData:[beacon _advertisementData]
                               completionBlock:block];
  } else if (_version == URIBEACON_V2) {
    [[self scanner] _writeURIv2WithPeripheral:_peripheral
                                          url:[beacon URI]
                              completionBlock:block];
  } else {
    NSAssert(0, @"invalid configurable beacon");
  }
}

- (void)readBeaconWithCompletionBlock:(void (^)(NSError *error,
                                                UBUriBeacon *beacon))block {
  if (_version == URIBEACON_V1) {
    [[self scanner] _readBeaconWithPeripheral:_peripheral
                              completionBlock:^(NSError *error, NSData *data) {
                                  UBUriBeacon *beacon = [[UBUriBeacon alloc]
                                      initWithPeripheral:_peripheral
                                                    data:data
                                                    RSSI:[self RSSI]];
                                  block(error, beacon);
                              }];
  } else if (_version == URIBEACON_V2) {
    [[self scanner]
        _readURIv2WithPeripheral:_peripheral
                 completionBlock:^(NSError *error, NSURL *uri) {
                     UBUriBeacon *beacon =
                         [[UBUriBeacon alloc] initWithURI:uri txPowerLevel:0];
                     block(error, beacon);
                 }];
  } else {
    NSAssert(0, @"invalid configurable beacon");
  }
}

- (void)_updateWithConfigurableBeacon:(UBConfigurableUriBeacon *)beacon {
  _peripheral = beacon->_peripheral;
  [self setRSSI:[beacon RSSI]];
}

- (NSString *)description {
  return [NSString stringWithFormat:@"<%@: %p %@ %li>", [self class], self,
                                    [self identifier], (long)[self RSSI]];
}

@end
