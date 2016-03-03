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

#import "NSString+UB.h"

@implementation NSString (PW)

struct expansion {
  char code;
  const char *value;
  int length;
};

static struct expansion prefixExpansionsList[] = {
    {0, "http://www."},
    {1, "https://www."},
    {2, "http://"},
    {3, "https://"},
    {4, "urn:uuid:"},
};

static struct expansion expansionsList[] = {
    {0, ".com/"},
    {1, ".org/"},
    {2, ".edu/"},
    {3, ".net/"},
    {4, ".info/"},
    {5, ".biz/"},
    {6, ".gov/"},
    {7, ".com"},
    {8, ".org"},
    {9, ".edu"},
    {10, ".net"},
    {11, ".info"},
    {12, ".biz"},
    {13, ".gov"},
};

static const char *prefixExpansionsMapping[255];
static const char *expansionsMapping[255];

+ (NSString *)ub_decodedBeaconURIString:(NSData *)data {
  const char *bytes = [data bytes];
  NSUInteger length = [data length];
  NSMutableData *resultData = [NSMutableData data];
  for (unsigned i = 0; i < length; i++) {
    const char *expansionValue = NULL;
    if (i == 0) {
      expansionValue = prefixExpansionsMapping[(unsigned char)bytes[i]];
    } else {
      expansionValue = expansionsMapping[(unsigned char)bytes[i]];
    }
    if (expansionValue == NULL) {
      // TODO(dvh): There's probably room for optimization: several non-encoded
      // bytes
      // could be appended in one -appendBytes:length: call.
      [resultData appendBytes:&bytes[i] length:1];
    } else {
      [resultData appendBytes:expansionValue length:strlen(expansionValue)];
    }
    if ((i == 0) && (bytes[0] == 4) && (length == sizeof(uuid_t) + 1)) {
      uuid_t bytes;
      memcpy(&bytes, ((const char *) [data bytes]) + 1, sizeof(uuid_t));
      NSUUID * uuid = [[NSUUID alloc] initWithUUIDBytes:bytes];
      const char * uuidString = [[uuid UUIDString] UTF8String];
      [resultData appendBytes:uuidString length:strlen(uuidString)];
      break;
    }
  }

  return
      [[NSString alloc] initWithData:resultData encoding:NSUTF8StringEncoding];
}

- (NSData *)ub_encodedBeaconURIString {
  // TODO(dvh): There's room for optimization: implementing a trie would help.
  NSData *data = [self dataUsingEncoding:NSUTF8StringEncoding];
  NSMutableData *encodedData = [NSMutableData data];
  unsigned int i = 0;
  const char *bytes = [data bytes];
  while (i < [data length]) {
    int found = -1;
    int foundLength = -1;
    struct expansion *table = NULL;
    unsigned int tableLength = 0;
    if (i == 0) {
      table = prefixExpansionsList;
      tableLength =
          sizeof(prefixExpansionsList) / sizeof(prefixExpansionsList[0]);
    } else {
      table = expansionsList;
      tableLength = sizeof(expansionsList) / sizeof(expansionsList[0]);
    }
    for (unsigned int k = 0; k < tableLength; k++) {
      const char *value = table[k].value;
      if (strncmp(bytes + i, value, table[k].length) == 0 &&
          table[k].length > foundLength) {
        found = k;
        foundLength = table[k].length;
      }
    }
    if (found != -1) {
      char b = (char)found;
      [encodedData appendBytes:&b length:1];
      i += table[found].length;
    } else {
      [encodedData appendBytes:bytes + i length:1];
      i++;
    }
    if ((i == foundLength) && (found == 4)) {
      NSString *uuidString = [[NSString alloc]
          initWithData:[data subdataWithRange:NSMakeRange(
                                                  foundLength,
                                                  [data length] - foundLength)]
              encoding:NSUTF8StringEncoding];
      NSUUID *uuid = [[NSUUID alloc] initWithUUIDString:uuidString];
      uuid_t bytes;
      [uuid getUUIDBytes:bytes];
      [encodedData appendBytes:(const char *)&bytes length:sizeof(bytes)];
      break;
    }
  }
  return encodedData;
}

@end

__attribute__((constructor)) static void initialize() {
  // Build the mapping for text expansions.
  for (unsigned int i = 0;
       i < sizeof(expansionsList) / sizeof(expansionsList[0]); i++) {
    expansionsMapping[expansionsList[i].code] = expansionsList[i].value;
    expansionsList[i].length = (int) strlen(expansionsList[i].value);
  }
  for (unsigned int i = 0;
       i < sizeof(prefixExpansionsList) / sizeof(prefixExpansionsList[0]); i++) {
    prefixExpansionsMapping[prefixExpansionsList[i].code] = prefixExpansionsList[i].value;
    prefixExpansionsList[i].length = (int) strlen(prefixExpansionsList[i].value);
  }
}
