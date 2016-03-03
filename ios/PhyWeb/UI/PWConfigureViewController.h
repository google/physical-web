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

#import <UIKit/UIKit.h>

#import "UriBeacon.h"

// This part of the UI will show a configuration panel for a configurable
// beacon. It will let the user set a URL for it.
@interface PWConfigureViewController : UIViewController

// Beacon to configure.
@property(nonatomic, retain) UBConfigurableUriBeacon *beacon;

@end
