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

#import "PWConfigureViewController.h"

#import <UriBeacon/UriBeacon.h>
#import <MBProgressHUD/MBProgressHUD.h>

#import "PWURLShortener.h"

#define DEFAULT_TX_POWER_LEVEL -22

@interface PWConfigureViewController ()<UITextFieldDelegate>

@end

@implementation PWConfigureViewController {
  UITextField *_textField;
  BOOL _connected;
  MBProgressHUD *_hud;
  NSTimeInterval _delayStartTime;
  UILabel *_shortenerLabel;
  NSURL *_resultURL;
}

- (void)viewDidLoad {
  [super viewDidLoad];

  [[self view] setBackgroundColor:[UIColor whiteColor]];

  CGRect bounds = [[self view] bounds];
  UILabel *label = [[UILabel alloc]
      initWithFrame:CGRectMake(10, 74, bounds.size.width - 20, 0)];
  [label setText:@"Enter URI:"];
  [label sizeToFit];
  [[self view] addSubview:label];

  _textField = [[UITextField alloc]
      initWithFrame:CGRectMake(10, CGRectGetMaxY([label frame]) + 10,
                               bounds.size.width - 20, 30)];
  [_textField setPlaceholder:@"https://www.google.com"];
  [_textField setDelegate:self];
  [_textField setKeyboardType:UIKeyboardTypeURL];
  [_textField setAutocapitalizationType:UITextAutocapitalizationTypeNone];
  [_textField setAutocorrectionType:UITextAutocorrectionTypeNo];
  [_textField setReturnKeyType:UIReturnKeyDone];
  [_textField setClearButtonMode:UITextFieldViewModeWhileEditing];
  [_textField setBorderStyle:UITextBorderStyleRoundedRect];
  [_textField addTarget:self
                 action:@selector(_textChanged:)
       forControlEvents:UIControlEventEditingChanged];
  [[self view] addSubview:_textField];

  _shortenerLabel = [[UILabel alloc]
      initWithFrame:CGRectMake(10, CGRectGetMaxY([_textField frame]) + 10,
                               bounds.size.width - 20, 40)];
  [_shortenerLabel setFont:[UIFont systemFontOfSize:12]];
  [_shortenerLabel setNumberOfLines:0];
  [[self view] addSubview:_shortenerLabel];
}

- (void)viewWillAppear:(BOOL)animated {
  [super viewWillAppear:animated];
  PWConfigureViewController *__weak weakSelf = self;
  [_textField setEnabled:NO];

  [[[self view] window] setUserInteractionEnabled:NO];
  _hud = [MBProgressHUD showHUDAddedTo:self.view animated:YES];
  [_hud setMode:MBProgressHUDModeIndeterminate];
  [_hud setLabelText:@"Reading"];

  _delayStartTime = [NSDate timeIntervalSinceReferenceDate];
  _resultURL = nil;

  // Read beacon content.
  [[self beacon] connect:^(NSError *error) {
      PWConfigureViewController *strongSelf = weakSelf;
      [[strongSelf beacon]
          readBeaconWithCompletionBlock:^(NSError *error, UBUriBeacon *beacon) {
              [strongSelf _connectedWithBeacon:beacon];
          }];
  }];
}

- (void)_connectedWithBeacon:(UBUriBeacon *)beacon {
  PWConfigureViewController *__weak weakSelf = self;
  [PWURLShortener
       expandURL:[beacon URI]
      completion:^(NSError *error, NSURL *resultURL) {
          PWConfigureViewController *strongSelf = weakSelf;

          _resultURL = resultURL;
          [strongSelf
              _performSelectorAfterWaitDelay:@selector(_readDoneWithError:)
                                      object:error];
      }];
}

- (void)_readDoneWithError:(NSError *)error {
  [[[self view] window] setUserInteractionEnabled:YES];
  [_hud hide:YES];
  _hud = nil;

  [_textField setText:[_resultURL absoluteString]];
  _connected = YES;
  [_textField setEnabled:YES];
  [_textField becomeFirstResponder];
}

- (void)viewDidAppear:(BOOL)animated {
  [super viewDidAppear:animated];
}

- (void)viewWillDisappear:(BOOL)animated {
  [_textField resignFirstResponder];
  [[self beacon] disconnect:^(NSError *error) { _connected = NO; }];
  [super viewWillDisappear:animated];
}

- (void)_textChanged:(id)sender {
  NSURL *url = nil;
  if ([[_textField text] length] > 0) {
    url = [NSURL URLWithString:[_textField text]];
  }
  if (url != nil) {
    UBUriBeacon *beacon =
        [[UBUriBeacon alloc] initWithURI:url txPowerLevel:DEFAULT_TX_POWER_LEVEL];
    if ([beacon isValid]) {
      [_shortenerLabel
          setTextColor:[UIColor colorWithRed:0.3 green:0.6 blue:0.3 alpha:1.0]];
      [_shortenerLabel setText:@"The URL is valid."];
    } else {
      [_shortenerLabel
          setTextColor:[UIColor colorWithRed:0.3 green:0.3 blue:1.0 alpha:1.0]];
      [_shortenerLabel
          setText:
              @"The URL is valid and will be shortened using goo.gl service."];
    }
  } else {
    [_shortenerLabel setText:nil];
  }
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
  NSURL *url = [NSURL URLWithString:[_textField text]];
  // Add http:// if there's no scheme.
  if ([[url scheme] length] == 0) {
    NSString *urlString =
        [@"http://" stringByAppendingString:[_textField text]];
    url = [NSURL URLWithString:urlString];
  }
  if (url == nil) {
    UIAlertView *alert = [[UIAlertView alloc]
            initWithTitle:@"The URL is not valid"
                  message:@"Please check you typed it properly."
                 delegate:nil
        cancelButtonTitle:@"OK"
        otherButtonTitles:nil];
    [alert show];
    return YES;
  }

  // Start writing process.
  _delayStartTime = [NSDate timeIntervalSinceReferenceDate];
  [_textField resignFirstResponder];
  [self _startProgress];

  UBUriBeacon *beaconData =
      [[UBUriBeacon alloc] initWithURI:url txPowerLevel:DEFAULT_TX_POWER_LEVEL];
  if (![beaconData isValid]) {
    [PWURLShortener shortenURL:url
                    completion:^(NSError *error, NSURL *resultURL) {
                        [self _validateWithURL:resultURL];
                    }];
  } else {
    [self _validateWithURL:url];
  }

  return YES;
}

- (void)_validateWithURL:(NSURL *)url {
  UBUriBeacon *beaconData =
      [[UBUriBeacon alloc] initWithURI:url txPowerLevel:DEFAULT_TX_POWER_LEVEL];
  if (![beaconData isValid]) {
    [self _performSelectorAfterWaitDelay:@selector(_showLongURLError)
                                  object:nil];
    return;
  }

  [self _reallyWriteBeaconWithURL:url];
}

- (void)_performSelectorAfterWaitDelay:(SEL)selector object:(id)object {
  NSTimeInterval delay =
      [NSDate timeIntervalSinceReferenceDate] - _delayStartTime;
  delay = 1.5 - delay;
  if (delay < 0) {
    delay = 0;
  }
  [self performSelector:selector withObject:object afterDelay:delay];
}

- (void)_showLongURLError {
  [self _stopProgress];
  UIAlertView *alert = [[UIAlertView alloc]
          initWithTitle:@"The URL is too long"
                message:@"You could use a URL shortener to help."
               delegate:nil
      cancelButtonTitle:@"OK"
      otherButtonTitles:nil];
  [alert show];
  [_textField becomeFirstResponder];
}

- (void)_startProgress {
  [[[self view] window] setUserInteractionEnabled:NO];

  _hud = [MBProgressHUD showHUDAddedTo:self.view animated:YES];
  [_hud setMode:MBProgressHUDModeIndeterminate];
  [_hud setLabelText:@"Writing"];
}

- (void)_stopProgress {
  [[[self view] window] setUserInteractionEnabled:YES];
  [_hud hide:YES];
  _hud = nil;
}

- (void)_showWriteDone {
  [[[self view] window] setUserInteractionEnabled:NO];
  _hud = [MBProgressHUD showHUDAddedTo:self.view animated:YES];
  _hud.customView = [[UIImageView alloc]
      initWithImage:[UIImage imageNamed:@"37x-Checkmark.png"]];
  _hud.mode = MBProgressHUDModeCustomView;
  _hud.labelText = @"Completed";
}

- (void)_hideWriteDone {
  [_hud hide:YES];
  _hud = nil;

  [self _dismissViewController];
  [[[self view] window] setUserInteractionEnabled:YES];
}

- (void)_reallyWriteBeaconWithURL:(NSURL *)url {
  UBUriBeacon *beaconData =
      [[UBUriBeacon alloc] initWithURI:url txPowerLevel:DEFAULT_TX_POWER_LEVEL];
  PWConfigureViewController *__weak weakSelf = self;
  [[self beacon]
          writeBeacon:beaconData
      completionBlock:^(NSError *error) {
          PWConfigureViewController *strongSelf = weakSelf;
          NSLog(@"write done");
          [strongSelf
              _performSelectorAfterWaitDelay:@selector(_writeDoneWithError:)
                                      object:error];
      }];
}

- (void)_writeDoneWithError:(NSError *)error {
  [self _stopProgress];

  if (error == nil) {
    [self _showWriteDone];
    [self performSelector:@selector(_hideWriteDone)
               withObject:nil
               afterDelay:1.5];
    return;
  }

  [_textField becomeFirstResponder];
  UIAlertView *alert = [[UIAlertView alloc]
          initWithTitle:@"An error occurred while writing the beacon"
                message:[error localizedDescription]
               delegate:nil
      cancelButtonTitle:@"OK"
      otherButtonTitles:nil];
  [alert show];
}

- (void)_dismissViewController {
  [[self navigationController] popViewControllerAnimated:YES];
}

@end
