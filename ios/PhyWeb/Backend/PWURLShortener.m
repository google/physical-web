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

#import "PWURLShortener.h"

#import "PWMetadataRequest.h"

#define REDIRECT_TIMEOUT 10

@interface PWInternalURLExpander
    : NSObject<NSURLConnectionDelegate, NSURLConnectionDataDelegate>

@property(nonatomic, copy) NSURL *URL;

- (void)startWithCompletionBlock:(void (^)(NSError *error,
                                           NSURL *resultURL))block;

@end

@interface PWInternalURLShortener
    : NSObject<NSURLConnectionDelegate, NSURLConnectionDataDelegate>

@property(nonatomic, copy) NSURL *URL;

- (void)startWithCompletionBlock:(void (^)(NSError *error,
                                           NSURL *resultURL))block;

@end

@implementation PWURLShortener

+ (void)shortenURL:(NSURL *)URL
        completion:(void (^)(NSError *error, NSURL *resultURL))block {
  PWInternalURLShortener *shortener = [[PWInternalURLShortener alloc] init];
  [shortener setURL:URL];
  [shortener startWithCompletionBlock:block];
}

+ (void)expandURL:(NSURL *)URL
       completion:(void (^)(NSError *error, NSURL *resultURL))block {
  PWInternalURLExpander *expander = [[PWInternalURLExpander alloc] init];
  [expander setURL:URL];
  [expander startWithCompletionBlock:block];
}

@end

@implementation PWInternalURLShortener {
  void (^_completionBlock)(NSError *error, NSURL *resultURL);
  NSURLConnection *_connection;
  NSInteger _responseCode;
  NSMutableData *_responseData;
}

- (void)startWithCompletionBlock:(void (^)(NSError *error,
                                           NSURL *resultURL))block {
  _completionBlock = [block copy];
  _responseData = [NSMutableData data];

  NSDictionary *postInfo = @{ @"longUrl" : [[self URL] absoluteString] };
  NSData *postData =
      [NSJSONSerialization dataWithJSONObject:postInfo options:0 error:NULL];
  NSString *urlString = [NSString
      stringWithFormat:@"https://%@/shorten-url", [PWMetadataRequest hostname]];
  NSMutableURLRequest *request =
      [[NSMutableURLRequest alloc] initWithURL:[NSURL URLWithString:urlString]];
  [request setHTTPMethod:@"POST"];
  [request addValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
  [request setHTTPBody:postData];
  _connection = [[NSURLConnection alloc] initWithRequest:request delegate:self];
  [_connection start];
}

- (void)connection:(NSURLConnection *)connection
    didReceiveResponse:(NSURLResponse *)response {
  _responseCode = [(NSHTTPURLResponse *)response statusCode];
}

- (void)connection:(NSURLConnection *)connection didReceiveData:(NSData *)data {
  [_responseData appendData:data];
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection {
  NSDictionary *responseInfo =
      [NSJSONSerialization JSONObjectWithData:_responseData
                                      options:0
                                        error:NULL];
  if (![responseInfo isKindOfClass:[NSDictionary class]]) {
    [self _doneWithError:nil URL:nil];
    return;
  }
  NSString *responseURLString = [responseInfo objectForKey:@"id"];
  if (![responseURLString isKindOfClass:[NSString class]]) {
    [self _doneWithError:nil URL:nil];
    return;
  }
  NSURL *responseURL = [NSURL URLWithString:responseURLString];
  // responseURL might be nil if NSURL parser fails.
  [self _doneWithError:nil URL:responseURL];
  return;
}

- (void)connection:(NSURLConnection *)connection
    didFailWithError:(NSError *)error {
  [self _doneWithError:error URL:nil];
}

- (void)_doneWithError:(NSError *)error URL:(NSURL *)responseURL {
  if (_completionBlock == nil) {
    return;
  }
  if (responseURL == nil) {
    responseURL = [self URL];
  }
  _completionBlock(error, responseURL);
  _completionBlock = nil;
}

@end

@implementation PWInternalURLExpander {
  void (^_completionBlock)(NSError *error, NSURL *resultURL);
  NSURLConnection *_connection;
  NSInteger _responseCode;
  NSURL *_redirectedURL;
}

- (void)startWithCompletionBlock:(void (^)(NSError *error,
                                           NSURL *resultURL))block {
  _completionBlock = block;

  if (![self _supportsURL:[self URL]]) {
    [self _doneWithError:nil URL:nil];
    return;
  }

  NSMutableURLRequest *request =
      [[NSMutableURLRequest alloc] initWithURL:[self URL]];
  [request setTimeoutInterval:REDIRECT_TIMEOUT];
  [request setHTTPMethod:@"HEAD"];
  _connection = [[NSURLConnection alloc] initWithRequest:request delegate:self];
  [_connection start];
}

- (void)connection:(NSURLConnection *)connection
    didReceiveResponse:(NSURLResponse *)response {
  _responseCode = [(NSHTTPURLResponse *)response statusCode];
}

- (NSURLRequest *)connection:(NSURLConnection *)connection
             willSendRequest:(NSURLRequest *)request
            redirectResponse:(NSURLResponse *)redirectResponse {
  NSInteger responseCode = [(NSHTTPURLResponse *)redirectResponse statusCode];
  if (responseCode == 301) {
    _redirectedURL = [request URL];
    [_connection cancel];
    [self _doneWithError:nil URL:_redirectedURL];
    return nil;
  }
  return request;
}

- (void)connectionDidFinishLoading:(NSURLConnection *)connection {
  [self _doneWithError:nil URL:nil];
}

- (void)connection:(NSURLConnection *)connection
    didFailWithError:(NSError *)error {
  [self _doneWithError:error URL:nil];
}

- (void)_doneWithError:(NSError *)error URL:(NSURL *)responseURL {
  if (_completionBlock == nil) {
    return;
  }
  if (responseURL == nil) {
    responseURL = [self URL];
  }
  _completionBlock(error, responseURL);
  _completionBlock = nil;
}

- (BOOL)_supportsURL:(NSURL *)URL {
  NSString *hostname = [[URL host] lowercaseString];
  static NSMutableSet *supported = nil;
  @synchronized([self class]) {
    if (supported == nil) {
      supported = [[NSMutableSet alloc] init];
      [supported addObject:@"t.co"];
      [supported addObject:@"goo.gl"];
      [supported addObject:@"bit.ly"];
      [supported addObject:@"j.mp"];
      [supported addObject:@"bitly.com"];
      [supported addObject:@"amzn.to"];
      [supported addObject:@"fb.com"];
      [supported addObject:@"bit.do"];
      [supported addObject:@"adf.ly"];
      [supported addObject:@"u.to"];
      [supported addObject:@"tinyurl.com"];
      [supported addObject:@"buzurl.com"];
      [supported addObject:@"yourls.org"];
      [supported addObject:@"qr.net"];
    }
  }
  return [supported containsObject:hostname];
}

@end
