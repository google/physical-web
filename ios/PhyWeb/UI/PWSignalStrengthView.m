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

#import "PWSignalStrengthView.h"

@implementation PWSignalStrengthView {
  NSInteger _quality;
}

@synthesize quality = _quality;

- (instancetype)initWithFrame:(CGRect)frame {
  self = [super initWithFrame:frame];
  self.opaque = NO;
  return self;
}

- (void)setQuality:(NSInteger)quality {
  _quality = quality;
  [self setNeedsDisplay];
}

- (void)drawRect:(CGRect)rect {
  [[UIColor blackColor] setFill];
  if (_quality >= 5) {
    CGRect rect = CGRectMake(0, 9, 3, 6);
    UIRectFill(rect);
  }
  if (_quality >= 25) {
    CGRect rect = CGRectMake(4, 6, 3, 9);
    UIRectFill(rect);
  }
  if (_quality >= 50) {
    CGRect rect = CGRectMake(8, 3, 3, 12);
    UIRectFill(rect);
  }
  if (_quality >= 75) {
    CGRect rect = CGRectMake(12, 0, 3, 15);
    UIRectFill(rect);
  }
}

@end
