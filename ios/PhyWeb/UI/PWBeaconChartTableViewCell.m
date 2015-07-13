//
//  PWBeaconChartTableViewCell.m
//  PhyWeb
//
//  Created by Hoa Dinh on 7/7/15.
//  Copyright © 2015 Hoa Dinh. All rights reserved.
//

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
