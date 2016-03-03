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

#import "TodayViewController.h"
#import <NotificationCenter/NotificationCenter.h>
#import <CoreBluetooth/CoreBluetooth.h>
#import "PWBeaconManager.h"
#import "PWBeacon.h"
#import "PWBeaconCell.h"

#define ROW_HEIGHT 70

#define DEBUG_TODAY 0

@interface TodayViewController () <NCWidgetProviding, UITableViewDataSource,
                                   UITableViewDelegate,
                                   CBCentralManagerDelegate>

@end

@implementation TodayViewController {
  NSMutableArray *_beacons;
  UITableView *_tableView;
  void (^_completionHandler)(NCUpdateResult);
  BOOL _scheduledUpdated;
  BOOL _firstUpdate;
  CBCentralManager *_centralManager;
  UILabel *_label;
  NSDate *_updateDate;
  BOOL _requestedOnce;
}

- (id)initWithNibName:(NSString *)nibNameOrNil
               bundle:(NSBundle *)nibBundleOrNil {
  self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
  [[PWBeaconManager sharedManager]
      registerChangeBlock:^{ [self _updateViewAfterDelay]; }];
  _centralManager =
      [[CBCentralManager alloc] initWithDelegate:self queue:nil options:nil];
  return self;
}

- (void)centralManagerDidUpdateState:(CBCentralManager *)central {
  [self _updateBluetoothState];
}

- (void)_updateBluetoothState {
  NSString *stateText = @"(none)";
  switch ([_centralManager state]) {
    case CBCentralManagerStateUnknown:
      stateText = @"Unknown";
      break;
    case CBCentralManagerStateResetting:
      stateText = @"Resetting";
      break;
    case CBCentralManagerStateUnsupported:
      stateText = @"Unsupported";
      break;
    case CBCentralManagerStateUnauthorized:
      stateText = @"Unauthorized";
      break;
    case CBCentralManagerStatePoweredOff:
      stateText = @"Powered off";
      break;
    case CBCentralManagerStatePoweredOn:
      stateText = @"Powered on";
      break;
  }

  NSString *text =
      [NSString stringWithFormat:@"%@, %@", stateText, _updateDate];
  [_label setText:text];
  [_label sizeToFit];
}

- (void)viewDidLoad {
  [super viewDidLoad];

  CGRect bounds = [[self view] bounds];
  _tableView =
      [[UITableView alloc] initWithFrame:bounds style:UITableViewStylePlain];
  [_tableView setDataSource:self];
  [_tableView setDelegate:self];
  [_tableView setAutoresizingMask:UIViewAutoresizingFlexibleHeight |
                                  UIViewAutoresizingFlexibleWidth];
  [_tableView setRowHeight:ROW_HEIGHT];
  [[self view] addSubview:_tableView];
  [_tableView setSeparatorStyle:UITableViewCellSeparatorStyleNone];
  _label = [[UILabel alloc] initWithFrame:CGRectMake(0, 0, 100, 30)];
  [_label setBackgroundColor:[UIColor redColor]];
#if !DEBUG_TODAY
  [_label setHidden:YES];
#endif
  [[self view] addSubview:_label];

  [self _updateBluetoothState];
}

- (void)_updateViewAfterDelay {
  if (_scheduledUpdated) {
    return;
  }

  _updateDate = [NSDate date];
  [self _updateBluetoothState];

  _scheduledUpdated = YES;
  // The first update will be scheduled after 1 sec, the subsequent updates will
  // be scheduled after 5 sec. If the list was empty, an update will also be
  // scheduled after 1 sec.
  NSTimeInterval delay = 1;
  if (_firstUpdate || [_beacons count] == 0) {
    delay = 1;
  }
  [self performSelector:@selector(_updateBeaconsNow)
             withObject:nil
             afterDelay:delay];
  _firstUpdate = NO;
}

- (void)_updateBeaconsNow {
  [NSObject cancelPreviousPerformRequestsWithTarget:self
                                           selector:@selector(_updateBeaconsNow)
                                             object:nil];
  _scheduledUpdated = NO;
  _beacons = [[[PWBeaconManager sharedManager] beacons] mutableCopy];
  [self _sort];
  [self _reloadData];

  [self _saveBeacons];
}

// Sort results by RSSI value.
- (void)_sort {
  [_beacons sortUsingComparator:^NSComparisonResult(id obj1, id obj2) {
      PWBeacon *beacon1 = obj1;
      PWBeacon *beacon2 = obj2;
      NSInteger regionDifference = (NSInteger)[[beacon1 uriBeacon] region] -
                                   (NSInteger)[[beacon2 uriBeacon] region];
      if (regionDifference > 0) {
        return NSOrderedDescending;
      } else if (regionDifference < 0) {
        return NSOrderedAscending;
      } else {
        return [[beacon1 title] caseInsensitiveCompare:[beacon2 title]];
      }
  }];
}

- (void)_reloadData {
  [_tableView reloadData];
  CGSize size = [_tableView contentSize];
  CGRect frame = [_tableView frame];
  frame.size.height = size.height;
  frame.origin = CGPointZero;
  [_tableView setFrame:frame];
  self.preferredContentSize = size;
}

- (UIEdgeInsets)widgetMarginInsetsForProposedMarginInsets:
                    (UIEdgeInsets)defaultMarginInsets {
  return UIEdgeInsetsMake(0, 0, 30, 0);
}

- (void)widgetPerformUpdateWithCompletionHandler:
            (void (^)(NCUpdateResult))completionHandler {
  [self performSelector:@selector(_loadingDone) withObject:nil afterDelay:2];
  if ([[PWBeaconManager sharedManager] isStarted]) {
    [[PWBeaconManager sharedManager] stop];
  }

  _updateDate = [NSDate date];
  [self _updateBluetoothState];

  _completionHandler = completionHandler;

  _firstUpdate = YES;
  _requestedOnce = NO;
  [[PWBeaconManager sharedManager] resetBeacons];
  [[PWBeaconManager sharedManager] start];
  [self _updateViewAfterDelay];

  [self _loadBeacons];
  [self _reloadData];

  _completionHandler(NCUpdateResultNewData);
}

- (void)_loadingDone {
  _requestedOnce = YES;
  [self _reloadData];
}

- (NSInteger)tableView:(UITableView *)tableView
    numberOfRowsInSection:(NSInteger)section {
  if ([_beacons count] == 0) {
    return 1;
  } else if ([_beacons count] > 3) {
    return 4;
  } else {
    return [_beacons count];
  }
}

- (UITableViewCell *)tableView:(UITableView *)tableView
         cellForRowAtIndexPath:(NSIndexPath *)indexPath {
  if ([_beacons count] == 0) {
    UITableViewCell *cell =
        [tableView dequeueReusableCellWithIdentifier:@"no-beacons"];
    if (cell == nil) {
      cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault
                                    reuseIdentifier:@"no-beacons"];
      UILabel *label = [[UILabel alloc]
          initWithFrame:CGRectMake(50, 0, [_tableView frame].size.width - 50,
                                   30)];
      [label setTag:150];
      [cell addSubview:label];
      [label setTextColor:[UIColor colorWithWhite:0.5 alpha:1.0]];
      [label setFont:[UIFont systemFontOfSize:14]];
      UIView *selectedBackgroundView = [[UIView alloc]
          initWithFrame:CGRectMake(0, 0, [_tableView frame].size.width, 30)];
      [selectedBackgroundView
          setBackgroundColor:[UIColor colorWithWhite:1.0 alpha:0.07]];
      [cell setSelectionStyle:UITableViewCellSelectionStyleNone];
      [cell setSelectedBackgroundView:selectedBackgroundView];
    }
    UILabel *label = (UILabel *) [cell viewWithTag:150];
    NSString *noBeaconText = nil;
    if (_requestedOnce) {
      noBeaconText = [NSString stringWithFormat:@"No beacon nearby"];
    } else {
      noBeaconText = [NSString stringWithFormat:@"Scanning..."];
    }
    [label setText:noBeaconText];
    return cell;
  } else if ([indexPath row] == 3) {
    UITableViewCell *cell =
        [tableView dequeueReusableCellWithIdentifier:@"more"];
    if (cell == nil) {
      cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault
                                    reuseIdentifier:@"more"];
      UILabel *label = [[UILabel alloc]
          initWithFrame:CGRectMake(50, 0, [_tableView frame].size.width - 50,
                                   30)];
      [cell addSubview:label];
      [label setTextColor:[UIColor colorWithWhite:0.5 alpha:1.0]];
      NSString *moreText =
          [NSString stringWithFormat:@"%i more beacons nearby...",
                                     (int)[_beacons count] - 3];
      [label setText:moreText];
      [label setFont:[UIFont systemFontOfSize:14]];
      UIView *selectedBackgroundView = [[UIView alloc]
          initWithFrame:CGRectMake(0, 0, [_tableView frame].size.width, 30)];
      [selectedBackgroundView
          setBackgroundColor:[UIColor colorWithWhite:1.0 alpha:0.07]];
      [cell setSelectionStyle:UITableViewCellSelectionStyleNone];
      [cell setSelectedBackgroundView:selectedBackgroundView];
    }
    return cell;
  } else {
    PWBeaconCell *cell =
        [tableView dequeueReusableCellWithIdentifier:@"device"];
    if (cell == nil) {
      cell = [[PWBeaconCell alloc] initWithStyle:UITableViewCellStyleSubtitle
                                 reuseIdentifier:@"device"];
    }
    PWBeacon *beacon = [_beacons objectAtIndex:[indexPath row]];
    [cell setBeacon:beacon];
    return cell;
  }
}

- (void)tableView:(UITableView *)tableView
    didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
  [_tableView deselectRowAtIndexPath:indexPath animated:YES];
  if ([_beacons count] == 0) {
    return;
  }
  if ([indexPath row] == 3) {
    [[self extensionContext] openURL:[NSURL URLWithString:@"x-physweb:"]
                   completionHandler:nil];
  } else {
    PWBeacon *beacon = [_beacons objectAtIndex:[indexPath row]];
    [[self extensionContext] openURL:[beacon URL] completionHandler:nil];
  }
}

- (CGFloat)tableView:(UITableView *)tableView
    heightForRowAtIndexPath:(NSIndexPath *)indexPath {
  if ([_beacons count] == 0) {
    return 30;
  } else if ([indexPath row] == 3) {
    return 30;
  } else {
    return ROW_HEIGHT;
  }
}

- (void)_saveBeacons {
  [[PWBeaconManager sharedManager] serializeBeacons:_beacons];
}

- (void)_loadBeacons {
  _beacons =
      [[[PWBeaconManager sharedManager] unserializedBeacons] mutableCopy];
}

@end
