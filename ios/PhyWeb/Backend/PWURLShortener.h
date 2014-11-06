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

#import <Foundation/Foundation.h>

typedef void (^PWURLShortenerCompletionBlock)(NSString *shortUrl);

@interface PWURLShortener : NSObject

// Shortens a URL using goo.gl. The completion block is called when the URL
// has been shortened. If there's an error, resultURL is be same as URL.
+ (void)shortenURL:(NSURL *)URL
        completion:(void (^)(NSError *error, NSURL *resultURL))block;

// Expands a URL if it's one of the supported URL shorteners. The completion
// block is called when the URL has been expanded. If there's an error,
// resultURL will be the same as URL.
+ (void)expandURL:(NSURL *)URL
       completion:(void (^)(NSError *error, NSURL *resultURL))block;

@end
