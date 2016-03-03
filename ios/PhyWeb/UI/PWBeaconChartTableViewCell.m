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

#import "PWBeaconChartTableViewCell.h"

@implementation PWBeaconChartTableViewCell {
  UILabel *_rssiLabel;
}

@synthesize rssiLabel = _rssiLabel;

- (id)initWithStyle:(UITableViewCellStyle)style
    reuseIdentifier:(NSString *)reuseIdentifier {
  self = [super initWithStyle:style reuseIdentifier:reuseIdentifier];

  _rssiLabel = [[UILabel alloc] initWithFrame:CGRectZero];
  [_rssiLabel setTextAlignment:NSTextAlignmentRight];
  [[self contentView] addSubview:_rssiLabel];

  return self;
}

- (void)layoutSubviews {
  [super layoutSubviews];
  CGRect frame = CGRectMake(10, 10, [self bounds].size.width - 100, 40);
  [[self textLabel] setFrame:frame];

  frame = CGRectMake([self bounds].size.width - 90, 10, 80, 40);
  [_rssiLabel setFrame:frame];
}

@end
