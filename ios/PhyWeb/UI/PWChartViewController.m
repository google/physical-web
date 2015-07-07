//
//  PWBeaconDetailViewController.m
//  PhyWeb
//
//  Created by Hoa Dinh on 6/24/15.
//  Copyright © 2015 Hoa Dinh. All rights reserved.
//

#import "PWChartViewController.h"

#import "JBLineChartView.h"
#import "PWBeacon.h"
#import "PWBeaconManager.h"
#import "PWBeaconChartTableViewCell.h"

@interface PWChartViewController ()<JBLineChartViewDataSource,
                                    JBLineChartViewDelegate,
                                    UITableViewDataSource, UITableViewDelegate>

@end

@implementation PWChartViewController {
  JBLineChartView *_chartView;
  int _beaconsCount;
  int _maxTime;
  CGFloat **_rssiValues;
  CGFloat **_distanceValues;
  UITableView *_tableView;
  NSInteger _selectedHorizontalIndex;
  NSMutableArray *_urls;
  BOOL _showRSSI;
  NSURL *_url;
}

@synthesize URL = _url;

- (instancetype)initWithNibName:(NSString *)nibNameOrNil
                         bundle:(NSBundle *)nibBundleOrNil {
  self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];

  UIBarButtonItem *doneButton =
      [[UIBarButtonItem alloc] initWithTitle:@"Done"
                                       style:UIBarButtonItemStylePlain
                                      target:self
                                      action:@selector(_done:)];
  [[self navigationItem] setLeftBarButtonItem:doneButton];
  UIBarButtonItem *toggleButton =
      [[UIBarButtonItem alloc] initWithTitle:@"Toggle"
                                       style:UIBarButtonItemStylePlain
                                      target:self
                                      action:@selector(_toggle:)];
  [[self navigationItem] setRightBarButtonItem:toggleButton];
  [self setTitle:@"Distance"];
  _selectedHorizontalIndex = -1;

  return self;
}

- (void)_done:(id)sender {
  [self dismissViewControllerAnimated:YES completion:nil];
}

- (void)_toggle:(id)sender {
  _showRSSI = !_showRSSI;
  if (_showRSSI) {
    [_chartView setMinimumValue:128];
    [_chartView setMaximumValue:256];
    [self setTitle:@"RSSI"];
  } else {
    [_chartView setMinimumValue:0.0];
    [_chartView setMaximumValue:30.0];
    [self setTitle:@"Distance"];
  }
  [_chartView reloadData];
  [_tableView reloadData];
}

#define FREE_SPACE_PATH_LOSS_CONSTANT_FOR_BLE 41

static double distanceFromRSSI(int txPower, double rssi) {
  int pathLoss = txPower - rssi;
  return pow(10.0, (pathLoss - FREE_SPACE_PATH_LOSS_CONSTANT_FOR_BLE) / 20.0);
}

- (void)viewDidLoad {
  [super viewDidLoad];

  CGRect frame = [[self view] bounds];
  frame.size.height = frame.size.height / 2;
  _chartView = [[JBLineChartView alloc] initWithFrame:frame];
  [_chartView setMinimumValue:0.0];
  [_chartView setMaximumValue:30.0];
  [_chartView setDelegate:self];
  [_chartView setDataSource:self];
  [_chartView setBackgroundColor:[UIColor colorWithWhite:0.9 alpha:1.0]];
  [_chartView setAutoresizingMask:UIViewAutoresizingFlexibleHeight |
                                  UIViewAutoresizingFlexibleWidth];
  [_chartView setShowsLineSelection:NO];
  [[self view] addSubview:_chartView];

  UIPanGestureRecognizer *panGesture = [[UIPanGestureRecognizer alloc]
      initWithTarget:self
              action:@selector(_panGestureRecognized:)];
  [panGesture setCancelsTouchesInView:NO];
  [_chartView addGestureRecognizer:panGesture];

  frame = [[self view] bounds];
  frame.origin.y = frame.size.height / 2;
  frame.size.height = frame.size.height / 2;
  _tableView = [[UITableView alloc] initWithFrame:frame];
  [_tableView setDataSource:self];
  [_tableView setDelegate:self];
  [_tableView setAutoresizingMask:UIViewAutoresizingFlexibleTopMargin |
                                  UIViewAutoresizingFlexibleHeight |
                                  UIViewAutoresizingFlexibleWidth];
  [[self view] addSubview:_tableView];

  [self _periodicReloadData];
}

- (void)_panGestureRecognized:(UIPanGestureRecognizer *)gestureRecognizer {
  [NSObject
      cancelPreviousPerformRequestsWithTarget:self
                                     selector:@selector(_periodicReloadData)
                                       object:nil];
  if (gestureRecognizer.state == UIGestureRecognizerStateEnded) {
    [self _periodicReloadData];
  }
}

- (void)_periodicReloadData {
  [self _reloadData];
  [self performSelector:@selector(_periodicReloadData)
             withObject:nil
             afterDelay:1.0];
}

- (void)_reloadData {
  _urls = [[NSMutableArray alloc] init];
  for (PWBeacon *beacon in [[PWBeaconManager sharedManager] beacons]) {
    [_urls addObject:[beacon displayURL]];
  }

  NSTimeInterval startTime = [[PWBeaconManager sharedManager] startTime];
  NSTimeInterval maxTime = startTime;
  for (PWBeacon *beacon in [[PWBeaconManager sharedManager] beacons]) {
    NSArray *lastValue = [[beacon rssiHistory] lastObject];
    if (lastValue == nil) {
      continue;
    }
    // NSNumber * nbValue = [lastValue objectAtIndex:0];
    NSNumber *nbTimestamp = [lastValue objectAtIndex:1];
    if ([nbTimestamp doubleValue] > maxTime) {
      maxTime = [nbTimestamp doubleValue];
    }
  }
  _beaconsCount = (int)[[[PWBeaconManager sharedManager] beacons] count] + 2;
  _maxTime = ceil(maxTime - startTime) + 1;
  _rssiValues = calloc(_beaconsCount, sizeof(CGFloat *));
  _distanceValues = calloc(_beaconsCount, sizeof(CGFloat *));
  for (int i = 0; i < _beaconsCount; i++) {
    _rssiValues[i] = calloc(_maxTime, sizeof(CGFloat));
    _distanceValues[i] = calloc(_maxTime, sizeof(CGFloat));
    for (int k = 0; k < _maxTime; k++) {
      _rssiValues[i][k] = NAN;
      _distanceValues[i][k] = NAN;
    }
  }

  for (int i = 0; i < _beaconsCount; i++) {
    if (i < 2) {
      continue;
    }
    PWBeacon *beacon =
        [[[PWBeaconManager sharedManager] beacons] objectAtIndex:i - 2];
    for (NSArray *value in [beacon rssiHistory]) {
      NSNumber *nbValue = [value objectAtIndex:0];
      NSNumber *nbTimestamp = [value objectAtIndex:1];
      int time = round([nbTimestamp doubleValue]) - startTime;
      _rssiValues[i][time] =
          [[beacon uriBeacon] txPowerLevel] - [nbValue intValue];
      _distanceValues[i][time] = distanceFromRSSI(
          [[beacon uriBeacon] txPowerLevel], [nbValue intValue]);
    }
  }
  for (int i = 0; i < _beaconsCount; i++) {
    CGFloat currentRssi = NAN;
    CGFloat currentDistance = NAN;
    if (i == 0) {
      currentDistance = 2;
      currentRssi = 37;
    } else if (i == 1) {
      currentDistance = 10;
      currentRssi = 61;
    }
    for (int k = 0; k < _maxTime; k++) {
      if (isnan(_rssiValues[i][k])) {
        _rssiValues[i][k] = currentRssi;
      } else {
        currentRssi = _rssiValues[i][k];
      }
      if (isnan(_distanceValues[i][k])) {
        _distanceValues[i][k] = currentDistance;
      } else {
        currentDistance = _distanceValues[i][k];
      }
    }
  }

  [_tableView reloadData];
  [_chartView reloadData];
}

- (NSUInteger)numberOfLinesInLineChartView:(JBLineChartView *)lineChartView {
  return _beaconsCount;
}

- (NSUInteger)lineChartView:(JBLineChartView *)lineChartView
    numberOfVerticalValuesAtLineIndex:(NSUInteger)lineIndex {
  return 20;
}

- (CGFloat)lineChartView:(JBLineChartView *)lineChartView
    verticalValueForHorizontalIndex:(NSUInteger)horizontalIndex
                        atLineIndex:(NSUInteger)lineIndex {
  if (_showRSSI) {
    if (horizontalIndex >= _maxTime) {
      return NAN;
    } else {
      if (_maxTime >= 20) {
        return _rssiValues[lineIndex][horizontalIndex + _maxTime - 20] + 128;
      } else {
        return _rssiValues[lineIndex][horizontalIndex] + 128;
      }
    }
  } else {
    if (horizontalIndex >= _maxTime) {
      return NAN;
    } else {
      if (_maxTime >= 20) {
        return _distanceValues[lineIndex][horizontalIndex + _maxTime - 20];
      } else {
        return _distanceValues[lineIndex][horizontalIndex];
      }
    }
  }
}

- (UIColor *)lineChartView:(JBLineChartView *)lineChartView
   colorForLineAtLineIndex:(NSUInteger)lineIndex {
  static NSArray *colors = nil;
  if (colors == nil) {
    colors = @[
      [UIColor brownColor],
      [UIColor blueColor],
      [UIColor redColor],
      [UIColor greenColor],
      [UIColor grayColor],
      [UIColor orangeColor],
      [UIColor purpleColor]
    ];
  }
  return colors[lineIndex % [colors count]];
}

- (CGFloat)lineChartView:(JBLineChartView *)lineChartView
 widthForLineAtLineIndex:(NSUInteger)lineIndex {
  if (lineIndex < 2) {
    return 1.0;
  }
  if ([_urls[lineIndex - 2] isEqual:_url]) {
    return 3.0;
  } else {
    return 1.0;
  }
}

- (JBLineChartViewLineStyle)lineChartView:(JBLineChartView *)lineChartView
              lineStyleForLineAtLineIndex:(NSUInteger)lineIndex {
  return lineIndex >= 2 ? JBLineChartViewLineStyleSolid
                        : JBLineChartViewLineStyleDashed;
}

- (void)lineChartView:(JBLineChartView *)lineChartView
 didSelectLineAtIndex:(NSUInteger)lineIndex
      horizontalIndex:(NSUInteger)horizontalIndex
           touchPoint:(CGPoint)touchPoint {
  if (horizontalIndex >= _maxTime) {
  } else {
    if (_maxTime >= 20) {
      _selectedHorizontalIndex = horizontalIndex + _maxTime - 20;
    } else {
      _selectedHorizontalIndex = horizontalIndex;
    }
    [_tableView reloadData];
  }
}

- (NSInteger)tableView:(UITableView *)tableView
 numberOfRowsInSection:(NSInteger)section {
  return _beaconsCount - 2;
}

- (UITableViewCell *)tableView:(UITableView *)tableView
         cellForRowAtIndexPath:(NSIndexPath *)indexPath {
  static NSArray *colors = nil;
  if (colors == nil) {
    colors = @[
      [UIColor brownColor],
      [UIColor blueColor],
      [UIColor redColor],
      [UIColor greenColor],
      [UIColor grayColor],
      [UIColor orangeColor],
      [UIColor purpleColor]
    ];
  }

  NSInteger row = [indexPath row];
  PWBeaconChartTableViewCell *cell = (PWBeaconChartTableViewCell *)
      [_tableView dequeueReusableCellWithIdentifier:@"Beacon"];
  if (cell == nil) {
    cell = [[PWBeaconChartTableViewCell alloc]
          initWithStyle:UITableViewCellStyleDefault
        reuseIdentifier:@"Beacon"];
  }
  [[cell textLabel]
      setText:[NSString stringWithFormat:@"%@", [_urls objectAtIndex:row]]];
  if (_selectedHorizontalIndex != -1) {
    if (_showRSSI) {
      [[cell rssiLabel]
          setText:[NSString
                      stringWithFormat:@"%5.2g",
                                       _rssiValues[row + 2]
                                                  [_selectedHorizontalIndex]]];
    } else {
      [[cell rssiLabel]
          setText:[NSString
                      stringWithFormat:
                          @"%5.2g",
                          _distanceValues[row + 2][_selectedHorizontalIndex]]];
    }
  } else {
    [[cell rssiLabel] setText:@""];
  }
  [[cell textLabel] setTextColor:colors[(row + 2) % [colors count]]];

  return cell;
}

@end
