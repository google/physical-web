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

#import "UBUriBeaconScanner.h"

#import <CoreBluetooth/CoreBluetooth.h>

@interface UBUriBeaconScanner (Private)

- (void)_connectBeaconWithPeripheral:(CBPeripheral *)peripheral
                     completionBlock:(void (^)(NSError *error))block;

- (void)_disconnectBeaconWithPeripheral:(CBPeripheral *)peripheral
                        completionBlock:(void (^)(NSError *error))block;

- (void)_writeBeaconWithPeripheral:(CBPeripheral *)peripheral
                 advertisementData:(NSData *)data
                   completionBlock:(void (^)(NSError *error))block;

- (void)_readBeaconWithPeripheral:(CBPeripheral *)peripheral
                  completionBlock:(void (^)(NSError *error, NSData *data))block;

- (void)_writeURIv2WithPeripheral:(CBPeripheral *)peripheral
                              url:(NSURL *)url
                  completionBlock:(void (^)(NSError *error))block;

- (void)_readURIv2WithPeripheral:(CBPeripheral *)peripheral
                 completionBlock:(void (^)(NSError *error, NSURL *uri))block;

@end
