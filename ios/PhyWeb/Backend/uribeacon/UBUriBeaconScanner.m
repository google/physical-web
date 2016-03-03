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

#import "UBUriBeaconScanner.h"
#import "UBUriBeaconScannerPrivate.h"

#import "UBConfigurableUriBeacon.h"
#import "UBConfigurableUriBeaconPrivate.h"
#import "UBUriBeacon.h"
#import "UBUriBeaconPrivate.h"
#import "UBUriBeaconReader.h"
#import "UBUriBeaconWriter.h"
#import "UBUriReader.h"
#import "UBUriWriter.h"
#import "NSURL+UB.h"

#import <CoreBluetooth/CoreBluetooth.h>
#import <UIKit/UIKit.h>

#define SCAN_DELAY 4
#define PAUSE_DELAY 2

// When a beacon is connected, we have to stop scanning to avoid disconnection
// of the device.
// Therefore, when a beacon is connected, we act like bluetooth was turned off.

enum {
  STATE_ASSERT,
  STATE_IDLE_OFF,  // When the beacon browser is idle and bluetooth is turned
                   // off.
  STATE_IDLE_ON,  // When the beacon browser is idle and bluetooth is turned on.
  STATE_STARTED,  // Browing the nearby beacons has been requested but it's not
                  // scanning because bluetooth is turned off.
  STATE_SCANNING,  // It's currently scanning for nearby beacons.
  STATE_PAUSED,    // It's currently paused scanning the nearby beacons.
  STATE_INACTIVE_IDLE_OFF,  // Same as STATE_IDLE_OFF but the application is in
                            // the background.
  STATE_INACTIVE_IDLE_ON,   // Same as STATE_IDLE_ON but the application is in
                            // the background.
  STATE_INACTIVE_STARTED,   // Same as STATE_STARTED but the application is in
                            // the background.
  STATE_INACTIVE_SCANNING,  // Same as STATE_SCANNING but the application is in
                            // the background.
};

enum {
  TRANSITION_ON,   // Bluetooth has been turned on or the last connected device
                   // disconnected.
  TRANSITION_OFF,  // Bluetooth has been turned off or a device has been
                   // connected.
  TRANSITION_START,  // Start browing has been requested through the API.
  TRANSITION_STOP,   // Stop browsing has been requested through the API.
  TRANSITION_SCAN_DELAY_EXPIRED,  // Scan has been performed during [SCAN_DELAY]
                                  // sec and it's done.
  TRANSITION_PAUSE_DELAY_EXPIRED,  // Scan has been paused during [PAUSE_DELAY]
                                   // sec.
  TRANSITION_BECOME_ACTIVE,        // The application became active.
  TRANSITION_RESIGN_ACTIVE,        // The application is now in the background.
};

// Below, we define all state transitions in the state machine of the beacon
// browser.

typedef struct {
  int originState;
  int transition;
  int destinationState;
} stateChange;

static stateChange stateGraph[] = {
    {STATE_IDLE_OFF, TRANSITION_ON, STATE_IDLE_ON},
    {STATE_IDLE_OFF, TRANSITION_START, STATE_STARTED},
    {STATE_IDLE_OFF, TRANSITION_RESIGN_ACTIVE, STATE_INACTIVE_IDLE_OFF},
    {STATE_IDLE_ON, TRANSITION_OFF, STATE_IDLE_OFF},
    {STATE_IDLE_ON, TRANSITION_START,
     STATE_SCANNING},  // start scanning, set timer
    {STATE_IDLE_ON, TRANSITION_RESIGN_ACTIVE, STATE_INACTIVE_IDLE_ON},
    {STATE_STARTED, TRANSITION_STOP, STATE_IDLE_OFF},
    {STATE_STARTED, TRANSITION_ON,
     STATE_SCANNING},  // start scanning, set timer
    {STATE_STARTED, TRANSITION_RESIGN_ACTIVE, STATE_INACTIVE_STARTED},
    {STATE_STARTED, TRANSITION_BECOME_ACTIVE,
     STATE_STARTED},  // if -startScanningWithUpdateBlock: is called in
                      // -applicationDidBecomeActive:
    {STATE_SCANNING, TRANSITION_STOP, STATE_IDLE_ON},
    {STATE_SCANNING, TRANSITION_OFF, STATE_STARTED},  // stop scanning
    {STATE_SCANNING, TRANSITION_SCAN_DELAY_EXPIRED,
     STATE_PAUSED},  // stop scanning, set timer
    {STATE_SCANNING, TRANSITION_RESIGN_ACTIVE,
     STATE_INACTIVE_SCANNING},  // stop scanning, start scanning
    {STATE_PAUSED, TRANSITION_STOP, STATE_IDLE_ON},
    {STATE_PAUSED, TRANSITION_OFF, STATE_STARTED},
    {STATE_PAUSED, TRANSITION_PAUSE_DELAY_EXPIRED,
     STATE_SCANNING},  // start scanning, set timer
    {STATE_PAUSED, TRANSITION_RESIGN_ACTIVE,
     STATE_INACTIVE_SCANNING},  // start scanning
    {STATE_INACTIVE_IDLE_OFF, TRANSITION_ON, STATE_INACTIVE_IDLE_ON},
    {STATE_INACTIVE_IDLE_OFF, TRANSITION_START, STATE_INACTIVE_STARTED},
    {STATE_INACTIVE_IDLE_OFF, TRANSITION_BECOME_ACTIVE, STATE_IDLE_OFF},
    {STATE_INACTIVE_IDLE_ON, TRANSITION_OFF, STATE_INACTIVE_IDLE_OFF},
    {STATE_INACTIVE_IDLE_ON, TRANSITION_START,
     STATE_INACTIVE_SCANNING},  // start scanning
    {STATE_INACTIVE_IDLE_ON, TRANSITION_BECOME_ACTIVE, STATE_IDLE_ON},
    {STATE_INACTIVE_STARTED, TRANSITION_STOP, STATE_INACTIVE_IDLE_OFF},
    {STATE_INACTIVE_STARTED, TRANSITION_ON,
     STATE_INACTIVE_SCANNING},  // start scanning
    {STATE_INACTIVE_STARTED, TRANSITION_BECOME_ACTIVE, STATE_STARTED},
    {STATE_INACTIVE_SCANNING, TRANSITION_STOP, STATE_INACTIVE_IDLE_ON},
    {STATE_INACTIVE_SCANNING, TRANSITION_OFF,
     STATE_INACTIVE_STARTED},  // stop scanning
    {STATE_INACTIVE_SCANNING, TRANSITION_BECOME_ACTIVE,
     STATE_SCANNING},  // stop scanning, start scanning, set timer
};

@interface UBUriBeaconScanner ()<CBCentralManagerDelegate, CBPeripheralDelegate>

@end

@implementation UBUriBeaconScanner {
  CBCentralManager *_beaconsCentralManager;
  CBCentralManager *_configurableBeaconsCentralManager;
  int _state;
  BOOL _bluetoothOnOrConnected;

  NSMutableDictionary *_updatedBeacons;
  NSMutableDictionary *_updatedConfigurableBeacons;
  NSMutableArray *_beacons;
  NSMutableArray *_configurableBeacons;
  void (^_updateBlock)(void);

  NSMutableDictionary *_connectionsBlocks;
  NSMutableDictionary *_disconnectionsBlocks;
  NSMutableDictionary *_writers;
  NSMutableDictionary *_readers;
  NSMutableSet *_connectedBeacons;

  UIApplication *_application;
}

- (id)init {
  return [self initWithApplication:nil];
}

- (id)initWithApplication:(UIApplication *)application {
  self = [super init];
  if (!self) {
    return nil;
  }
  _beaconsCentralManager =
      [[CBCentralManager alloc] initWithDelegate:self queue:nil options:nil];
  [_beaconsCentralManager setDelegate:self];
  _configurableBeaconsCentralManager =
      [[CBCentralManager alloc] initWithDelegate:self queue:nil options:nil];
  [_configurableBeaconsCentralManager setDelegate:self];

  _application = application;
  BOOL applicationActive =
      _application != nil
          ? [_application applicationState] == UIApplicationStateActive
          : YES;
  _bluetoothOnOrConnected =
      [_beaconsCentralManager state] == CBCentralManagerStatePoweredOn;
  if (applicationActive && _bluetoothOnOrConnected) {
    _state = STATE_IDLE_ON;
  } else if (applicationActive && !_bluetoothOnOrConnected) {
    _state = STATE_IDLE_OFF;
  } else if (!applicationActive && _bluetoothOnOrConnected) {
    _state = STATE_INACTIVE_IDLE_ON;
  } else if (!applicationActive && !_bluetoothOnOrConnected) {
    _state = STATE_INACTIVE_IDLE_OFF;
  }

  [[NSNotificationCenter defaultCenter]
      addObserver:self
         selector:@selector(_didBecomeActive:)
             name:UIApplicationDidBecomeActiveNotification
           object:_application];
  [[NSNotificationCenter defaultCenter]
      addObserver:self
         selector:@selector(_willResignActive:)
             name:UIApplicationWillResignActiveNotification
           object:_application];
  _beacons = [NSMutableArray array];
  _configurableBeacons = [NSMutableArray array];

  _connectionsBlocks = [NSMutableDictionary dictionary];
  _disconnectionsBlocks = [NSMutableDictionary dictionary];
  _writers = [NSMutableDictionary dictionary];
  _readers = [NSMutableDictionary dictionary];
  _connectedBeacons = [NSMutableSet set];

  return self;
}

- (void)dealloc {
  [self _stopTimers];
  [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)_stopTimers {
  [NSObject
      cancelPreviousPerformRequestsWithTarget:self
                                     selector:@selector(_timerScanningExpired)
                                       object:nil];
  [NSObject
      cancelPreviousPerformRequestsWithTarget:self
                                     selector:@selector(_timerPausingExpired)
                                       object:nil];
}

- (void)_startTimerScanning {
  [self performSelector:@selector(_timerScanningExpired)
             withObject:nil
             afterDelay:SCAN_DELAY];
}

- (void)_timerScanningExpired {
  [self _transition:TRANSITION_SCAN_DELAY_EXPIRED];
}

- (void)_startTimerPausing {
  [self performSelector:@selector(_timerPausingExpired)
             withObject:nil
             afterDelay:PAUSE_DELAY];
}

- (void)_timerPausingExpired {
  [self _transition:TRANSITION_PAUSE_DELAY_EXPIRED];
}

- (void)_transition:(int)transition {
  [self _stopTimers];
  int previousState = _state;
  int state = STATE_ASSERT;
  for (unsigned int i = 0; i < sizeof(stateGraph) / sizeof(stateGraph[0]);
       i++) {
    if ((stateGraph[i].originState == _state) &&
        (stateGraph[i].transition == transition)) {
      state = stateGraph[i].destinationState;
      break;
    }
  }
  NSAssert(state != STATE_ASSERT, @"Transition not found in graph %i %i",
           _state, transition);
  _state = state;

  if ((_state == STATE_SCANNING) || (_state == STATE_INACTIVE_SCANNING)) {
    if ((previousState == STATE_SCANNING) ||
        (previousState == STATE_INACTIVE_SCANNING)) {
      [self _stopScanning];
    }
    [self _startScanning];
  } else if (_state == STATE_PAUSED) {
    [self _stopScanning];
    [self _startTimerPausing];
  } else if ((previousState == STATE_SCANNING) && (_state == STATE_STARTED)) {
    [self _stopScanning];
  } else if ((previousState == STATE_INACTIVE_SCANNING) &&
             (_state == STATE_INACTIVE_STARTED)) {
    [self _stopScanning];
  }
}

- (void)startScanningWithUpdateBlock:(void (^)(void))block {
  _updateBlock = [block copy];
  [self _transition:TRANSITION_START];
}

- (void)stopScanning {
  [self _transition:TRANSITION_STOP];
  _updateBlock = nil;
}

- (void)_startScanning {
  BOOL applicationActive = NO;
  switch (_state) {
    case STATE_IDLE_OFF:
    case STATE_IDLE_ON:
    case STATE_STARTED:
    case STATE_SCANNING:
    case STATE_PAUSED:
      applicationActive = YES;
      break;
  }
  _updatedBeacons = [NSMutableDictionary dictionary];
  _updatedConfigurableBeacons = [NSMutableDictionary dictionary];
  if (applicationActive) {
    [_beaconsCentralManager scanForPeripheralsWithServices:@[
      [CBUUID UUIDWithString:URIBEACON_SERVICE],
      [CBUUID UUIDWithString:EDDYSTONE_SERVICE]
    ] options:nil];
    [_configurableBeaconsCentralManager scanForPeripheralsWithServices:@[
      [CBUUID UUIDWithString:CONFIG_V1_SERVICE],
      [CBUUID UUIDWithString:CONFIG_V2_SERVICE]
    ] options:nil];
    [self _startTimerScanning];
  } else {
    NSArray *services = @[
      [CBUUID UUIDWithString:URIBEACON_SERVICE],
      [CBUUID UUIDWithString:EDDYSTONE_SERVICE]
    ];
    [_beaconsCentralManager scanForPeripheralsWithServices:services
                                                   options:nil];
  }
}

- (void)_stopScanning {
  [_beaconsCentralManager stopScan];
  [_configurableBeaconsCentralManager stopScan];
  [self _updateDataWithDeletion:YES];
}

- (void)_willResignActive:(NSNotification *)notification {
  [self _transition:TRANSITION_RESIGN_ACTIVE];
}

- (void)_didBecomeActive:(NSNotification *)notification {
  [self _transition:TRANSITION_BECOME_ACTIVE];
}

- (void)centralManager:(CBCentralManager *)central
 didDiscoverPeripheral:(CBPeripheral *)peripheral
     advertisementData:(NSDictionary *)advertisementData
                  RSSI:(NSNumber *)RSSI {
  if (central == _configurableBeaconsCentralManager) {
    UBConfigurableUriBeacon *configurableBeacon =
        [[UBConfigurableUriBeacon alloc] initWithPeripheral:peripheral
                                          advertisementData:advertisementData
                                                       RSSI:RSSI];
    [configurableBeacon setScanner:self];
    if (configurableBeacon != nil) {
      [_updatedBeacons removeObjectForKey:[configurableBeacon identifier]];
      [_updatedConfigurableBeacons setObject:configurableBeacon
                                      forKey:[configurableBeacon identifier]];
      [self _updateData];
    }
  } else {
    UBUriBeacon *beacon =
        [[UBUriBeacon alloc] initWithPeripheral:peripheral
                              advertisementData:advertisementData
                                           RSSI:RSSI];
    if (beacon != nil) {
      [_updatedConfigurableBeacons removeObjectForKey:[beacon identifier]];
      [_updatedBeacons setObject:beacon forKey:[beacon identifier]];
      [self _updateData];
    }
  }
}

- (void)centralManagerDidUpdateState:(CBCentralManager *)central {
  [self _updateBluetoothState];
}

- (void)_updateBluetoothState {
  BOOL bluetoothOnOrConnected =
      ([_beaconsCentralManager state] == CBCentralManagerStatePoweredOn) &&
      ([_connectedBeacons count] == 0);
  if (_bluetoothOnOrConnected == bluetoothOnOrConnected) {
    return;
  }
  _bluetoothOnOrConnected = bluetoothOnOrConnected;
  if (bluetoothOnOrConnected) {
    [self _transition:TRANSITION_ON];
  } else {
    [self _transition:TRANSITION_OFF];
  }
}

- (void)_updateData {
  [self _updateDataWithDeletion:NO];
}

- (void)_updateDataWithDeletion:(BOOL)applyDeletion {
  BOOL updated = NO;

  NSMutableDictionary *existingBeacons = [NSMutableDictionary dictionary];
  NSSet *updatedBeaconsIdentifiers = nil;
  for (UBUriBeacon *beacon in _beacons) {
    [existingBeacons setObject:beacon forKey:[beacon identifier]];
  }
  updatedBeaconsIdentifiers = [NSSet setWithArray:[_updatedBeacons allKeys]];
  for (UBUriBeacon *beacon in _beacons) {
    if (![updatedBeaconsIdentifiers containsObject:[beacon identifier]]) {
      // has deletion.
      updated = YES;
      break;
    }
  }
  NSMutableArray *beacons = [NSMutableArray array];
  NSMutableSet *beaconsSet = [NSMutableSet set];
  if (!applyDeletion) {
    // Add existing beacons in the results.
    for (UBUriBeacon *beacon in _beacons) {
      // Don't add beacons that are now in configuration mode.
      if ([_updatedConfigurableBeacons objectForKey:[beacon identifier]] !=
          nil) {
        continue;
      }
      UBUriBeacon *updatedBeacon =
          [_updatedBeacons objectForKey:[beacon identifier]];
      if (updatedBeacon != nil) {
        [beacon _updateWithBeacon:updatedBeacon];
        updated = YES;
      }
      [beacons addObject:beacon];
      [beaconsSet addObject:[beacon identifier]];
    }
  }
  // Add updated beacons.
  for (UBUriBeacon *beacon in [_updatedBeacons allValues]) {
    if ([beaconsSet containsObject:[beacon identifier]]) {
      continue;
    }

    UBUriBeacon *existingBeacon =
        [existingBeacons objectForKey:[beacon identifier]];
    if (existingBeacon != nil) {
      [beacons addObject:existingBeacon];
      if (![existingBeacon isEqual:beacon]) {
        // Update values.
        [existingBeacon _updateWithBeacon:beacon];
        updated = YES;
      }
    } else {
      [beacons addObject:beacon];
      [beaconsSet addObject:[beacon identifier]];
      updated = YES;
    }
  }

  existingBeacons = [NSMutableDictionary dictionary];
  for (UBConfigurableUriBeacon *beacon in _configurableBeacons) {
    [existingBeacons setObject:beacon forKey:[beacon identifier]];
  }
  updatedBeaconsIdentifiers =
      [NSSet setWithArray:[_updatedConfigurableBeacons allKeys]];
  for (UBConfigurableUriBeacon *beacon in _configurableBeacons) {
    if (![updatedBeaconsIdentifiers containsObject:[beacon identifier]]) {
      // has deletion.
      updated = YES;
      break;
    }
  }
  NSMutableArray *configurableBeacons = [NSMutableArray array];
  NSMutableSet *configurableBeaconsSet = [NSMutableSet set];
  if (!applyDeletion) {
    // Add existing configurable beacons in the result.
    for (UBConfigurableUriBeacon *beacon in _configurableBeacons) {
      // Don't add beacons that are not in configuration mode now.
      if ([_updatedBeacons objectForKey:[beacon identifier]] != nil) {
        continue;
      }
      UBConfigurableUriBeacon *updatedBeacon =
          [_updatedConfigurableBeacons objectForKey:[beacon identifier]];
      if (updatedBeacon != nil) {
        [beacon _updateWithConfigurableBeacon:updatedBeacon];
        [_updatedConfigurableBeacons removeObjectForKey:[beacon identifier]];
        updated = YES;
      }
      [configurableBeacons addObject:beacon];
      [configurableBeaconsSet addObject:[beacon identifier]];
    }
  }
  // Add updated beacons.
  for (UBConfigurableUriBeacon *beacon in
       [_updatedConfigurableBeacons allValues]) {
    if ([configurableBeaconsSet containsObject:[beacon identifier]]) {
      continue;
    }
    UBConfigurableUriBeacon *existingBeacon =
        [existingBeacons objectForKey:[beacon identifier]];
    if (existingBeacon != nil) {
      [configurableBeacons addObject:existingBeacon];
      if (![existingBeacon isEqual:beacon]) {
        // Update values.
        [existingBeacon _updateWithConfigurableBeacon:beacon];
        updated = YES;
      }
    } else {
      [configurableBeacons addObject:beacon];
      [configurableBeaconsSet addObject:[beacon identifier]];
      updated = YES;
    }
  }

  if (updated) {
    NSMutableSet *existingBeaconsIdentifiers = [NSMutableSet set];
    for (UBUriBeacon *beacon in beacons) {
      [existingBeaconsIdentifiers addObject:[beacon identifier]];
    }
    for (UBConfigurableUriBeacon *beacon in configurableBeacons) {
      [existingBeaconsIdentifiers addObject:[beacon identifier]];
    }

    _beacons = beacons;
    _configurableBeacons = configurableBeacons;
    [self _notify];
  }
}

- (void)_notify {
  if (_updateBlock != nil) {
    _updateBlock();
  }
}

- (NSArray * /* UBUriBeacon */)beacons {
  return _beacons;
}

- (NSArray * /* UBURIConfigurableBeacon */)configurableBeacons {
  return _configurableBeacons;
}

- (void)_connectBeaconWithPeripheral:(CBPeripheral *)peripheral
                     completionBlock:(void (^)(NSError *error))block {
  [_configurableBeaconsCentralManager connectPeripheral:peripheral options:nil];
  [_connectionsBlocks setObject:block forKey:[peripheral identifier]];
  [_connectedBeacons addObject:[peripheral identifier]];
  [self _updateBluetoothState];
}

- (void)centralManager:(CBCentralManager *)central
  didConnectPeripheral:(CBPeripheral *)peripheral {
  void (^block)(NSError *error) =
      [_connectionsBlocks objectForKey:[peripheral identifier]];
  [_connectionsBlocks removeObjectForKey:[peripheral identifier]];
  if (block != nil) {
    block(NULL);
  }
}

- (void)_disconnectBeaconWithPeripheral:(CBPeripheral *)peripheral
                        completionBlock:(void (^)(NSError *error))block {
  [_configurableBeaconsCentralManager cancelPeripheralConnection:peripheral];
  [_disconnectionsBlocks setObject:block forKey:[peripheral identifier]];
  [_connectedBeacons removeObject:[peripheral identifier]];
  // Remove the given configurable beacon. Now it's been disconnected, it should
  // switch back to normal mode.
  NSUInteger indexToRemove = [_configurableBeacons
      indexOfObjectPassingTest:^BOOL(id obj, NSUInteger idx, BOOL *stop) {
        UBConfigurableUriBeacon *beacon = obj;
        return [[beacon identifier] isEqual:[peripheral identifier]];
      }];
  if (indexToRemove != NSNotFound) {
    [_configurableBeacons removeObjectAtIndex:indexToRemove];
    [self _notify];
  }
  [self _updateBluetoothState];
}

- (void)centralManager:(CBCentralManager *)central
    didDisconnectPeripheral:(CBPeripheral *)peripheral
                      error:(NSError *)error {
  void (^block)(NSError *error) =
      [_disconnectionsBlocks objectForKey:[peripheral identifier]];
  [_disconnectionsBlocks removeObjectForKey:[peripheral identifier]];
  if (block != nil) {
    block(error);
  }
}

- (void)_writeBeaconWithPeripheral:(CBPeripheral *)peripheral
                 advertisementData:(NSData *)data
                   completionBlock:(void (^)(NSError *error))block {
  UBUriBeaconWriter *writer = [[UBUriBeaconWriter alloc] init];
  [writer setPeripheral:peripheral];
  [writer setData:data];
  [_writers setObject:writer forKey:[peripheral identifier]];
  [writer writeWithCompletionBlock:^(NSError *error) {
    void (^writeCompletionBlock)(NSError *error) = [block copy];
    [_writers removeObjectForKey:[peripheral identifier]];
    if (writeCompletionBlock != nil) {
      writeCompletionBlock(error);
    }
  }];
}

- (void)_readBeaconWithPeripheral:(CBPeripheral *)peripheral
                  completionBlock:
                      (void (^)(NSError *error, NSData *data))block {
  UBUriBeaconReader *reader = [[UBUriBeaconReader alloc] init];
  [reader setPeripheral:peripheral];
  [_readers setObject:reader forKey:[peripheral identifier]];
  [reader readWithCompletionBlock:^(NSError *error, NSData *data) {
    void (^readCompletionBlock)(NSError *error, NSData *data) = [block copy];
    [_readers removeObjectForKey:[peripheral identifier]];
    if (readCompletionBlock != nil) {
      readCompletionBlock(error, data);
    }
  }];
}

- (void)_writeURIv2WithPeripheral:(CBPeripheral *)peripheral
                              url:(NSURL *)url
                  completionBlock:(void (^)(NSError *error))block {
  UBUriWriter *writer = [[UBUriWriter alloc] init];
  [writer setPeripheral:peripheral];
  [writer setData:[url ub_encodedBeaconURI]];
  [writer setCharacteristic:CONFIG_V2_CHARACTERISTIC_URI];
  [_writers setObject:writer forKey:[peripheral identifier]];
  [writer writeWithCompletionBlock:^(NSError *error) {
    void (^writeCompletionBlock)(NSError *error) = [block copy];
    [_writers removeObjectForKey:[peripheral identifier]];
    if (writeCompletionBlock != nil) {
      writeCompletionBlock(error);
    }
  }];
}

- (void)_readURIv2WithPeripheral:(CBPeripheral *)peripheral
                 completionBlock:(void (^)(NSError *error, NSURL *uri))block {
  UBUriReader *reader = [[UBUriReader alloc] init];
  [reader setPeripheral:peripheral];
  [reader setCharacteristic:CONFIG_V2_CHARACTERISTIC_URI];
  [_readers setObject:reader forKey:[peripheral identifier]];
  [reader readWithCompletionBlock:^(NSError *error, NSData *data) {
    void (^readCompletionBlock)(NSError *error, NSURL *uri) = [block copy];
    [_readers removeObjectForKey:[peripheral identifier]];
    if (readCompletionBlock != nil) {
      readCompletionBlock(error, [NSURL ub_decodedBeaconURI:data]);
    }
  }];
}

@end
