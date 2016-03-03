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

#import "NSData+UBUnitTest.h"

@implementation NSData (UBUnitTest)

- (NSArray *)ub_unitTestValue {
  NSMutableArray *result = [NSMutableArray array];
  NSMutableString *currentString = nil;
  const unsigned char *buf = [self bytes];
  for (NSUInteger i = 0; i < [self length]; ++i) {
    // Convert to string data after offset 10.
    if ((buf[i] >= 32) && (buf[i] < 127) && (i >= 10)) {
      if (currentString == nil) {
        currentString = [NSMutableString string];
      }
      [currentString appendFormat:@"%c", buf[i]];
    } else {
      if (currentString != nil) {
        [result addObject:currentString];
        currentString = nil;
      }
      [result addObject:[NSNumber numberWithInt:buf[i]]];
    }
  }
  if (currentString != nil) {
    [result addObject:currentString];
    currentString = nil;
  }
  return result;
}

+ (NSData *)ub_dataWithUnitTestValue:(NSArray *)array {
  if (array == nil) {
    return nil;
  }
  if (array == (NSArray *)[NSNull null]) {
    return nil;
  }
  NSMutableData *result = [NSMutableData data];
  for (NSUInteger i = 0; i < [array count]; i++) {
    if ([array[i] isKindOfClass:[NSNumber class]]) {
      unsigned char value = [array[i] intValue];
      [result appendBytes:&value length:1];
    } else if ([array[i] isKindOfClass:[NSString class]]) {
      [result appendData:[(NSString *)array[i]
                             dataUsingEncoding:NSUTF8StringEncoding]];
    }
  }
  return result;
}

@end
