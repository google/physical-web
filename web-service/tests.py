#!/usr/bin/env python
#
# Copyright 2014 Google Inc.
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
import urllib
import urllib2

################################################################################

ENDPOINT = 'http://localhost:8080/resolve-scan'

################################################################################

def test1():
    values = {
        "objects": [
            {'url': 'http://www.caltrain.com/schedules/realtime/stations/mountainviewstation-mobile.html'},
            {'url': 'http://benfry.com/distellamap/'},
            {'url': 'http://en.wikipedia.org/wiki/Le_D%C3%A9jeuner_sur_l%E2%80%99herbe'},
            {'url': 'http://sfmoma.org'}
        ]
    }

    req = urllib2.Request(ENDPOINT, json.dumps(values))
    response = urllib2.urlopen(req)
    the_page = response.read()
    print json.dumps(json.loads(the_page), indent=4)

if __name__ == '__main__':
    test1()
