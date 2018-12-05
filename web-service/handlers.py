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
import helpers
import json
import logging
import models
import webapp2

################################################################################

class Index(webapp2.RequestHandler):
    def get(self):
        self.response.out.write('')

class GoUrl(webapp2.RequestHandler):
    def get(self):
        url = self.request.get('url')
        url = url.encode('ascii', 'ignore')
        self.redirect(url)

################################################################################

class RefreshUrl(webapp2.RequestHandler):
    def post(self):
        url = self.request.get('url')
        helpers.RefreshUrl(url)

################################################################################

class ResolveScan(webapp2.RequestHandler):
    def post(self):
        input_data = self.request.body

        try:
            input_object = json.loads(input_data) # TODO: Data is not sanitised.
            objects = input_object.get('objects', [])
        except:
            objects = []

        metadata_output = helpers.BuildResponse(objects)
        output = {
          'metadata': metadata_output
        }
        self.response.headers['Content-Type'] = 'application/json'
        json_data = json.dumps(output);
        self.response.write(json_data)

################################################################################

class DemoMetadata(webapp2.RequestHandler):
    def get(self):
        objects = [
            {'url': 'http://www.caltrain.com/schedules/realtime/stations/mountainviewstation-mobile.html'},
            {'url': 'http://benfry.com/distellamap/'},
            {'url': 'http://en.wikipedia.org/wiki/Le_D%C3%A9jeuner_sur_l%E2%80%99herbe'},
            {'url': 'http://sfmoma.org'}
        ]
        metadata_output = helpers.BuildResponse(objects)
        output = {
          'metadata': metadata_output
        }
        self.response.headers['Content-Type'] = 'application/json'
        json_data = json.dumps(output);
        self.response.write(json_data)

################################################################################

app = webapp2.WSGIApplication([
    ('/', Index),
    ('/resolve-scan', ResolveScan),
    ('/refresh-url', RefreshUrl),
    ('/go', GoUrl),
    ('/demo', DemoMetadata)
], debug=True)
