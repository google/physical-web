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

#import "PWBeaconCell.h"

#import "PWBeacon.h"
#import "PWSignalStrengthView.h"
#import <SDWebImage/UIImageView+WebCache.h>

#define FAVICON_WHITESPACE 1

#define TITLE_FONT_SIZE 18
#define TITLE_FONT [UIFont systemFontOfSize:TITLE_FONT_SIZE]
#if TODAY_EXTENSION
#define TITLE_COLOR [UIColor whiteColor]
#else
#define TITLE_COLOR [UIColor colorWithRed:0.1529 green:0 blue:0.6510 alpha:1.0]
#endif

#define URL_FONT_SIZE 14
#define URL_FONT [UIFont systemFontOfSize:URL_FONT_SIZE]
#if TODAY_EXTENSION
#define URL_COLOR [UIColor colorWithWhite:0.85 alpha:1.0]
#else
#define URL_COLOR [UIColor colorWithRed:0 green:0.4235 blue:0.098 alpha:1.0]
#endif

#define DESC_FONT_SIZE 14
#define DESC_FONT [UIFont systemFontOfSize:URL_FONT_SIZE]
#if TODAY_EXTENSION
#define DESC_COLOR [UIColor whiteColor]
#else
#define DESC_COLOR [UIColor blackColor]
#endif

#define FAVICON_SIZE 20
#if TODAY_EXTENSION
#define MARGIN 50
#else
#define MARGIN 20
#endif
#define VERTICAL_MARGIN 10
#define INNER_MARGIN 5
#define URL_MARGIN 2

#define TITLE_MAX_HEIGHT 39
#define URL_MAX_HEIGHT 17
#define DESC_MAX_HEIGHT 51

typedef struct {
  CGRect titleRect;
  CGRect urlRect;
  CGRect descriptionRect;
} CellDimension;

@implementation PWBeaconCell {
  UILabel *_titleLabel;
  UILabel *_urlLabel;
  UILabel *_descriptionLabel;
  UIImageView *_faviconView;
  PWSignalStrengthView *_strengthView;
  PWBeacon *_beacon;
}

@synthesize beacon = _beacon;

- (instancetype)initWithStyle:(UITableViewCellStyle)style
              reuseIdentifier:(NSString *)reuseIdentifier {
  self = [super initWithStyle:style reuseIdentifier:reuseIdentifier];

  _strengthView = [[PWSignalStrengthView alloc] initWithFrame:CGRectZero];
  [_strengthView setHidden:YES];
  [self addSubview:_strengthView];
  _titleLabel = [[UILabel alloc] initWithFrame:CGRectZero];
  [self addSubview:_titleLabel];
  _urlLabel = [[UILabel alloc] initWithFrame:CGRectZero];
  [self addSubview:_urlLabel];
  _descriptionLabel = [[UILabel alloc] initWithFrame:CGRectZero];
  [self addSubview:_descriptionLabel];
  _faviconView = [[UIImageView alloc] initWithFrame:CGRectZero];
  [_faviconView setContentMode:UIViewContentModeScaleAspectFit];
  [self addSubview:_faviconView];

  [_titleLabel setFont:TITLE_FONT];
  [_titleLabel setTextColor:TITLE_COLOR];
  [_titleLabel setNumberOfLines:2];
  [_urlLabel setFont:URL_FONT];
  [_urlLabel setTextColor:URL_COLOR];
  [_descriptionLabel setFont:DESC_FONT];
  [_descriptionLabel setTextColor:DESC_COLOR];
  [_descriptionLabel setNumberOfLines:3];

  [_titleLabel setLineBreakMode:NSLineBreakByTruncatingTail];
  [_urlLabel setLineBreakMode:NSLineBreakByTruncatingTail];
  [_descriptionLabel setLineBreakMode:NSLineBreakByTruncatingTail];
#if TODAY_EXTENSION
  [_faviconView setHidden:YES];
  [_descriptionLabel setHidden:YES];

  UIView *selectedBackgroundView = [[UIView alloc] initWithFrame:CGRectZero];
  [selectedBackgroundView
      setBackgroundColor:[UIColor colorWithWhite:1.0 alpha:0.07]];
  [self setSelectedBackgroundView:selectedBackgroundView];

#endif

  return self;
}

- (void)prepareForReuse {
  [_faviconView setImage:nil];
}

- (void)layoutSubviews {
#if TODAY_EXTENSION
  [[self selectedBackgroundView] setFrame:[self bounds]];
#endif

  CellDimension dimension;
  computeLayout(_beacon, [self bounds].size.width, &dimension);
#if !TODAY_EXTENSION
  if ([_beacon iconURL] != nil) {
    [_faviconView setHidden:NO];
  } else {
    if (FAVICON_WHITESPACE) {
      [_faviconView setHidden:NO];
    } else {
      [_faviconView setHidden:YES];
    }
  }
#endif

  [_titleLabel setFrame:dimension.titleRect];
  [_urlLabel setFrame:dimension.urlRect];
  [_descriptionLabel setFrame:dimension.descriptionRect];

  CGRect frame;
  CGRect bounds = [self bounds];
  frame = CGRectMake(bounds.size.width - MARGIN - 15,
                     VERTICAL_MARGIN + FAVICON_SIZE + 10, 15, 15);
  [_strengthView setFrame:frame];
  bounds = [self bounds];
  frame = CGRectMake(bounds.size.width - MARGIN - FAVICON_SIZE, VERTICAL_MARGIN,
                     FAVICON_SIZE, FAVICON_SIZE);
  [_faviconView setFrame:frame];
}

- (void)setBeacon:(PWBeacon *)beacon {
  _beacon = beacon;

  [_titleLabel setText:[[self beacon] title]];
  [_urlLabel setText:[[[self beacon] displayURL] absoluteString]];
  [_descriptionLabel setText:snippetForBeacon([self beacon])];
#if !TODAY_EXTENSION
  if ([[self beacon] iconURL] == nil) {
    [_faviconView setImage:nil];
  } else {
    [_faviconView sd_setImageWithURL:[[self beacon] iconURL]];
  }
#endif
  [_strengthView setHidden:![[NSUserDefaults standardUserDefaults]
                               boolForKey:@"DebugMode"]];

  NSInteger rssi = [[[self beacon] uriBeacon] RSSI];
  if (rssi == 127) {
    rssi = -100;
  }
  if (rssi < -100) {
    rssi = -100;
  }
  if (rssi > -50) {
    rssi = -50;
  }
  // Compute quality in percent based on RSSI value.
  NSInteger quality = 2 * (rssi + 100);
  [_strengthView setQuality:quality];

  [self setNeedsLayout];
}

#define FREE_SPACE_PATH_LOSS_CONSTANT_FOR_BLE 41

static double distanceFromRSSI(int txPower, double rssi) {
  int pathLoss = txPower - rssi;
  return pow(10.0, (pathLoss - FREE_SPACE_PATH_LOSS_CONSTANT_FOR_BLE) / 20.0);
}

static NSString *snippetForBeacon(PWBeacon *beacon) {
  if ([[NSUserDefaults standardUserDefaults] boolForKey:@"DebugMode"]) {
    return [NSString
        stringWithFormat:
            @"[discovery:%g request:%g rssi:%i tx:%i dist:%.2g rank: %.2g] %@",
            [beacon discoveryDelay], [beacon requestDelay],
            (int)[[beacon uriBeacon] RSSI],
            (int)[[beacon uriBeacon] txPowerLevel],
            distanceFromRSSI([[beacon uriBeacon] txPowerLevel],
                             [[beacon uriBeacon] RSSI]),
            [beacon rank], [beacon snippet] != nil ? [beacon snippet] : @""];
  } else {
    return [beacon snippet];
  }
}

static void computeLayout(PWBeacon *beacon, CGFloat containerWidth,
                          CellDimension *result) {
  CGRect titleRect;
  CGRect urlRect;
  CGRect descriptionRect;

  NSMutableDictionary *attr = [[NSMutableDictionary alloc] init];
  CGSize size;

  [attr setObject:TITLE_FONT forKey:NSFontAttributeName];
  size = CGSizeMake(containerWidth - (FAVICON_SIZE + MARGIN + MARGIN),
                    TITLE_MAX_HEIGHT);
  titleRect =
      [[beacon title] boundingRectWithSize:size
                                   options:NSStringDrawingUsesLineFragmentOrigin
                                attributes:attr
                                   context:nil];
  titleRect.origin = CGPointMake(MARGIN, VERTICAL_MARGIN);
  titleRect = CGRectIntegral(titleRect);

  urlRect.origin = CGPointMake(MARGIN, CGRectGetMaxY(titleRect) + INNER_MARGIN);
  urlRect.size = CGSizeMake(containerWidth - (FAVICON_SIZE + MARGIN + MARGIN),
                            URL_MAX_HEIGHT);

  [attr setObject:DESC_FONT forKey:NSFontAttributeName];
  size = CGSizeMake(containerWidth - (MARGIN + MARGIN), DESC_MAX_HEIGHT);
  NSString *snippet = snippetForBeacon(beacon);
  if (snippet == nil) {
    descriptionRect.size = CGSizeZero;
    descriptionRect.origin = CGPointMake(MARGIN, CGRectGetMaxY(urlRect));
  } else {
    descriptionRect =
        [snippet boundingRectWithSize:size
                              options:NSStringDrawingUsesLineFragmentOrigin
                           attributes:attr
                              context:nil];
    descriptionRect.origin =
        CGPointMake(MARGIN, CGRectGetMaxY(urlRect) + INNER_MARGIN);
    descriptionRect = CGRectIntegral(descriptionRect);
  }

  result->titleRect = titleRect;
  result->urlRect = urlRect;
  result->descriptionRect = descriptionRect;
}

+ (CGFloat)heightForDevice:(PWBeacon *)device
                 tableView:(UITableView *)tableView {
  CellDimension dimension;
  computeLayout(device, [tableView bounds].size.width, &dimension);
  return CGRectGetMaxY(dimension.descriptionRect) + VERTICAL_MARGIN;
}

@end
