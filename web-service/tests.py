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
import nose
import os
import signal
import subprocess
import sys
import unittest
import urllib
import urllib2

LOCAL_TEST_PORT = 9002

REGRESSION_TEST_URLS = [
        'http://www.blackanddecker.fr',
        'http://www.google.com',
        'http://dota2.gamepedia.com/',
        'http://www.orange.fr',
        'http://librarian.codes',
        'http://fredrikthalberg.com',
        'http://harleykwyn.com',
    ]

REGRESSION_TEST_BAD_URLS = [
        'http://google.com/asdfasdfasdfasdfa',
        'http://www.',
    ]

class PwsTest(unittest.TestCase):
    _HOST = None  # Set in main()
    _ENABLE_EXPERIMENTAL = False

    @property
    def HOST(self):
        PwsTest._HOST

    @property
    def ENABLE_EXPERIMENTAL(self):
        PwsTest._ENABLE_EXPERIMENTAL

    def request(self, params=None, payload=None):
        """
        Makes an http request to our endpoint

        If payload is None, this performs a GET request.
        Otherwise, the payload is json-serialized and a POST request is sent.

        """
        JSON = getattr(self, 'JSON', False)
        url = '{}/{}'.format(self.HOST, self.PATH)
        if params:
            url += '?{}'.format(urllib.urlencode(params))
        args = [url]
        if payload is not None:
            args.append(json.dumps(payload))
        req = urllib2.Request(*args)
        req.add_header("Content-Type", "application/json")
        response = urllib2.urlopen(req)
        data = response.read()
        if JSON:
            data = json.loads(data)
            # Print so we have something nice to look at when we fail
            print json.dumps(data, indent=2)
        else:
            print data
        return response.code, data


class TestResolveScan(PwsTest):
    PATH = 'resolve-scan'
    JSON = True

    def call(self, values):
        return self.request(payload=values)[1]

    def test_demo_data(self):
        result = self.call({
            'objects': [
                { 'url': 'http://www.caltrain.com/schedules/realtime/stations/mountainviewstation-mobile.html' },
                { 'url': 'http://benfry.com/distellamap/' },
                { 'url': 'http://en.wikipedia.org/wiki/Le_D%C3%A9jeuner_sur_l%E2%80%99herbe' },
                { 'url': 'http://sfmoma.org' }
            ]
        })
        self.assertIn('metadata', result)
        self.assertEqual(len(result['metadata']), 4)
        self.assertIn('description', result['metadata'][0])
        self.assertIn('title', result['metadata'][0])
        self.assertIn('url', result['metadata'][0])
        self.assertIn('displayUrl', result['metadata'][0])
        self.assertIn('rank', result['metadata'][0])
        self.assertIn('id', result['metadata'][0])
        self.assertIn('icon', result['metadata'][0])

    def test_invalid_data(self):
        result = self.call({
            'objects': [
                { 'url': 'http://totallybadurlthatwontwork.com/' },
                { 'usdf': 'http://badkeys' },
            ]
        })
        self.assertIn('metadata', result)
        self.assertEqual(len(result['metadata']), 0)
        #self.assertEqual(len(result['unresolved']), 1)

    def test_secure_only(self):
        result = self.call({
            'objects': [
                { 'url': 'https://en.wikipedia.org/wiki/Le_D%C3%A9jeuner_sur_l%E2%80%99herbe' },
                { 'url': 'http://www.lemonde.fr/' },
            ],
            'secureOnly': True
        })
        self.assertIn('metadata', result)
        self.assertEqual(len(result['metadata']), 1)
        self.assertEqual(result['metadata'][0]['id'],
                         'https://en.wikipedia.org/wiki/Le_D%C3%A9jeuner_sur_l%E2%80%99herbe')

    def test_rssi_ranking(self):
        result = self.call({
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
        })
        self.assertIn('metadata', result)
        self.assertEqual(len(result['metadata']), 4)
        self.assertEqual(result['metadata'][0]['id'],
                         'http://benfry.com/distellamap/')
        self.assertEqual(result['metadata'][1]['id'],
                         'http://en.wikipedia.org/wiki/'
                         'Le_D%C3%A9jeuner_sur_l%E2%80%99herbe')
        self.assertEqual(result['metadata'][2]['id'],
                         'http://sfmoma.org')
        self.assertEqual(result['metadata'][3]['id'],
                         'http://www.caltrain.com/schedules/realtime/'
                         'stations/mountainviewstation-mobile.html')

    def test_url_which_redirects(self):
        result = self.call({
            'objects': [
                {
                    'url': 'http://goo.gl/KYvLwO',
                },
            ]
        })
        self.assertIn('metadata', result)
        self.assertEqual(len(result['metadata']), 1)

        beaconResult = result['metadata'][0]

        self.assertEqual(beaconResult['id'],
                         'http://goo.gl/KYvLwO')
        self.assertEqual(beaconResult['url'],
                         'https://github.com/Google/physical-web')
        self.assertEqual(beaconResult['displayUrl'],
                         'https://github.com/Google/physical-web')

    def test_redirect_with_rssi_tx_power(self):
        if not self.ENABLE_EXPERIMENTAL:
            return

        result = self.call({
            'objects': [
                {
                    'url': '{}/experimental/googl/KYvLwO'.format(self.HOST),
                    'rssi': -41,
                    'txpower': -22
                },
                {
                    'url': '{}/experimental/googl/r8iJqW'.format(self.HOST),
                    'rssi': -91,
                    'txpower': -22
                },
            ]
        })
        self.assertIn('metadata', result)
        self.assertEqual(len(result['metadata']), 1)
        self.assertEqual(result['metadata'][0]['url'],
                         'https://github.com/Google/physical-web')

    def test_regression_urls(self):
        result = self.call({
            'objects': [ {'url': url} for url in REGRESSION_TEST_URLS ]
        })
        self.assertIn('metadata', result)
        self.assertEqual(len(result['metadata']), len(REGRESSION_TEST_URLS))

        for beaconResult in result['metadata']:
            self.assertIn('description', beaconResult)
            self.assertIn('title', beaconResult)
            self.assertIn('url', beaconResult)
            self.assertIn('rank', beaconResult)
            self.assertIn('id', beaconResult)
            self.assertIn('icon', beaconResult)

    def test_regression_bad_urls(self):
        result = self.call({
            'objects': [ {'url': url} for url in REGRESSION_TEST_BAD_URLS ]
        })
        self.assertIn('metadata', result)
        self.assertEqual(len(result['metadata']), 0)

    def test_invalid_rssi(self):
        result = self.call({
            'objects': [{
                'url': 'http://github.com/google/physical-web/',
                'rssi': 127,
                'txpower': -41
                }]
        })
        self.assertIn('metadata', result)
        self.assertEqual(len(result['metadata']), 1)

        beaconResult = result['metadata'][0]

        self.assertIn('description', beaconResult)
        self.assertIn('title', beaconResult)
        self.assertIn('url', beaconResult)
        self.assertIn('rank', beaconResult)
        self.assertIn('id', beaconResult)
        self.assertIn('icon', beaconResult)

        self.assertEqual(1000, beaconResult['rank'])


class TestShortenUrl(PwsTest):
    PATH = 'shorten-url'
    JSON = True

    def call(self, values):
        return self.request(payload=values)[1]

    def test_github_url(self):
        result = self.call({
            'longUrl': 'http://www.github.com/Google/physical-web'
        })
        self.assertIn('kind', result)
        self.assertIn('id', result)
        self.assertIn('longUrl', result)
        self.assertTrue(result['id'].startswith('http://goo.gl/'))


class RefreshUrl(PwsTest):
    PATH = 'refresh-url'

    def call(self, url):
        params = {'url': url}
        return self.request(params=params, payload='')[1]

    def test_github_url(self):
        result = self.call('https://github.com/google/physical-web')
        self.assertEqual(result, '')


class TestGo(PwsTest):
    PATH = 'go'

    def call(self, url):
        params = {'url': url}
        return self.request(params=params)[0]

    def test_github_url(self):
        result = self.call('https://github.com/google/physical-web')
        self.assertEqual(result, 200)


def main():
    """The main routine."""
    # Parse arguments
    local_url = 'http://localhost:{}'.format(LOCAL_TEST_PORT)
    parser = argparse.ArgumentParser(description='Run web-service tests')
    parser.add_argument(
            '-e', '--endpoint', dest='endpoint', default='auto',
            help='Which server to test against.\n'
                 'auto:  {} (server starts automatically)\n'
                 'local: http://localhost:8080\n'
                 'prod:  https://url-caster.appspot.com\n'
                 'dev:   https://url-caster-dev.appspot.com\n'
                 '*:     Other values interpreted literally'
                 .format(local_url))
    parser.add_argument('-x', '--experimental', dest='experimental', action='store_true', default=False)
    args = parser.parse_args()

    # Setup the endpoint
    endpoint = args.endpoint
    server = None
    if endpoint.lower() == 'auto':
        endpoint = local_url
        print 'Starting local server...',
        server = subprocess.Popen([
            'dev_appserver.py', os.path.dirname(__file__),
            '--port', str(LOCAL_TEST_PORT),
            '--admin_port', str(LOCAL_TEST_PORT + 1),
        ], bufsize=1, stderr=subprocess.PIPE, preexec_fn=os.setsid)
        # Wait for the server to start up
        while True:
            line = server.stderr.readline()
            if 'Unable to bind' in line:
                print 'Rogue server already running.'
                return 1
            if 'running at: {}'.format(local_url) in line:
                break
        print 'done'
    elif endpoint.lower() == 'local':
        endpoint = 'http://localhost:8080'
    elif endpoint.lower() == 'prod':
        endpoint = 'https://url-caster.appspot.com'
    elif endpoint.lower() == 'dev':
        endpoint = 'https://url-caster-dev.appspot.com'
    PwsTest.HOST = endpoint
    PwsTest.ENABLE_EXPERIMENTAL = args.experimental

    # Run the tests
    try:
        nose.runmodule()
    finally:
        # Teardown the endpoint
        if server:
            os.killpg(os.getpgid(server.pid), signal.SIGINT)
            server.wait()

    # We should never get here since nose.runmodule will call exit
    return 0


if __name__ == '__main__':
    try:
        exit(main())
    except KeyboardInterrupt:
        sys.stderr.write('Exiting due to KeyboardInterrupt!\n')
