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
#import "UriBeacon.h"

@protocol PWMetadataRequestDelegate;

// This class will request to the metadata server the information about a
// beacon.
@interface PWMetadataRequest : NSObject

// Returns the name of the physical web server.
+ (NSString *)hostname;

// The list of peripherals.
@property(nonatomic, retain) NSArray * /* UBUriBeacon */ uriBeacons;

// Requests demo meta data.
@property(nonatomic, assign, getter=isDemo) BOOL demo;

// Delegate of the request. The delegate will be notified when the request is
// done.
@property(nonatomic, weak) id<PWMetadataRequestDelegate> delegate;

// List of beacons augmented with their metadata.
@property(nonatomic, retain, readonly) NSArray * /* PWBeacon */ results;

// It will be nil if no error happened. Otherwise, it's the error that happened
// during the request.
@property(nonatomic, retain, readonly) NSError *error;

@property(nonatomic, assign, readonly) NSTimeInterval delay;

// Start the request.
- (void)start;

// Cancel the request.
- (void)cancel;

@end

@protocol PWMetadataRequestDelegate

- (void)metadataRequest_done:(PWMetadataRequest *)request;

@end
