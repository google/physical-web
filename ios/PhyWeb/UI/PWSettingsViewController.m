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

#import "PWSettingsViewController.h"

#import <CoreBluetooth/CoreBluetooth.h>
#import <CoreFoundation/CoreFoundation.h>
#import <MBProgressHUD/MBProgressHUD.h>

#import "PWBeaconManager.h"
#import "PWConfigureViewController.h"
#import "PWPlaceholderView.h"
#import "PWSimpleWebViewController.h"

@interface PWSettingsViewController () <
    CBCentralManagerDelegate, UITableViewDataSource, UITableViewDelegate>

@end

@implementation PWSettingsViewController {
  id _registeredBlock;
  PWPlaceholderView *_placeholderView;
  CBCentralManager *_centralManager;
  UILabel *_versionLabel;
}

- (id)initWithNibName:(NSString *)nibNameOrNil
               bundle:(NSBundle *)nibBundleOrNil {
  self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];

  UIBarButtonItem *cancelButton =
      [[UIBarButtonItem alloc] initWithTitle:@"Cancel"
                                       style:UIBarButtonItemStylePlain
                                      target:self
                                      action:@selector(_cancel:)];
  [[self navigationItem] setLeftBarButtonItem:cancelButton];

  _centralManager =
      [[CBCentralManager alloc] initWithDelegate:self queue:nil options:nil];

  return self;
}

- (void)viewDidLoad {
  [super viewDidLoad];

  CGRect bounds = [[self view] bounds];
  CGRect frame = bounds;
  frame.origin.y = 64;
  frame.size.height -= 64;
  _placeholderView = [[PWPlaceholderView alloc] initWithFrame:frame];
  [_placeholderView setAutoresizingMask:UIViewAutoresizingFlexibleHeight |
                                        UIViewAutoresizingFlexibleWidth];
  [_placeholderView setShowLabel:YES];
  UITapGestureRecognizer *doubleTapRecognizer = [[UITapGestureRecognizer alloc]
      initWithTarget:self
              action:@selector(_enableDebugMode)];
  [doubleTapRecognizer setNumberOfTapsRequired:2];
  [_placeholderView addGestureRecognizer:doubleTapRecognizer];
  [self _updateLayoutForSize:bounds.size];

  [[self view] addSubview:_placeholderView];
  [self _updatedPlaceholderViewState];

  frame = CGRectMake(0, bounds.size.height - 120, bounds.size.width, 90);
  UITableView *tableView =
      [[UITableView alloc] initWithFrame:frame style:UITableViewStylePlain];
  [tableView setBounces:NO];
  [tableView setSeparatorStyle:UITableViewCellSeparatorStyleNone];
  [tableView setAutoresizingMask:UIViewAutoresizingFlexibleWidth |
                                 UIViewAutoresizingFlexibleTopMargin];
  [tableView setDataSource:self];
  [tableView setDelegate:self];
  [[self view] addSubview:tableView];

  frame = bounds;
  frame.origin.x = 20;
  frame.origin.y = bounds.size.height - 30;
  frame.size.height = 20;
  frame.size.width = bounds.size.width - 30;
  _versionLabel = [[UILabel alloc] initWithFrame:frame];
  [_versionLabel setAutoresizingMask:UIViewAutoresizingFlexibleWidth |
                                     UIViewAutoresizingFlexibleTopMargin];
  [_versionLabel setFont:[UIFont boldSystemFontOfSize:14]];
  [_versionLabel setTextAlignment:NSTextAlignmentCenter];
  [_versionLabel setTextColor:[UIColor colorWithWhite:0.8 alpha:1.0]];
  NSString *version = [[[NSBundle mainBundle] infoDictionary]
      objectForKey:@"CFBundleShortVersionString"];
  NSString *buildNumber = [[[NSBundle mainBundle] infoDictionary]
      objectForKey:(id)kCFBundleVersionKey];

  [_versionLabel setText:[NSString stringWithFormat:@"version %@ build %@",
                                                    version, buildNumber]];
  [[self view] addSubview:_versionLabel];

  [[self view] setBackgroundColor:[UIColor whiteColor]];
}

- (void)viewWillTransitionToSize:(CGSize)size
       withTransitionCoordinator:
           (id<UIViewControllerTransitionCoordinator>)coordinator {
  [super viewWillTransitionToSize:size withTransitionCoordinator:coordinator];
  [self _updateLayoutForSize:size];
}

- (void)_updateLayoutForSize:(CGSize)size {
  CGRect frame = [_placeholderView frame];
  if (size.width > size.height) {
    frame.origin.y = 33;
    frame.size.height -= 33;
  } else {
    frame.origin.y = 64;
    frame.size.height -= 64;
  }
  [_placeholderView setFrame:frame];
}

- (void)viewDidDisappear:(BOOL)animated {
  [_placeholderView stop];
}

- (void)centralManagerDidUpdateState:(CBCentralManager *)central {
  [self _updatedPlaceholderViewState];
}

- (void)_enableDebugMode {
  BOOL debugMode =
      ![[NSUserDefaults standardUserDefaults] boolForKey:@"DebugMode"];
  [[NSUserDefaults standardUserDefaults] setBool:debugMode forKey:@"DebugMode"];

  MBProgressHUD *hud = [MBProgressHUD showHUDAddedTo:self.view animated:YES];
  [hud setMode:MBProgressHUDModeText];
  [hud setLabelText:debugMode ? @"Debug Mode Enabled" : @"Debug Mode Disabled"];
  [hud hide:YES afterDelay:1.5];
}

- (void)_updatedPlaceholderViewState {
  BOOL enabled = ([_centralManager state] == CBCentralManagerStatePoweredOn);
  [_placeholderView setBluetoothEnabled:enabled];
  if (enabled) {
    [_placeholderView
        setLabel:
            @"Put the beacon in configuration mode in order to set it up."];
  } else {
    [_placeholderView
        setLabel:@"Please turn on bluetooth in order to configure beacons."];
  }
}

- (void)viewDidAppear:(BOOL)animated {
  [super viewDidAppear:animated];
  PWSettingsViewController *__weak weakSelf = self;
  _registeredBlock =
      [[PWBeaconManager sharedManager] registerConfigurationChangeBlock:^{
        PWSettingsViewController *strongSelf = weakSelf;
        [strongSelf _openConfigurationView];
      }];
}

- (void)viewWillDisappear:(BOOL)animated {
  [super viewWillDisappear:animated];

  [[PWBeaconManager sharedManager]
      unregisterConfigurationChangeBlock:_registeredBlock];
  _registeredBlock = nil;
}

- (void)_openConfigurationView {
  // Trigger configuration panel if there's a one configurable beacon.
  if ([[[PWBeaconManager sharedManager] configurableBeacons] count] > 0) {
    [[PWBeaconManager sharedManager]
        unregisterConfigurationChangeBlock:_registeredBlock];
    _registeredBlock = nil;

    UBConfigurableUriBeacon *beacon =
        [[[PWBeaconManager sharedManager] configurableBeacons] objectAtIndex:0];
    PWConfigureViewController *configureViewController =
        [[PWConfigureViewController alloc] initWithNibName:nil bundle:nil];
    [configureViewController setBeacon:beacon];
    [[self navigationController] pushViewController:configureViewController
                                           animated:YES];
  }
}

- (void)_cancel:(id)sender {
  [self dismissViewControllerAnimated:YES completion:nil];
}

#pragma mark - Table view data source

- (NSInteger)tableView:(UITableView *)tableView
    numberOfRowsInSection:(NSInteger)section {
  return 2;
}

- (UITableViewCell *)tableView:(UITableView *)tableView
         cellForRowAtIndexPath:(NSIndexPath *)indexPath {
  UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:@"Cell"];
  if (cell == nil) {
    cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault
                                  reuseIdentifier:@"Cell"];
    [[cell textLabel] setTextColor:[UIColor colorWithWhite:0.4 alpha:1.0]];
    [cell setAccessoryType:UITableViewCellAccessoryDisclosureIndicator];
  }
  switch ([indexPath row]) {
    case 0:
      [[cell textLabel] setText:@"Getting Started"];
      break;
    case 1:
      [[cell textLabel] setText:@"Open Source Licenses"];
      break;
  }

  return cell;
}

- (void)tableView:(UITableView *)tableView
    didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
  [tableView deselectRowAtIndexPath:indexPath animated:YES];
  switch ([indexPath row]) {
    case 0: {
      PWSimpleWebViewController *controller = [[PWSimpleWebViewController alloc]
          initWithURL:[NSURL URLWithString:@"http://google.github.io/"
                             @"physical-web/mobile/ios/"
                             @"getting-started.html"]];
      [controller setTitle:@"Getting Started"];
      [[self navigationController] pushViewController:controller animated:YES];
      break;
    }
    case 1: {
      NSString *path =
          [[NSBundle mainBundle] pathForResource:@"licenses" ofType:@"html"];
      NSData *data = [NSData dataWithContentsOfFile:path];
      NSString *htmlString =
          [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
      PWSimpleWebViewController *controller =
          [[PWSimpleWebViewController alloc] initWithHTMLString:htmlString];
      [controller setTitle:@"Licenses"];
      [[self navigationController] pushViewController:controller animated:YES];
      break;
    }
  }
}

@end
