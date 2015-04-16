#!/usr/bin/env python
#
# Copyright 2015 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
import json
import urllib2

################################################################################

LOCAL_ENDPOINT = 'http://localhost:8080'
REMOTE_ENDPOINT = 'http://url-caster.appspot.com'
REMOTE_DEV_ENDPOINT = 'http://url-caster-dev.appspot.com'

################################################################################

def resolveScanForValues(endpoint, values):
    req = urllib2.Request(endpoint + '/resolve-scan', json.dumps(values))
    response = urllib2.urlopen(req)
    return json.loads(response.read())

################################################################################

def testDemoData(endpoint):
    values = {
        'objects': [
            { 'url': 'http://www.caltrain.com/schedules/realtime/stations/mountainviewstation-mobile.html' },
            { 'url': 'http://benfry.com/distellamap/' },
            { 'url': 'http://en.wikipedia.org/wiki/Le_D%C3%A9jeuner_sur_l%E2%80%99herbe' },
            { 'url': 'http://sfmoma.org' }
        ]
    }

    result = resolveScanForValues(endpoint, values)
    print json.dumps(result, indent=4)

################################################################################

def testRssiRanking(endpoint):
    values = {
        'objects': [
            {
                'url': 'http://www.caltrain.com/schedules/realtime/stations/mountainviewstation-mobile.html',
                'rssi': -75,
                'txpower': -22,
            },
            {
                'url': 'http://benfry.com/distellamap/',
                'rssi': -95,
                'txpower': -63,
            },
            {
                'url': 'http://en.wikipedia.org/wiki/Le_D%C3%A9jeuner_sur_l%E2%80%99herbe',
                'rssi': -61,
                'txpower': -22,
            },
            {
                'url': 'http://sfmoma.org',
                'rssi': -74,
                'txpower': -22,
            },
        ]
    }

    result = resolveScanForValues(endpoint, values)
    print json.dumps(result, indent=4)

################################################################################

def testUrlShortener(endpoint):
    values = {
        'longUrl': 'www.github.com/Google/physical-web'
    }
    req = urllib2.Request(endpoint + '/shorten-url', json.dumps(values))
    response = urllib2.urlopen(req)
    ret = json.loads(response.read())
    print ret

################################################################################
################################################################################

if __name__ == '__main__':
    testDemoData(LOCAL_ENDPOINT)
    testRssiRanking(LOCAL_ENDPOINT)
    testUrlShortener(LOCAL_ENDPOINT)
