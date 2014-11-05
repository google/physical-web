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

#import "PWGradientView.h"

@implementation PWGradientView {
  UIColor* _bottomColor;
  UIColor* _topColor;
}

@synthesize bottomColor = _bottomColor;
@synthesize topColor = _topColor;

- (instancetype)initWithFrame:(CGRect)frame {
  self = [super initWithFrame:frame];
  self.opaque = NO;
  return self;
}

- (void)drawRect:(CGRect)rect {
  CGContextRef context = UIGraphicsGetCurrentContext();

  CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
  CGFloat locations[3] = {0., 0.6, 1.};
  CGFloat components[12] = {1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 0.};
  CGGradientRef gradient =
      CGGradientCreateWithColorComponents(colorSpace, components, locations, 3);
  CGRect bounds = [self bounds];
  CGPoint point = bounds.origin;
  point.y += bounds.size.height;
  CGContextDrawLinearGradient(context, gradient, bounds.origin, point, 0);
  CFRelease(gradient);
  CFRelease(colorSpace);

  [super drawRect:rect];
}

@end
