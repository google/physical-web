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
#import <UIKit/UIKit.h>

@class UBUriBeacon;
@class UBConfigurableUriBeacon;

/**
 UBUriBeaconScanner will scan for UriBeacons and configurable UriBeacons.
 It will scan for beacons when the application is running in the background if
 the application supports it. You'll need to add the background capability
 `bluetooth-central` in the Info.plist file. When it's scanning in background,
 only non-configurable beacons will be discovered.

 Here's an example of how to scan beacons:
 <pre><code>_scanner = [[UBUriBeaconScanner alloc]
     initWithApplication:[UIApplication sharedApplication]];
 [_scanner startScanningWithUpdateBlock:^{
   NSLog(@"beacons: %@", [_scanner beacons]);
   NSLog(@"configurable beacons: %@", [_scanner configurableBeacons]);
 }];
 </code></pre>
 */
@interface UBUriBeaconScanner : NSObject

/**
 * Initializes the scanner.
 * The -[UIApplication sharedApplication] should be given as argument.
 * If you're running in an extension, you should use nil.
 */
- (id)initWithApplication:(UIApplication*)application;

/**
 * Start discovering UriBeacons. It will check frequently for beacon
 * to know whether some appear or disappear.
 * @param block The block will be called when there's a change in the list of
 * beacons or configurable beacons.
 */
- (void)startScanningWithUpdateBlock:(void (^)(void))block;

/** Stop discovery of UriBeacons and configurable UriBeacons. */
- (void)stopScanning;

/** Returns the list of nearby UriBeacons. */
- (NSArray* /* UBUriBeacon */)beacons;

/** Returns the list of UriBeacons that can be configured. */
- (NSArray* /* UBConfigurableUriBeacon */)configurableBeacons;

@end
