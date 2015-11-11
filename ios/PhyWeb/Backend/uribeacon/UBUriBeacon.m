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

#import "UBUriBeacon.h"
#import "UBUriBeaconPrivate.h"

#import "NSURL+UB.h"

enum {
  SERVICE_TYPE_URIBEACON,
  SERVICE_TYPE_EDDYSTONE,
};

@implementation UBUriBeacon

- (id)initWithPeripheral:(CBPeripheral *)peripheral
       advertisementData:(NSDictionary *)advertisementData
                    RSSI:(NSNumber *)RSSI {
  self = [super init];
  if (!self) {
    return nil;
  }
  NSDictionary *info =
      [advertisementData objectForKey:CBAdvertisementDataServiceDataKey];
  if (info == nil) {
    // No service data.
    return nil;
  }
  int type = SERVICE_TYPE_URIBEACON;
  NSData *data = [info objectForKey:[CBUUID UUIDWithString:URIBEACON_SERVICE]];
  if (data == nil) {
    // No UriBeacon service data.
    type = SERVICE_TYPE_EDDYSTONE;
    data = [info objectForKey:[CBUUID UUIDWithString:EDDYSTONE_SERVICE]];
    if (data == nil) {
      // No service data.
      return nil;
    }
  }
  if (![self _parseFromData:data type:type]) {
    // Data couldn't be parsed.
    return nil;
  }
  [self setIdentifier:[peripheral identifier]];
  [self setRSSI:[RSSI intValue]];

  return self;
}

- (id)initWithPeripheral:(CBPeripheral *)peripheral
                    data:(NSData *)data
                    RSSI:(NSInteger)RSSI {
  self = [super init];
  if (!self) {
    return nil;
  }
  if ([data length] < 8) {
    return nil;
  }
  if (![self _parseFromData:[data subdataWithRange:NSMakeRange(
                                                       8, [data length] - 8)]
                       type:SERVICE_TYPE_URIBEACON]) {
    // Data couldn't be parsed.
    return nil;
  }
  [self setIdentifier:[peripheral identifier]];
  [self setRSSI:RSSI];

  return self;
}

- (id)initWithURI:(NSURL *)URI txPowerLevel:(int)txPowerLevel {
  return [self initWithURI:URI txPowerLevel:txPowerLevel flags:0];
}

- (id)initWithURI:(NSURL *)URI
     txPowerLevel:(int)txPowerLevel
            flags:(UBUriBeaconFlags)flags {
  self = [super init];
  if (!self) {
    return nil;
  }
  [self setURI:URI];
  [self setTxPowerLevel:txPowerLevel];
  [self setFlags:flags];
  [self setRSSI:127];
  return self;
}

- (BOOL)_parseFromData:(NSData *)data type:(int)type {
  size_t length = [data length];
  if (length <= 2) {
    return NO;
  }
  int packetType = ((unsigned char *)[data bytes])[0];
  if (type == SERVICE_TYPE_EDDYSTONE && ((packetType & 0xf0) != 0x10)) {
    return NO;
  }
  [self setFlags:((unsigned char *)[data bytes])[0]];
  [self setTxPowerLevel:((char *)[data bytes])[1]];
  NSData *encodedURI = [data subdataWithRange:NSMakeRange(2, length - 2)];
  [self setURI:[NSURL ub_decodedBeaconURI:encodedURI]];
  return YES;
}

- (BOOL)isEqual:(id)object {
  UBUriBeacon *otherBeacon = object;
  return [[self identifier] isEqual:[otherBeacon identifier]] &&
         ([self RSSI] == [otherBeacon RSSI]) &&
         [[self URI] isEqual:[otherBeacon URI]] &&
         ([self txPowerLevel] == [otherBeacon txPowerLevel]) &&
         [self flags] == [otherBeacon flags];
}

- (NSData *)_advertisementData {
  static char advdata[] = {
      0x03,  // length
      0x03,  // Service ID
      0xD8,
      0xFE,  // UUID
      0x0,   // length
      0x16,  // Service Data
      0xD8,
      0xFE,  // UUID
      0x0,   // flags
      0x0,   // Transmit power
  };

  NSMutableData *result = [NSMutableData data];
  [result appendBytes:advdata length:sizeof(advdata)];
  NSData *encodedURI = [[self URI] ub_encodedBeaconURI];
  [result appendData:encodedURI];
  unsigned char *bytes = [result mutableBytes];
  bytes[4] = [encodedURI length] + 5;
  bytes[8] = [self flags];
  bytes[9] = [self txPowerLevel];

  if ([result length] > 28) {
    // URL is too long to fit in the packet.
    return nil;
  }

  return result;
}

- (void)_updateWithBeacon:(UBUriBeacon *)beacon {
  [self setRSSI:[beacon RSSI]];
  [self setURI:[beacon URI]];
  [self setTxPowerLevel:[beacon txPowerLevel]];
  [self setFlags:[beacon flags]];
}

- (BOOL)isValid {
  return [self _advertisementData] != nil;
}

/*
 * Key to variable names used in this class (viz. Physics):
 *
 * c = speed of light (2.9979 x 10^8 m/s);
 *
 * f = frequency (Bluetooth frequency is 2.45GHz = 2.45x10^9 Hz);
 *
 * l = wavelength (meters);
 *
 * d = distance (from transmitter to receiver in meters);
 *
 *
 * Free-space path loss (FSPL) is proportional to the square of the distance
 * between the transmitter and the receiver, and also proportional to the square
 *of the frequency of the radio signal.
 *
 * FSPL = (4 * pi * d / l)^2 = (4 * pi * d * f / c )^2
 *
 * FSPL (dBm) = 20*log10(d) + 20*log10(f) + 20*log10(4*pi/c) = 20*log10(d) +
 *              PATH_LOSS_AT_1M
 *
 * Calculating constants:
 *
 * FSPL_FREQ = 20*log10(f) = 20*log10(2.45 * 10^9) = 188.78 [round to 189]
 *
 * FSPL_LIGHT = 20*log10(4*pi/c) = 20*log10(4pi/2.9979*10^8) = -147.55
 *                                                          [round to -148]
 *
 * PATH_LOSS_AT_1M = FSPL_FREQ + FSPL_LIGHT = 188.78 - 147.55 = 41.23
 *                                                          [round to 41]
 *
 *
 * Re-arranging formula to provide a solution for distance when the path loss
 * (FPSL) is available:
 *
 * 20*log10(d) = path loss - PATH_LOSS_AT_1M
 *
 * distance(d) = 10^((path loss - PATH_LOSS_AT_1M)/20.0)
 *
 * The beacon will broadcast its power as it would be seen in ideal conditions
 * at 1 meter, computed using the following equation from its own source power.
 *
 * calibratedTxPower = txPowerAtSource - path loss at 1m (for BLE 1m path loss
 * is 41dBm)
 */

// Free Space Path Loss (FSPL) Constants (see above)
#define FSPL_FREQ 189
#define FSPL_LIGHT -148

/* (dBm) PATH_LOSS at 1m for isotropic antenna transmitting BLE */
#define FREE_SPACE_PATH_LOSS_CONSTANT_FOR_BLE \
  (FSPL_FREQ + FSPL_LIGHT)  // const = 41

// Cutoff distances between different regions.
#define NEAR_TO_MID_METERS 0.5
#define MID_TO_FAR_METERS 2.0

- (UBUriBeaconRegion)region {
  NSInteger txPowerAtSource = [self txPowerLevel];
  NSInteger pathLoss = txPowerAtSource - [self RSSI];
  // Distance calculation
  double distance =
      pow(10.0, (pathLoss - FREE_SPACE_PATH_LOSS_CONSTANT_FOR_BLE) / 20.0);

  if (distance < 0) {
    return UBUriBeaconRegionUnknown;
  }
  if (distance <= NEAR_TO_MID_METERS) {
    return UBUriBeaconRegionNear;
  }
  if (distance <= MID_TO_FAR_METERS) {
    return UBUriBeaconRegionMid;
  }
  return UBUriBeaconRegionFar;
}

- (NSString *)description {
  return [NSString stringWithFormat:@"<%@: %p %@ %@ %lu %i %li>", [self class],
                                    self, [self identifier], [self URI],
                                    (unsigned long)[self flags],
                                    [self txPowerLevel], (long)[self RSSI]];
}

@end
