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

#import "PWSimpleWebViewController.h"

#import <WebKit/WebKit.h>

@interface PWSimpleWebViewController () <WKNavigationDelegate>

@end

@implementation PWSimpleWebViewController {
  WKWebView *_webView;
  NSURL *_url;
  NSString *_title;
  NSString *_htmlString;
}

@synthesize title = _title;

- (instancetype)initWithURL:(NSURL *)url {
  self = [super initWithNibName:nil bundle:nil];
  _url = url;
  return self;
}

- (instancetype)initWithHTMLString:(NSString *)htmlString {
  self = [super initWithNibName:nil bundle:nil];
  _htmlString = htmlString;
  return self;
}

- (void)viewDidLoad {
  [super viewDidLoad];

  CGRect frame = [[self view] bounds];
  [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:YES];
  _webView = [[WKWebView alloc] initWithFrame:frame];
  [_webView setNavigationDelegate:self];
  if (_url != nil) {
    [_webView loadRequest:[NSURLRequest requestWithURL:_url]];
  } else {
    [_webView loadHTMLString:_htmlString baseURL:nil];
  }
  [[self view] addSubview:_webView];

  if ([self proceedButtonVisible]) {
    UIBarButtonItem *button =
        [[UIBarButtonItem alloc] initWithTitle:@"Proceed"
                                         style:UIBarButtonItemStyleDone
                                        target:self
                                        action:@selector(_proceed:)];
    [[self navigationItem] setRightBarButtonItem:button];
  }
}

- (void)viewDidDisappear:(BOOL)animated {
  [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
}

- (void)webView:(WKWebView *)webView
    didFailNavigation:(WKNavigation *)navigation
            withError:(NSError *)error {
  [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
}

- (void)webView:(WKWebView *)webView
    didFinishNavigation:(WKNavigation *)navigation {
  [[UIApplication sharedApplication] setNetworkActivityIndicatorVisible:NO];
}

- (void)_proceed:(id)sender {
  if ([[self delegate]
          respondsToSelector:@selector(
                                 simpleWebViewControllerProceedPressed:)]) {
    [[self delegate] simpleWebViewControllerProceedPressed:self];
  }
}

@end
