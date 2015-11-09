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

#import "UBUriBeaconWriter.h"

#include "UBUriBeaconPrivate.h"

enum {
  NEED_WRITE_DATA1,
  NEED_WRITE_DATA2,
};

@interface UBUriBeaconWriter ()<CBPeripheralDelegate>

@end

@implementation UBUriBeaconWriter {
  int _state;
  NSMutableDictionary *_characteristics;
  void (^_completionBlock)(NSError *error);
  NSUInteger _length;
}

- (id)init {
  self = [super init];
  if (!self) {
    return nil;
  }

  _state = NEED_WRITE_DATA1;

  return self;
}

- (void)dealloc {
  [_peripheral setDelegate:nil];
}

- (void)writeWithCompletionBlock:(void (^)(NSError *error))block {
  _completionBlock = [block copy];

  [_peripheral setDelegate:self];
  [_peripheral discoverServices:@[ [CBUUID UUIDWithString:CONFIG_V1_SERVICE] ]];
}

- (void)peripheral:(CBPeripheral *)peripheral
    didDiscoverServices:(NSError *)error {
  if (error != nil) {
    [self _writeDoneWithError:error];
    return;
  }

  CBService *service = [[peripheral services] objectAtIndex:0];
  [peripheral discoverCharacteristics:
                  @[
                    [CBUUID UUIDWithString:CONFIG_V1_CHARACTERISTIC_DATA1],
                    [CBUUID UUIDWithString:CONFIG_V1_CHARACTERISTIC_DATA2]
                  ] forService:service];
}

- (void)peripheral:(CBPeripheral *)peripheral
    didDiscoverCharacteristicsForService:(CBService *)service
                                   error:(NSError *)error {
  if (error != nil) {
    [self _writeDoneWithError:error];
    return;
  }

  _characteristics = [NSMutableDictionary dictionary];
  for (CBCharacteristic *characteristic in [service characteristics]) {
    [_characteristics setObject:characteristic forKey:[characteristic UUID]];
  }

  CBCharacteristic *c = [_characteristics
      objectForKey:[CBUUID UUIDWithString:CONFIG_V1_CHARACTERISTIC_DATA1]];
  _length = [[self data] length];

  NSData *data = nil;
  if (_length > 20) {
    data = [[self data] subdataWithRange:NSMakeRange(0, 20)];
  } else {
    data = [self data];
  }
  [peripheral writeValue:data
       forCharacteristic:c
                    type:CBCharacteristicWriteWithResponse];
}

- (void)peripheral:(CBPeripheral *)peripheral
    didWriteValueForCharacteristic:(CBCharacteristic *)characteristic
                             error:(NSError *)error {
  if (error != nil) {
    [self _writeDoneWithError:error];
    return;
  }

  if (_state == NEED_WRITE_DATA2) {
    [self _writeDoneWithError:nil];
    return;
  }

  if (_length > 20) {
    NSData *data = [[self data] subdataWithRange:NSMakeRange(20, _length - 20)];
    _state = NEED_WRITE_DATA2;
    CBCharacteristic *c = [_characteristics
        objectForKey:[CBUUID UUIDWithString:CONFIG_V1_CHARACTERISTIC_DATA2]];
    [peripheral writeValue:data
         forCharacteristic:c
                      type:CBCharacteristicWriteWithResponse];
  } else {
    [self _writeDoneWithError:nil];
  }
}

- (void)_writeDoneWithError:(NSError *)error {
  [_peripheral setDelegate:nil];
  _completionBlock(error);
}

@end
