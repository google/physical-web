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

#import "UBUriBeaconReader.h"

#include "UBUriBeaconPrivate.h"

enum {
  NEED_READ_DATASIZE,
  NEED_READ_DATA1,
  NEED_READ_DATA2,
};

@interface UBUriBeaconReader ()<CBPeripheralDelegate>

@end

@implementation UBUriBeaconReader {
  int _state;
  NSMutableDictionary *_characteristics;
  void (^_completionBlock)(NSError *error, NSData *data);
  NSUInteger _length;
  NSMutableData *_result;
}

- (id)init {
  self = [super init];
  if (!self) {
    return nil;
  }

  _state = NEED_READ_DATA1;

  return self;
}

- (void)dealloc {
  [_peripheral setDelegate:nil];
}

- (void)readWithCompletionBlock:(void (^)(NSError *error, NSData *data))block {
  _completionBlock = [block copy];

  [_peripheral setDelegate:self];
  [_peripheral discoverServices:@[ [CBUUID UUIDWithString:CONFIG_V1_SERVICE] ]];
}

- (void)peripheral:(CBPeripheral *)peripheral
    didDiscoverServices:(NSError *)error {
  if (error != nil) {
    [self _readDoneWithError:error];
    return;
  }

  CBService *service = [[peripheral services] objectAtIndex:0];
  [peripheral discoverCharacteristics:
                  @[
                    [CBUUID UUIDWithString:CONFIG_V1_CHARACTERISTIC_DATA1],
                    [CBUUID UUIDWithString:CONFIG_V1_CHARACTERISTIC_DATA2],
                    [CBUUID UUIDWithString:CONFIG_V1_CHARACTERISTIC_DATASIZE]
                  ] forService:service];
}

- (void)peripheral:(CBPeripheral *)peripheral
    didDiscoverCharacteristicsForService:(CBService *)service
                                   error:(NSError *)error {
  if (error != nil) {
    [self _readDoneWithError:error];
    return;
  }

  _characteristics = [NSMutableDictionary dictionary];
  for (CBCharacteristic *characteristic in [service characteristics]) {
    [_characteristics setObject:characteristic forKey:[characteristic UUID]];
  }

  _result = [NSMutableData data];
  _state = NEED_READ_DATASIZE;
  CBCharacteristic *c = [_characteristics
      objectForKey:[CBUUID UUIDWithString:CONFIG_V1_CHARACTERISTIC_DATASIZE]];
  [peripheral readValueForCharacteristic:c];
}

- (void)peripheral:(CBPeripheral *)peripheral
    didUpdateValueForCharacteristic:(CBCharacteristic *)characteristic
                              error:(NSError *)error {
  if (error != nil) {
    [self _readDoneWithError:error];
    return;
  }

  NSData *data = nil;
  switch (_state) {
    case NEED_READ_DATASIZE:
      data = [characteristic value];
      if ([data length] > 0) {
        _length = ((const char *)[data bytes])[0];
      }
      if (_length == 0) {
        [self _readDoneWithError:nil];
      } else {
        _state = NEED_READ_DATA1;
        CBCharacteristic *c = [_characteristics
            objectForKey:[CBUUID UUIDWithString:CONFIG_V1_CHARACTERISTIC_DATA1]];
        [peripheral readValueForCharacteristic:c];
      }
      break;
    case NEED_READ_DATA1:
      [_result appendData:[characteristic value]];
      if (_length <= 20) {
        [self _readDoneWithError:nil];
      } else {
        _state = NEED_READ_DATA2;
        CBCharacteristic *c = [_characteristics
            objectForKey:[CBUUID UUIDWithString:CONFIG_V1_CHARACTERISTIC_DATA2]];
        [peripheral readValueForCharacteristic:c];
      }
      break;
    case NEED_READ_DATA2:
      [_result appendData:[characteristic value]];
      [self _readDoneWithError:nil];
      break;
  }
}

- (void)_readDoneWithError:(NSError *)error {
  [_peripheral setDelegate:nil];
  _completionBlock(error, _result);
}

@end
