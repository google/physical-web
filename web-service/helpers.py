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

try:
    from google.appengine.api import taskqueue, urlfetch, app_identity
    import models
except Exception as e:
    if __name__ != '__main__':
        raise e
    else:
        print "Warning: import exception '{0}'".format(e)

from urlparse import urljoin, urlparse
import base64
import cgi
import datetime
import json
import logging
import lxml.etree

################################################################################

ENABLE_EXPERIMENTAL = app_identity.get_application_id().endswith('-dev')

################################################################################

def BuildResponse(objects):
    metadata_output = []

    # Resolve the devices
    for obj in objects:
        url = obj.get('url', None)
        force_update = obj.get('force', False)
        rssi = obj.get('rssi', None)
        txpower = obj.get('txpower', None)
        distance = ComputeDistance(rssi, txpower)

        def append_invalid():
            metadata_output.append({
                'id': url,
                'url': url
            })

        if url is None:
            continue

        parsed_url = urlparse(url)
        if parsed_url.scheme != 'http' and parsed_url.scheme != 'https':
            append_invalid()
            continue

        siteInfo = GetSiteInfoForUrl(url, distance, force_update)

        if siteInfo is None:
            continue

        # If the cache is older than 5 minutes, queue a refresh
        updated_ago = datetime.datetime.now() - siteInfo.updated_on
        if updated_ago > datetime.timedelta(minutes=5):
            logging.info('Queue RefreshUrl for url: {0}, which was updated {1} ago'.format(url, updated_ago))
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
        device_data['distance'] = distance
        metadata_output.append(device_data)

    metadata_output = map(ReplaceDistanceWithRank, RankedResponse(metadata_output))
    return metadata_output

################################################################################

def ComputeDistance(rssi, txpower):
    try:
        rssi = float(rssi)
        txpower = float(txpower)
        if rssi == 128:
            # According to wiki, 127 is MAX and 128 is INVALID.
            return None
        path_loss = txpower - rssi
        distance = pow(10.0, (path_loss - 41) / 20)
        return distance
    except:
        return None

def RankedResponse(metadata_output):
    def SortByDistanceCmp(a, b):
        return cmp(a['distance'], b['distance'])

    metadata_output.sort(SortByDistanceCmp)
    return metadata_output

def ReplaceDistanceWithRank(device_data):
    distance = device_data['distance']
    distance = distance if distance is not None else 1000
    device_data['rank'] = distance
    device_data.pop('distance', None)
    return device_data

################################################################################

# This is used to recursively look up in cache after each redirection.
# We don't cache the redirection itself, but we always want to cache the final destination.
def GetSiteInfoForUrl(url, distance=None, force_update=False):
    logging.info('GetSiteInfoForUrl url:{0}, distance:{1}'.format(url, distance))

    siteInfo = models.SiteInformation.get_by_id(url)

    if force_update or siteInfo is None:
        siteInfo = FetchAndStoreUrl(siteInfo, url, distance, force_update)

    return siteInfo

################################################################################

def FetchAndStoreUrl(siteInfo, url, distance=None, force_update=False):
    # Index the page
    try:
        headers = {}
        if ENABLE_EXPERIMENTAL and distance is not None:
            headers['X-PhysicalWeb-Distance'] = distance

        result = urlfetch.fetch(url,
                                follow_redirects=False,
                                validate_certificate=True,
                                headers=headers)
    except:
        return None

    logging.info('FetchAndStoreUrl url:{0}, status_code:{1}'.format(url, result.status_code))
    if result.status_code == 200 and result.content: # OK
        encoding = GetContentEncoding(result.content)
        assert result.final_url is None
        # TODO: Use the cache-content headers for storeUrl!
        return StoreUrl(siteInfo, url, result.content, encoding)
    elif result.status_code == 204: # No Content
        # TODO: What do we return?  we want to filter this result out
        return None
    elif result.status_code in [301, 302, 303, 307, 308]: # Moved Permanently, Found, See Other, Temporary Redirect, Permanent Redirect
        final_url = result.headers['location']
        logging.info('FetchAndStoreUrl url:{0}, redirects_to:{1}'.format(url, final_url))
        if siteInfo is not None:
            logging.info('Removing Stale Cache for url:{0}'.format(url))
            siteInfo.key.delete()
        # TODO: Most redirects should not be cached, but we should still check!
        return GetSiteInfoForUrl(final_url, distance, force_update)
    else:
        return None

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

def StoreUrl(siteInfo, url, content, encoding):
    title = None
    description = None
    icon = None

    # parse the content
    parser = lxml.etree.HTMLParser(encoding=encoding)
    htmltree = lxml.etree.fromstring(content, parser)

    # Try to find web manifest <link rel="manifest" href="...">.
    value = htmltree.xpath("//link[@rel='manifest']/attribute::href")
    if (len(value) > 0):
        # Fetch web manifest.
        manifestUrl = value[0]
        if "://" not in manifestUrl:
            manifestUrl = urljoin(url, manifestUrl)
        try:
            result = urlfetch.fetch(manifestUrl)
            if result.status_code == 200:
                manifestData = json.loads(result.content)
                if 'short_name' in manifestData:
                    title = manifestData['short_name']
                else:
                    title = manifestData['name']
        except:
            pass

    # Try to use <title>...</title>.
    if title is None:
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
        result = urlfetch.fetch(icon, validate_certificate=True)
        if result.status_code != 200:
            icon = None
        elif 'Content-Type' not in result.headers:
            icon = None
        else:
            contentType = result.headers['Content-Type']
            if not contentType.startswith('image/'):
                icon = None
            else:
                icon = 'data:' + contentType + ';base64,' + \
                       base64.b64encode(result.content)
    except Exception as e:
        s_url = url
        s_icon = icon
        if s_url is None:
            s_url = '[none]'
        if s_icon is None:
            s_icon = '[none]'
        logging.warning('Icon error with {0}->{1}: {2}'.format(s_url, s_icon, e))
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
        updated_ago = datetime.datetime.now() - siteInfo.updated_on
        if updated_ago < datetime.timedelta(seconds=5):
            logging.info('Skipping RefreshUrl for url: {0}, which was updated {1} ago'.format(url, updated_ago))
            return

        # Update the timestamp before starting the request, to make sure we do not request twice.
        siteInfo.put()

    FetchAndStoreUrl(siteInfo, url, force_update=True)

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

if __name__ == '__main__':
    for i in range(-22,-100,-1):
        print i, ComputeDistance(i, -22)
