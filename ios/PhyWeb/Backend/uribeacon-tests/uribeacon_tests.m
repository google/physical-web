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

#import <CoreBluetooth/CoreBluetooth.h>
#import <UIKit/UIKit.h>
#import <UriBeacon/UriBeacon.h>
#import <XCTest/XCTest.h>

#import "NSData+UBUnitTest.h"

// Redeclare private methods for testing.
@interface UBUriBeacon (Testing)

- (id)initWithPeripheral:(CBPeripheral *)peripheral
                    data:(NSData *)data
                    RSSI:(NSInteger)RSSI;

- (NSData *)_advertisementData;

@end

@interface uribeacon_tests : XCTestCase {
  NSDictionary *_tests;
}

@end

@implementation uribeacon_tests

- (void)setUp {
  [super setUp];
  NSString *filename =
      [[NSBundle bundleForClass:[self class]] pathForResource:@"testdata"
                                                       ofType:@"json"];
  NSData *data = [[NSData alloc] initWithContentsOfFile:filename];
  NSError *error = nil;
  _tests = [NSJSONSerialization JSONObjectWithData:data options:0 error:&error];
  if (error != nil) {
    NSLog(@"error: %@", error);
  }
}

- (void)tearDown {
  [super tearDown];
}

- (NSString *)_jsonStringWithScanRecord:(NSArray *)scanRecord {
  NSData *jsonData =
      [NSJSONSerialization dataWithJSONObject:scanRecord options:0 error:NULL];
  return [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
}

- (void)testEncode {
  NSArray *encodingTests = _tests[@"test-data"];
  unsigned int count = 0;
  unsigned int failure = 0;
  for (NSDictionary *encodingTest in encodingTests) {
    NSURL *url = [[NSURL alloc] initWithString:encodingTest[@"url"]];
    NSNumber *tx = encodingTest[@"tx"];
    if (tx == nil) {
      tx = @20;
    }
    NSNumber *flags = encodingTest[@"flags"];
    if (flags == nil) {
      flags = @0;
    }
    UBUriBeacon *beacon = [[UBUriBeacon alloc] initWithURI:url
                                              txPowerLevel:[tx intValue]
                                                     flags:[flags intValue]];
    if (encodingTest[@"scanRecord"] == [NSNull null]) {
      XCTAssert(![beacon isValid]);
    }

    if (encodingTest[@"scanRecord"] == [NSNull null]) {
      XCTAssert(![beacon isValid]);
      if ([beacon isValid]) {
        failure++;
      }
    } else {
      NSString *scanRecord =
          [self _jsonStringWithScanRecord:
                    [[beacon _advertisementData] ub_unitTestValue]];
      NSString *expectedValue =
          [self _jsonStringWithScanRecord:encodingTest[@"scanRecord"]];
      if (![scanRecord isEqualToString:expectedValue]) {
        NSLog(@"failed %@", url);
        failure++;
      }
      XCTAssertEqualObjects(scanRecord, expectedValue);
    }
    count++;
  }
  NSLog(@"tested %i URLs, %i passed, %i failed", (int)count,
        (int)(count - failure), (int)failure);
}

- (void)testDecode {
  NSArray *encodingTests = _tests[@"test-data"];
  unsigned int failure = 0;
  unsigned int count = 0;
  for (NSDictionary *encodingTest in encodingTests) {
    NSURL *url = [[NSURL alloc] initWithString:encodingTest[@"url"]];
    NSNumber *tx = encodingTest[@"tx"];
    if (tx == nil) {
      tx = @20;
    }
    NSNumber *flags = encodingTest[@"flags"];
    if (flags == nil) {
      flags = @0;
    }
    NSData *data =
        [NSData ub_dataWithUnitTestValue:encodingTest[@"scanRecord"]];
    if (data != nil) {
      UBUriBeacon *beacon =
          [[UBUriBeacon alloc] initWithPeripheral:nil data:data RSSI:0];
      if ([[url absoluteString] length] == 0) {
        url = nil;
      }
      if (![url isEqual:[beacon URI]]) {
        NSLog(@"failed %@", url);
        failure++;
      }
      XCTAssertEqualObjects(url, [beacon URI]);
      count++;
    }
  }
  NSLog(@"tested %i URLs, %i passed, %i failed", (int)[encodingTests count],
        (int)([encodingTests count] - failure), (int)failure);
}

@end
