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

#import "AppDelegate.h"

#import "PWBeaconsViewController.h"
#import "PWBeaconManager.h"

@interface AppDelegate ()

@end

@implementation AppDelegate {
  PWBeaconsViewController *_mainViewController;
}

- (void)applicationDidBecomeActive:(UIApplication *)application {
  if ([[PWBeaconManager sharedManager] isStarted]) {
    [[PWBeaconManager sharedManager] stop];
  }
  [_mainViewController disablePlaceholder];
  [[PWBeaconManager sharedManager] resetBeacons];
  [[PWBeaconManager sharedManager] start];
  [_mainViewController updateBeaconsNow];
}

- (BOOL)application:(UIApplication *)application
    didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
  self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
  self.window.backgroundColor = [UIColor whiteColor];
  [self.window makeKeyAndVisible];

  _mainViewController = [[PWBeaconsViewController alloc] init];
  [[self window] setRootViewController:_mainViewController];

  return YES;
}

@end
