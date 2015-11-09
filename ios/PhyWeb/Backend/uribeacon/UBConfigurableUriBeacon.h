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

@class UBUriBeacon;

/**
 This class represents a configurable UriBeacon.

 Here's an example of how to configure a beacon:
 <pre><code>// Write beacon data.
 - (void)writeURI:(NSURL *)URI
        forBeacon:(UBConfigurableUriBeacon *)configurableBeacon {
   UBUriBeacon *beacon = [[UBUriBeacon alloc] initWithURI:URI
   txPowerLevel:32];
   [configurableBeacon writeBeacon:beacon
                   completionBlock:^(NSError *error) {
                       if (error != nil) {
                         NSLog(@"An error happened: %@", error);
                       } else {
                         NSLog(@"The beacon has been successfully
                         configured.");
                       }
                   }];
 }
 </code></pre>
*/
@interface UBConfigurableUriBeacon : NSObject

/** iOS bluetooth device identifier. */
@property(nonatomic, copy) NSUUID* identifier;

/** RSSI value. */
@property(nonatomic, assign) NSInteger RSSI;

/**
 * To hold a beacon in configuration mode, you need to connect to it.
 *
 * @param block The block will be called when the connection is established.
 */
- (void)connect:(void (^)(NSError* error))block;

/**
 * Disconnects when configuration is done to reflect the changes.
 *
 * @param block The block will be called when the connection is terminated.
 */
- (void)disconnect:(void (^)(NSError* error))block;

/**
 * Writes beacon advertisement data. The beacon must be connected before writing
 * to it.
 *
 * @param beacon The beacon information to write.
 * @param block The block will be called when the data have been written.
 */
- (void)writeBeacon:(UBUriBeacon*)beacon
    completionBlock:(void (^)(NSError* error))block;

/**
 * Reads beacon advertisement data. The beacon must be connected before reading
 * it.
 *
 * @param block The block will be called when the data have been read.
 */
- (void)readBeaconWithCompletionBlock:(void (^)(NSError* error,
                                                UBUriBeacon* beacon))block;

@end
