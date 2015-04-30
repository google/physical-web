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

import SimpleHTTPServer
import SocketServer

class myHandler(SimpleHTTPServer.SimpleHTTPRequestHandler):
    def do_GET(self):
        try:
            rssi = float(self.headers['X-PhysicalWeb-Rssi'])
            txpower = float(self.headers['X-PhysicalWeb-TxPower'])
            path_loss = txpower - rssi;
        except:
            path_loss = None

        if path_loss > 50:
            self.send_response(204)
        else:
            self.send_response(301)
            # redirect to same path on goo.gl!
            self.send_header('Location', 'https://goo.gl/' + self.path)
        self.end_headers()

PORT = 8800
handler = SocketServer.TCPServer(("", PORT), myHandler)
print "serving at port {0}".format(PORT)
handler.serve_forever()
