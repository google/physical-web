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
import argparse
import json
import os
import subprocess
import urllib
import urllib2


LOCAL_TEST_PORT = 9002

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

def testInvalidData(endpoint):
    values = {
        'objects': [
            { 'url': 'http://totallybadurlthatwontwork.com/' },
            { 'usdf': 'http://badkeys' },
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

def testUrlWhichRedirects(endpoint):
    values = {
        'objects': [
            {
                'url': 'http://goo.gl/KYvLwO',
            },
        ]
    }

    result = resolveScanForValues(endpoint, values)
    print json.dumps(result, indent=4)

################################################################################

def testUrlShortener(endpoint):
    values = {
        'longUrl': 'http://www.github.com/Google/physical-web'
    }
    req = urllib2.Request(endpoint + '/shorten-url', json.dumps(values))
    response = urllib2.urlopen(req)
    ret = json.loads(response.read())
    print ret

################################################################################

def testRefreshUrl(endpoint):
    params = { 'url': 'https://github.com/google/physical-web' }
    req = urllib2.Request(endpoint + '/refresh-url?' + urllib.urlencode(params), '')
    response = urllib2.urlopen(req)
    ret = response.read()
    print ret

################################################################################

def testRedirectWithRssiTxPower(endpoint):
    values = {
        'objects': [
            {
                'url': endpoint + '/experimental/googl/KYvLwO',
                'rssi': -41,
                'txpower': -22,
                'force': True,
            },
            {
                'url': endpoint + '/experimental/googl/r8iJqW',
                'rssi': -91,
                'txpower': -22,
                'force': True,
            },
        ]
    }

    result = resolveScanForValues(endpoint, values)
    print json.dumps(result, indent=4)

################################################################################

def testGoLink(endpoint):
    params = { 'url': 'https://github.com/google/physical-web' }
    req = urllib2.Request(endpoint + '/go?' + urllib.urlencode(params))
    response = urllib2.urlopen(req)
    print response.getcode()

################################################################################


def main():
    """The main routine."""
    # Parse arguments
    local_url = 'http://localhost:{}'.format(LOCAL_TEST_PORT)
    parser = argparse.ArgumentParser(description='print things')
    parser.add_argument(
            '-e', '--endpoint', dest='endpoint', default='AUTO',
            help='Which server to test against.\n'
                 'AUTO:  {} (server starts automatically)\n'
                 'LOCAL: http://localhost:8080\n'
                 'PROD:  http://url-caster.appspot.com\n'
                 'DEV:   http://url-caster-dev.appspot.com\n'
                 '*:     Other values interpreted literally'
                 .format(local_url))
    args = parser.parse_args()

    # Setup the endpoint
    endpoint = args.endpoint
    server = None
    if endpoint == 'AUTO':
        endpoint = local_url
        print 'Starting local server...',
        server = subprocess.Popen([
            'dev_appserver.py', os.path.dirname(__file__),
            '--port', str(LOCAL_TEST_PORT),
            '--admin_port', str(LOCAL_TEST_PORT + 1),
        ], bufsize=1, stderr=subprocess.PIPE)
        # Wait for the server to start up
        for line in iter(server.stderr.readline, b''):
            if 'running at: {}'.format(local_url) in line:
                break
        print 'done'
    elif endpoint == 'LOCAL':
        endpoint = 'http://localhost:8080'
    elif endpoint == 'PROD':
        endpoint = 'http://url-caster.appspot.com'
    elif endpoint == 'DEV':
        endpoint = 'http://url-caster-dev.appspot.com'
 
    # Run the tests
    try:
        testDemoData(endpoint)
        testInvalidData(endpoint)
        testRssiRanking(endpoint)
        testUrlWhichRedirects(endpoint)
        testUrlShortener(endpoint)
        testRefreshUrl(endpoint)
        testRedirectWithRssiTxPower(endpoint)
        testGoLink(endpoint)
    finally:
        # Teardown the endpoint
        if server:
            server.kill()
    
    return 0  # All tests passed because they can't really fail right now...


if __name__ == '__main__':
    try:
        exit(main())
    except KeyboardInterrupt:
        sys.stderr.write('Exiting due to KeyboardInterrupt!\n')
