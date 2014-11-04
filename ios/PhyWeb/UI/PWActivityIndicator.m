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

#import "PWActivityIndicator.h"

@implementation PWActivityIndicator {
  UIImageView *_defaultView;
  UIImageView *_progress1View;
  UIImageView *_progress2View;
  UIImageView *_progress3View;
  int _currentImage;
  BOOL _started;
}

- (id)initWithFrame:(CGRect)frame {
  self = [super initWithFrame:frame];
  _currentImage = 0;
  _defaultView = [[UIImageView alloc]
      initWithImage:[UIImage imageNamed:@"LaunchIcon.png"]];
  [self addSubview:_defaultView];
  _progress1View =
      [[UIImageView alloc] initWithImage:[UIImage imageNamed:@"Progress1.png"]];
  [self addSubview:_progress1View];
  _progress2View =
      [[UIImageView alloc] initWithImage:[UIImage imageNamed:@"Progress2.png"]];
  [self addSubview:_progress2View];
  _progress3View =
      [[UIImageView alloc] initWithImage:[UIImage imageNamed:@"Progress3.png"]];
  [self addSubview:_progress3View];
  return self;
}

- (void)layoutSubviews {
  [_defaultView setFrame:[self bounds]];
  [_progress1View setFrame:[self bounds]];
  [_progress2View setFrame:[self bounds]];
  [_progress3View setFrame:[self bounds]];
}

- (void)start {
  if (_started) {
    return;
  }
  _started = YES;
  [self _showNextImage];
}

- (void)_showNextImage {
  _currentImage++;
  _currentImage %= 4;

  CGFloat alphaDefault = 0;
  CGFloat alpha1 = 0;
  CGFloat alpha2 = 0;
  CGFloat alpha3 = 0;

  switch (_currentImage) {
    case 0:
      alphaDefault = 1;
      break;
    case 1:
      alpha1 = 1;
      break;
    case 2:
      alpha2 = 1;
      break;
    case 3:
      alpha3 = 1;
      break;
  }

  [UIView animateWithDuration:0.25
                   animations:^{
                       [_defaultView setAlpha:alphaDefault];
                       [_progress1View setAlpha:alpha1];
                       [_progress2View setAlpha:alpha2];
                       [_progress3View setAlpha:alpha3];
                   }];
  [self performSelector:@selector(_showNextImage)
             withObject:nil
             afterDelay:1.5];
}

- (void)stop {
  _started = NO;
  [NSObject cancelPreviousPerformRequestsWithTarget:self];
  [UIView animateWithDuration:0.25
                   animations:^{
                       [_defaultView setAlpha:1.0];
                       [_progress1View setAlpha:0.0];
                       [_progress2View setAlpha:0.0];
                       [_progress3View setAlpha:0.0];
                   }];
}

@end
