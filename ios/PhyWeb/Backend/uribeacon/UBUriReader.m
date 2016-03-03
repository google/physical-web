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

#import "UBUriReader.h"

#include "UBUriBeaconPrivate.h"

@interface UBUriReader () <CBPeripheralDelegate>

@end

@implementation UBUriReader {
  void (^_completionBlock)(NSError *error, NSData *data);
  NSData *_data;
}

- (id)init {
  self = [super init];
  if (!self) {
    return nil;
  }

  return self;
}

- (void)dealloc {
  [_peripheral setDelegate:nil];
}

- (void)readWithCompletionBlock:(void (^)(NSError *error, NSData *data))block {
  _completionBlock = [block copy];

  [_peripheral setDelegate:self];
  [_peripheral discoverServices:@[ [CBUUID UUIDWithString:CONFIG_V2_SERVICE] ]];
}

- (void)peripheral:(CBPeripheral *)peripheral
    didDiscoverServices:(NSError *)error {
  if (error != nil) {
    [self _readDoneWithError:error];
    return;
  }

  CBService *service = nil;
  for (CBService *s in [peripheral services]) {
    if ([[[s UUID] UUIDString] caseInsensitiveCompare:CONFIG_V2_SERVICE] ==
        NSOrderedSame) {
      service = s;
    }
  }

  [peripheral discoverCharacteristics:@[
    [CBUUID UUIDWithString:[self characteristic]]
  ] forService:service];
}

- (void)peripheral:(CBPeripheral *)peripheral
    didDiscoverCharacteristicsForService:(CBService *)service
                                   error:(NSError *)error {
  if (error != nil) {
    [self _readDoneWithError:error];
    return;
  }

  CBCharacteristic *c = nil;
  for (CBCharacteristic *characteristic in [service characteristics]) {
    if ([[[characteristic UUID] UUIDString]
            caseInsensitiveCompare:[self characteristic]] == NSOrderedSame) {
      c = characteristic;
    }
  }

  [peripheral readValueForCharacteristic:c];
}

- (void)peripheral:(CBPeripheral *)peripheral
    didUpdateValueForCharacteristic:(CBCharacteristic *)characteristic
                              error:(NSError *)error {
  _data = [characteristic value];
  [self _readDoneWithError:error];
}

- (void)_readDoneWithError:(NSError *)error {
  [_peripheral setDelegate:nil];
  _completionBlock(error, _data);
}

@end
