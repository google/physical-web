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
from google.appengine.api import taskqueue
from google.appengine.api import urlfetch
from urlparse import urljoin
from urlparse import urlparse
import os
import re
from lxml import etree
import cgi
from google.appengine.api import urlfetch_errors

class BaseModel(ndb.Model):
    added_on = ndb.DateTimeProperty(auto_now_add = True)
    updated_on = ndb.DateTimeProperty(auto_now = True)

class SiteInformation(BaseModel):
    url = ndb.TextProperty()
    favicon_url = ndb.TextProperty()
    title = ndb.TextProperty()
    description = ndb.TextProperty()
    jsonlds = ndb.TextProperty()

class DemoMetadata(webapp2.RequestHandler):
    def get(self):
        objects = [
            {'url': 'http://www.caltrain.com/schedules/realtime/stations/mountainviewstation-mobile.html'},
            {'url': 'http://benfry.com/distellamap/'},
            {'url': 'http://en.wikipedia.org/wiki/Le_D%C3%A9jeuner_sur_l%E2%80%99herbe'},
            {'url': 'http://sfmoma.org'}
        ]
        metadata_output = BuildResponse(objects)
        output = {
          "metadata": metadata_output
        }
        self.response.headers['Content-Type'] = 'application/json'
        json_data = json.dumps(output);
        self.response.write(json_data)

class RefreshUrl(webapp2.RequestHandler):
    def post(self):
        url = self.request.get('url')
        #logging.info("refreshing " + url)
        siteInfo = SiteInformation.get_by_id(url)
        siteInfo = FetchAndStoreUrl(siteInfo, url)

class ResolveScan(webapp2.RequestHandler):
    def post(self):
        input_data = self.request.body
        input_object = json.loads(input_data) # Data is not sanitised.
        
        if "objects" in input_object:
            objects = input_object["objects"]
        else:
            objects = []
        
        metadata_output = BuildResponse(objects)
        output = {
          "metadata": metadata_output
        }
        self.response.headers['Content-Type'] = 'application/json'
        json_data = json.dumps(output);
        self.response.write(json_data)

class GoUrl(webapp2.RequestHandler):
    def get(self):
        url = self.request.get('url')
        url = url.encode('ascii','ignore')
        self.redirect(url)

def BuildResponse(objects):
        metadata_output = []
        
        # Resolve the devices
        
        for obj in objects:
            key_id = None
            url = None
            force = False
            valid = True
            siteInfo = None

            if "id" in obj:
                key_id = obj["id"]
            elif "url" in obj:
                key_id = obj["url"]
                url = obj["url"]
                parsed_url = urlparse(url)
                if parsed_url.scheme != 'http' and parsed_url.scheme != 'https':
                    valid = False

            if "force" in obj:
                force = True
                
            # We need to go and fetch.  We probably want to asyncly fetch.

            # We don't need RSSI yet.
            #rssi = obj["rssi"]

            if valid:
                # Really if we don't have the data we should not return it.
                siteInfo = SiteInformation.get_by_id(url)

                if force or siteInfo is None:
                    # If we don't have the data or it is older than 5 minutes, fetch.
                    siteInfo = FetchAndStoreUrl(siteInfo, url)
                if siteInfo is not None and siteInfo.updated_on < datetime.now() - timedelta(minutes=5):
                    # Updated time to make sure we don't request twice.
                    siteInfo.put()
                    # Add request to queue.
                    taskqueue.add(url='/refresh-url', params={'url': url})

            device_data = {};
            if siteInfo is not None:
                device_data["id"] = url
                device_data["url"] = siteInfo.url
                if siteInfo.title is not None:
                    device_data["title"] = siteInfo.title
                if siteInfo.description is not None:
                    device_data["description"] = siteInfo.description
                if siteInfo.favicon_url is not None:
                    device_data["icon"] = siteInfo.favicon_url
                if siteInfo.jsonlds is not None:
                    device_data["json-ld"] = json.loads(siteInfo.jsonlds)
            else:
                device_data["id"] = url
                device_data["url"] = url

            metadata_output.append(device_data)

        return metadata_output

def FetchAndStoreUrl(siteInfo, url):
    # Index the page
    try:
        result = urlfetch.fetch(url, validate_certificate = True)
    except:
        return StoreInvalidUrl(siteInfo, url)

    if result.status_code == 200:
        encoding = GetContentEncoding(result.content)
        final_url = GetExpandedURL(url)
        real_final_url = result.final_url
        if real_final_url is None:
            real_final_url = final_url
        return StoreUrl(siteInfo, url, final_url, real_final_url, result.content, encoding)
    else:
        return StoreInvalidUrl(siteInfo, url)

def GetExpandedURL(url):
    parsed_url = urlparse(url)
    final_url = url
    url_shorteners = ['t.co', 'goo.gl', 'bit.ly', 'j.mp', 'bitly.com',
        'amzn.to', 'fb.com', 'bit.do', 'adf.ly', 'u.to', 'tinyurl.com',
        'buzurl.com', 'yourls.org', 'qr.net']
    url_shorteners_set = set(url_shorteners)
    if parsed_url.netloc in url_shorteners_set and (parsed_url.path != '/' or
        parsed_url.path != ''):
        # expand
        result = urlfetch.fetch(url, method = 'HEAD', follow_redirects = False)
        if result.status_code == 301:
            final_url = result.headers['location']
    return final_url

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

def FlattenString(input):
    input = input.strip()
    input = input.replace("\r", " ");
    input = input.replace("\n", " ");
    input = input.replace("\t", " ");
    input = input.replace("\v", " ");
    input = input.replace("\f", " ");
    while "  " in input:
        input = input.replace("  ", " ");
    return input

def StoreInvalidUrl(siteInfo, url):
    if siteInfo is None:
        siteInfo = SiteInformation.get_or_insert(url, 
            url = url,
            title = None,
            favicon_url = None,
            description = None,
            jsonlds = None)
    else:
        # Don't update if it was already cached.
        siteInfo.put()

    return siteInfo

def StoreUrl(siteInfo, url, final_url, real_final_url, content, encoding):
    title = None
    description = None
    icon = None
    
    # parse the content
    
    parser = etree.HTMLParser(encoding=encoding)
    htmltree = etree.fromstring(content, parser)
    value = htmltree.xpath("//head//title/text()");
    if (len(value) > 0):
        title = value[0]
    if title is None:
        value = htmltree.xpath("//head//meta[@property='og:title']/attribute::content");
        if (len(value) > 0):
            title = value[0]
    if title is not None:
        title = FlattenString(title)
    
    # Try to use <meta name="description" content="...">.
    value = htmltree.xpath("//head//meta[@name='description']/attribute::content")
    if (len(value) > 0):
        description = value[0]
    if description is not None and len(description) == 0:
        description = None
    if description == title:
        description = None
    
    # Try to use <meta property="og:description" content="...">.
    if description is None:
        value = htmltree.xpath("//head//meta[@property='og:description']/attribute::content")
        description = ' '.join(value)
        if len(description) == 0:
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
        description = FlattenString(description)
        if len(description) > 500:
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
        value = htmltree.xpath("//head//link[@rel='apple-touch-icon-precomposed']/attribute::href");
        if (len(value) > 0):
            icon = value[0]
    if icon is None:
        value = htmltree.xpath("//head//link[@rel='apple-touch-icon']/attribute::href");
        if (len(value) > 0):
            icon = value[0]
    if icon is None:
        value = htmltree.xpath("//head//meta[@property='og:image']/attribute::content");
        if (len(value) > 0):
            icon = value[0]
    
    if icon is not None:
        if icon.startswith("./"):
            icon = icon[2:len(icon)]
        icon = urljoin(real_final_url, icon)
    if icon is None:
        icon = urljoin(real_final_url, "/favicon.ico")
    # make sure the icon exists
    try:
        result = urlfetch.fetch(icon, method = 'HEAD')
        if result.status_code != 200:
            icon = None
        else:
            contentType = result.headers['Content-Type']
            if contentType is None:
                icon = None
            elif not contentType.startswith('image/'):
                icon = None
    except:
        s_url = url
        s_final_url = final_url
        s_real_final_url = real_final_url
        s_icon = icon
        if s_url is None:
            s_url = "[none]"
        if s_final_url is None:
            s_final_url = "[none]"
        if s_real_final_url is None:
            s_real_final_url = "[none]"
        if s_icon is None:
            s_icon = "[none]"
        logging.warning("icon error with " + s_url + " " + s_final_url + " " + s_real_final_url + " -> " + s_icon)
        icon = None

    jsonlds = []
    value = htmltree.xpath("//head//script[@type='application/ld+json']/text()");
    for jsonldtext in value:
        jsonldobject = None
        try:
            jsonldobject = json.loads(jsonldtext) # Data is not sanitised.
        except UnicodeDecodeError:
            jsonldobject = None
        if jsonldobject is not None:
            jsonlds.append(jsonldobject)

    if (len(jsonlds) > 0):
        jsonlds_data = json.dumps(jsonlds);
    else:
        jsonlds_data = None

    if siteInfo is None:
        siteInfo = SiteInformation.get_or_insert(url, 
            url = final_url,
            title = title,
            favicon_url = icon,
            description = description,
            jsonlds = jsonlds_data)
    else:
        # update the data because it already exists
        siteInfo.url = final_url
        siteInfo.title = title
        siteInfo.favicon_url = icon
        siteInfo.description = description
        siteInfo.jsonlds = jsonlds_data
        siteInfo.put()
    
    return siteInfo

class Index(webapp2.RequestHandler):
    def get(self):
        self.response.out.write("")

app = webapp2.WSGIApplication([
    ('/', Index),
    ('/resolve-scan', ResolveScan),
    ('/refresh-url', RefreshUrl),
    ('/go', GoUrl),
    ('/demo', DemoMetadata)
], debug=True)
