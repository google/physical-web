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

#import "PWPlaceholderView.h"

#import "PWActivityIndicator.h"

@implementation PWPlaceholderView {
  UILabel *_placeholderLabel;
  PWActivityIndicator *_placeholderIcon;
  UIImageView *_bluetoothDisabledIcon;
  BOOL _showLabel;
  BOOL _bluetoothEnabled;
}

@synthesize showLabel = _showLabel;
@synthesize bluetoothEnabled = _bluetoothEnabled;

- (id)initWithFrame:(CGRect)frame {
  self = [super initWithFrame:frame];
  _placeholderIcon = [[PWActivityIndicator alloc] initWithFrame:CGRectZero];
  [self addSubview:_placeholderIcon];
  _bluetoothDisabledIcon = [[UIImageView alloc] initWithFrame:CGRectZero];
  [_bluetoothDisabledIcon setImage:[UIImage imageNamed:@"ScanError.png"]];
  [_bluetoothDisabledIcon setAlpha:0.0];
  [self addSubview:_bluetoothDisabledIcon];
  _placeholderLabel = [[UILabel alloc] initWithFrame:CGRectZero];
  [_placeholderLabel setTextAlignment:NSTextAlignmentCenter];
  [_placeholderLabel setFont:[UIFont boldSystemFontOfSize:20]];
  [_placeholderLabel setTextColor:[UIColor colorWithWhite:0.8 alpha:1.0]];
  [_placeholderLabel setText:@"No beacons detected"];
  [_placeholderLabel setNumberOfLines:0];
  [_placeholderLabel setAlpha:0.0];
  [self addSubview:_placeholderLabel];
  [_placeholderIcon start];
  return self;
}

- (void)layoutSubviews {
  CGRect bounds = [self bounds];
  if (bounds.size.width > bounds.size.height) {
    CGRect frame;
    frame.size =
        [_placeholderLabel sizeThatFits:CGSizeMake(bounds.size.width - 40, 0)];
    frame.size.width = bounds.size.width - 40;
    frame.origin = CGPointMake(20, 10);
    [_placeholderLabel setFrame:frame];
    frame.origin.x = (int)((bounds.size.width - 100) / 2);
    frame.origin.y = CGRectGetMaxY([_placeholderLabel frame]) + 10;
    frame.size.width = 100;
    frame.size.height = 100;
    frame = CGRectIntegral(frame);
    [_placeholderIcon setFrame:frame];
    [_bluetoothDisabledIcon setFrame:frame];
  } else {
    CGRect frame;
    frame.origin.x = (int)((bounds.size.width - 100) / 2);
    frame.origin.y = (bounds.size.height - 20) * 140 / 480;
    frame.size.width = 100;
    frame.size.height = 100;
    frame = CGRectIntegral(frame);
    [_placeholderIcon setFrame:frame];
    [_bluetoothDisabledIcon setFrame:frame];
    frame.size =
        [_placeholderLabel sizeThatFits:CGSizeMake(bounds.size.width - 40, 0)];
    frame.size.width = bounds.size.width - 40;
    frame.origin = CGPointMake(
        20, CGRectGetMinY([_placeholderIcon frame]) - frame.size.height - 10);
    [_placeholderLabel setFrame:frame];
  }
}

- (void)setShowLabel:(BOOL)showLabel {
  [self setShowLabel:showLabel animated:YES];
}

- (void)setShowLabel:(BOOL)showLabel animated:(BOOL)animated {
  if (!animated) {
    [_placeholderLabel setAlpha:showLabel ? 1.0 : 0.0];
  } else {
    [UIView animateWithDuration:0.25
                     animations:^{
                         [_placeholderLabel setAlpha:showLabel ? 1.0 : 0.0];
                     }];
  }
}

- (void)setBluetoothEnabled:(BOOL)enabled {
  _bluetoothEnabled = enabled;
  [UIView animateWithDuration:0.25
                   animations:^{
                       [_placeholderIcon setAlpha:enabled ? 1.0 : 0.0];
                       [_bluetoothDisabledIcon setAlpha:enabled ? 0.0 : 1.0];
                   }];
}

- (void)setLabel:(NSString *)label {
  [_placeholderLabel setText:label];
  [self setNeedsLayout];
}

- (NSString *)label {
  return [_placeholderLabel text];
}

- (void)start {
  [_placeholderIcon start];
}

- (void)stop {
  [_placeholderIcon stop];
}

@end
