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
import webapp2
import json
import logging
from datetime import datetime, timedelta
from google.appengine.ext import ndb
from google.appengine.api import urlfetch
from urlparse import urljoin
import os
import re
from lxml import etree
import cgi

class BaseModel(ndb.Model):
    added_on = ndb.DateTimeProperty(auto_now_add = True)
    updated_on = ndb.DateTimeProperty(auto_now = True)

class Device(BaseModel):
    name = ndb.StringProperty()
    url = ndb.StringProperty()

class SiteInformation(BaseModel):
    url = ndb.StringProperty()
    favicon_url = ndb.StringProperty()
    title = ndb.StringProperty()
    description = ndb.StringProperty()

class ResolveScan(webapp2.RequestHandler):
    def post(self):
        input_data = self.request.body
        input_object = json.loads(input_data) # Data is not sanitised.

        metadata_output = []
        output = {
          "metadata": metadata_output
        }

        devices = []
        if "objects" in input_object:
            objects = input_object["objects"]
        else:
            objects = []

        # Resolve the devices

        for obj in objects:
            key_id = None
            url = None
            force = False

            if "id" in obj:
                key_id = obj["id"]
            elif "url" in obj:
                key_id = obj["url"]
                url = obj["url"]

            if "force" in obj:
                force = True

            # We need to go and fetch.  We probably want to asyncly fetch.

            # We don't need RSSI yet.
            #rssi = obj["rssi"]

            # In this model we can only deal with one device with a given ID.
            device = Device.get_or_insert(key_id, name = key_id, url = url)

            device_data = {
              "id": device.name
            }

            if force or device.url is not None:
                # Really if we don't have the data we should not return it.
                siteInfo = SiteInformation.get_by_id(device.url)

                if force or siteInfo is None or siteInfo.updated_on < datetime.now() - timedelta(minutes=5):
                    # If we don't have the data or it is older than 5 minutes, fetch.
                    siteInfo = FetchAndStoreUrl(siteInfo, device.url)

                if siteInfo is not None:
                    device_data["url"] = siteInfo.url
                    device_data["title"] = siteInfo.title
                    device_data["description"] = siteInfo.description
                    device_data["icon"] = siteInfo.favicon_url
                    device_data["favicon_url"] = siteInfo.favicon_url
                else:
                    device_data["url"] = device.url

            metadata_output.append(device_data)

        logging.info(output);
        # Resolve from DB based off key.
        self.response.headers['Content-Type'] = 'application/json'
        json_data = json.dumps(output);
        self.response.write(json_data)

class SaveUrl(webapp2.RequestHandler):
    def post(self):
        name = self.request.get("name")
        url = self.request.get("url")

        title = ""
        icon = "/favicon.ico"

        device = Device.get_or_insert(name, name = name, url = url)
        device.url = url
        device.put()

        # Index the page
        FetchAndStoreUrl(device.url)
        self.redirect("/index.html")

def FetchAndStoreUrl(siteInfo, url):
    # Index the page
    result = urlfetch.fetch(url)
    if result.status_code == 200:
        encoding = GetContentEncoding(result.content)
        final_url = result.final_url or url
        return StoreUrl(siteInfo, url, final_url, result.content, encoding)

def GetContentEncoding(content):
    encoding = None
    parser = etree.HTMLParser(encoding='iso-8859-1')
    htmltree = etree.fromstring(content, parser)
    value = htmltree.xpath("//head//meta[@http-equiv='Content-Type']/attribute::content")
    if encoding is None:
        if (len(value) > 0):
            content_type = value[0]
            _, params = cgi.parse_header(content_type)
            if 'charset' in params:
                encoding = params['charset']

    if encoding is None:
        value = htmltree.xpath("//head//meta/attribute::charset")
        if (len(value) > 0):
            encoding = value[0]

    if encoding is None:
        try:
            encoding = 'utf-8'
            u_value = unicode(content, 'utf-8')
        except UnicodeDecodeError:
            encoding = 'iso-8859-1'
            u_value = unicode(content, 'iso-8859-1')

    return encoding

def StoreUrl(siteInfo, url, final_url, content, encoding):
    title = None
    description = None
    icon = None

    # parse the content

    parser = etree.HTMLParser(encoding=encoding)
    htmltree = etree.fromstring(content, parser)
    value = htmltree.xpath("//head//title/text()");
    if (len(value) > 0):
        title = value[0]

    # Try to use <meta name="description" content="...">.
    value = htmltree.xpath("//head//meta[@name='description']/attribute::content")
    if (len(value) > 0):
        description = value[0]
    if description is not None and len(description) == 0:
        description = None
    if description == title:
        description = None

    # Try to use <div class="content">...</div>.
    if description is None:
        value = htmltree.xpath("//body//*[@class='content']//*[not(*|self::script|self::style)]/text()")
        description = ' '.join(value)
        if len(description) == 0:
            description = None

    # Try to use <div id="content">...</div>.
    if description is None:
        value = htmltree.xpath("//body//*[@id='content']//*[not(*|self::script|self::style)]/text()")
        description = ' '.join(value)
        if len(description) == 0:
            description = None

    # Fallback on <body>...</body>.
    if description is None:
        value = htmltree.xpath("//body//*[not(*|self::script|self::style)]/text()")
        description = ' '.join(value)
        if len(description) == 0:
            description = None

    # Cleanup.
    if description is not None:
        description = description.strip()
        description = description.replace("\r", " ");
        description = description.replace("\n", " ");
        description = description.replace("\t", " ");
        description = description.replace("\v", " ");
        description = description.replace("\f", " ");
        while "  " in description:
            description = description.replace("  ", " ");
        if description is not None and len(description) > 500:
            description = description[:500]

    if icon is None:
        value = htmltree.xpath("//head//link[@rel='shortcut icon']/attribute::href");
        if (len(value) > 0):
            icon = value[0]
    if icon is None:
        value = htmltree.xpath("//head//link[@rel='icon']/attribute::href");
        if (len(value) > 0):
            icon = value[0]
    if icon is None:
        value = htmltree.xpath("//head//link[@rel='apple-touch-icon']/attribute::href");
        if (len(value) > 0):
            icon = value[0]

    if icon is not None:
        icon = urljoin(final_url, icon)
    if icon is None:
        icon = urljoin(final_url, "/favicon.ico")

    if siteInfo is None:
        siteInfo = SiteInformation.get_or_insert(url,
            url = final_url,
            title = title,
            favicon_url = icon,
            description = description)
    else:
        # update the data because it already exists
        siteInfo.url = final_url
        siteInfo.title = title
        siteInfo.favicon_url = icon
        siteInfo.description = description
        siteInfo.put()

    return siteInfo

class Index(webapp2.RequestHandler):
    def get(self):
        self.response.out.write("")

app = webapp2.WSGIApplication([
    ('/', Index),
    ('/resolve-scan', ResolveScan),
    ('/add-device', SaveUrl)
], debug=True)
