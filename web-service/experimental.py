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

class GooglRedirect(webapp2.RequestHandler):
    def get(self, path):
        return self._redirect(path);

    def head(self, path):
        return self._redirect(path);

    def _redirect(self, path):
        try:
            distance = float(self.request.headers['X-PhysicalWeb-Distance'])
        except:
            distance = None

        logging.info('GoogleRedirect with distance:{0}'.format(distance))

        if distance > 2:
            self.response.set_status(204)
            return

        self.redirect('http://goo.gl/{0}'.format(path))

################################################################################

app = webapp2.WSGIApplication([
    ('/experimental/googl/(.*)', GooglRedirect),
], debug=True)

if not helpers.ENABLE_EXPERIMENTAL:
    app = webapp2.WSGIApplication([], debug=True)
