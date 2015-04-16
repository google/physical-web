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

from google.appengine.api import urlfetch
import helpers
import json
import webapp2

################################################################################

class ShortURL(webapp2.RequestHandler):
    def post(self):
        input_data = self.request.body
        config = helpers.GetConfig()
        apikey = config['oauth_keys']['goo.gl']
        url = 'https://www.googleapis.com/urlshortener/v1/url?key=' + apikey
        referer = 'url-cast.physical-web.org'
        result = urlfetch.fetch(url,
                validate_certificate=True,
                method=urlfetch.POST,
                payload=input_data,
                headers = {
                    'Content-Type': 'application/json',
                    'Referer': referer
                })
        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(result.content)

################################################################################

app = webapp2.WSGIApplication([
    ('/shorten-url', ShortURL),
], debug=True)
