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

from google.appengine.api import taskqueue, urlfetch
from urlparse import urljoin, urlparse
import cgi
import datetime
import json
import logging
import lxml.etree
import models

################################################################################

def BuildResponse(objects):
    metadata_output = []

    # Resolve the devices
    for obj in objects:
        url = obj.get('url', None)
        force_update = obj.get('force', False)
        try:
            rssi = float(obj['rssi'])
            txpower = float(obj['txpower'])
        except:
            rssi = None
            txpower = None

        def append_invalid():
            metadata_output.append({
                'id': url,
                'url': url
            })

        if url is None:
            append_invalid()
            continue

        parsed_url = urlparse(url)
        if parsed_url.scheme != 'http' and parsed_url.scheme != 'https':
            append_invalid()
            continue

        siteInfo = GetSiteInfoForUrl(url, force_update)

        if siteInfo is None:
            append_invalid()
            continue

        # If the cache is older than 5 minutes, queue a refresh
        if siteInfo.updated_on < datetime.datetime.now() - datetime.timedelta(minutes=5):
            # Updated time to make sure we don't request twice.
            siteInfo.put()
            # Add request to queue.
            taskqueue.add(url='/refresh-url', params={'url': url})

        device_data = {}
        device_data['id'] = url
        device_data['url'] = siteInfo.url
        if siteInfo.title is not None:
            device_data['title'] = siteInfo.title
        if siteInfo.description is not None:
            device_data['description'] = siteInfo.description
        if siteInfo.favicon_url is not None:
            device_data['icon'] = siteInfo.favicon_url
        if siteInfo.jsonlds is not None:
            device_data['json-ld'] = json.loads(siteInfo.jsonlds)
        device_data['rssi'] = rssi
        device_data['txpower'] = txpower
        metadata_output.append(device_data)


    def ReplaceRssiTxPowerWithPathLossAsRank(device_data):
        try:
            path_loss = device_data['txpower'] - device_data['rssi']
            device_data['rank'] = path_loss
        except:
            # This fallback case is for requests which did not include rssi
            # and txpower. We could just not return any rank in this case,
            # but we may have other signals to use in the future, and always
            # returning some rank value for any request type could make client
            # implementations easier.  So lets just return a high fake value.
            device_data['rank'] = 1000.0
        finally:
            # Delete these keys, without error if they don't exist
            device_data.pop('txpower', None)
            device_data.pop('rssi', None)
        return device_data

    print metadata_output
    metadata_output = map(ReplaceRssiTxPowerWithPathLossAsRank, RankedResponse(metadata_output))
    return metadata_output

################################################################################

def RankedResponse(metadata_output):
    def ComputeDistance(obj):
        try:
            rssi = float(obj['rssi'])
            txpower = float(obj['txpower'])
            if rssi == 127 or rssi == 128:
                # TODO: What does rssi 127 mean, compared to no value?
                # According to wiki, 127 is MAX and 128 is INVALID.
                # I think we should just leave 127 to calc distance as usual, so it sorts to the end but before the unknowns
                return None
            path_loss = txpower - rssi
            distance = pow(10.0, path_loss - 41) # TODO: Took this from Hoa's patch, but should confirm accuracy
            return distance
        except:
            return None

    def SortByDistanceCmp(a, b):
        dista, distb = ComputeDistance(a), ComputeDistance(b)
        return cmp(dista, distb)

    metadata_output.sort(SortByDistanceCmp)
    return metadata_output

################################################################################

def GetSiteInfoForUrl(url, force_update):
    siteInfo = models.SiteInformation.get_by_id(url)

    if force_update or siteInfo is None:
        siteInfo = FetchAndStoreUrl(siteInfo, url, force_update)

    return siteInfo

################################################################################

def FetchAndStoreUrl(siteInfo, url, force_update):
    # Index the page
    try:
        result = urlfetch.fetch(url, follow_redirects = False, validate_certificate = True)
    except:
        return StoreInvalidUrl(siteInfo, url)

    if result.status_code == 200: # OK
        encoding = GetContentEncoding(result.content)
        assert result.final_url is None
        # TODO: Use the cache-content headers for storeUrl!
        return StoreUrl(siteInfo, url, result.content, encoding)
    elif result.status_code == 204: # No Content
        # TODO: What do we return?  we want to filter this result out
        pass
    elif result.status_code in [301, 302, 303, 307, 308]: # Moved Permanently, Found, See Other, Temporary Redirect, Permanent Redirect
        final_url = result.headers['location']
        # TODO: Most redirects should not be cached, but we should still check!
        return GetSiteInfoForUrl(final_url, force_update)
    else:
        return StoreInvalidUrl(siteInfo, url)

################################################################################

def GetContentEncoding(content):
    encoding = None
    parser = lxml.etree.HTMLParser(encoding='iso-8859-1')
    htmltree = lxml.etree.fromstring(content, parser)
    value = htmltree.xpath("//head//meta[@http-equiv='Content-Type']/attribute::content")
    if encoding is None:
        if (len(value) > 0):
            content_type = value[0]
            _, params = cgi.parse_header(content_type)
            if 'charset' in params:
                encoding = params['charset']

    if encoding is None:
        value = htmltree.xpath('//head//meta/attribute::charset')
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

################################################################################

def FlattenString(input):
    input = input.strip()
    input = input.replace('\r', ' ');
    input = input.replace('\n', ' ');
    input = input.replace('\t', ' ');
    input = input.replace('\v', ' ');
    input = input.replace('\f', ' ');
    while '  ' in input:
        input = input.replace('  ', ' ');
    return input

################################################################################

def StoreInvalidUrl(siteInfo, url):
    if siteInfo is None:
        siteInfo = models.SiteInformation.get_or_insert(url, 
            url = url,
            title = None,
            favicon_url = None,
            description = None,
            jsonlds = None)
    else:
        # Don't update if it was already cached.
        siteInfo.put()

    return siteInfo

################################################################################

def StoreUrl(siteInfo, url, content, encoding):
    title = None
    description = None
    icon = None

    # parse the content
    parser = lxml.etree.HTMLParser(encoding=encoding)
    htmltree = lxml.etree.fromstring(content, parser)
    value = htmltree.xpath('//head//title/text()');
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
        if icon.startswith('./'):
            icon = icon[2:len(icon)]
        icon = urljoin(url, icon)
    if icon is None:
        icon = urljoin(url, '/favicon.ico')
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
        s_icon = icon
        if s_url is None:
            s_url = '[none]'
        if s_icon is None:
            s_icon = '[none]'
        logging.warning('icon error with ' + s_url + ' -> ' + s_icon)
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
        siteInfo = models.SiteInformation.get_or_insert(url,
            url = url,
            title = title,
            favicon_url = icon,
            description = description,
            jsonlds = jsonlds_data)
    else:
        # update the data because it already exists
        siteInfo.url = url
        siteInfo.title = title
        siteInfo.favicon_url = icon
        siteInfo.description = description
        siteInfo.jsonlds = jsonlds_data
        siteInfo.put()

    return siteInfo

################################################################################

def RefreshUrl(url):
    siteInfo = models.SiteInformation.get_by_id(url)

    if siteInfo is not None:
        # If we've done an update within the last 5 seconds, don't do another one.
        # This is just to prevent abuse, accidental or otherwise
        if siteInfo.updated_on > datetime.datetime.now() - datetime.timedelta(seconds=5):
            logging.info('Skipping RefreshUrl for url: ' + url)
            return

        # Update the timestamp before starting the request
        siteInfo.put()

    siteInfo = FetchAndStoreUrl(siteInfo, url, force_update=False)

################################################################################

def GetConfig():
    import os.path
    if os.path.isfile('config.SECRET.json'):
        fname = 'config.SECRET.json'
    else:
        fname = 'config.SAMPLE.json'
    with open(fname) as configfile:
        return json.load(configfile)

################################################################################
