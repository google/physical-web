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

from urllib import quote_plus
from urlparse import urljoin, urlparse, urlsplit, urlunsplit
import cgi
import datetime
import json
import logging
import lxml.etree

################################################################################

ENABLE_EXPERIMENTAL = app_identity.get_application_id().endswith('-dev')
PHYSICAL_WEB_USER_AGENT = 'Mozilla/5.0' # TODO: Find a more descriptive string.
BASE_URL = 'https://' + app_identity.get_application_id() + '.appspot.com'
DEFAULT_SECURE_ONLY = False

################################################################################

def BuildResponse(objects, secure_only):
    metadata_output = []
    unresolved_output = []

    # Resolve the devices
    for obj in objects:
        url = obj.get('url', None)
        force_update = obj.get('force', False)
        rssi = obj.get('rssi', None)
        txpower = obj.get('txpower', None)
        distance = ComputeDistance(rssi, txpower)

        def append_invalid():
            #unresolved_output.append({
            #    'id': url
            #})
            pass

        if url is None:
            continue

        parsed_url = urlparse(url)
        if parsed_url.scheme != 'http' and parsed_url.scheme != 'https':
            append_invalid()
            continue

        try:
            siteInfo = GetSiteInfoForUrl(url, distance, force_update)
        except FailedFetchException:
            append_invalid()
            continue

        if siteInfo is None:
            # It's a valid url, which we didn't fail to fetch, so it must be `No Content`
            continue

        scheme, netloc, path, query, fragment = urlsplit(siteInfo.url)
        if fragment == '':
            fragment = parsed_url.fragment
        finalUrl = urlunsplit((scheme, netloc, path, query, fragment))

        if secure_only and scheme != 'https':
            append_invalid()
            continue

        device_data = {}
        device_data['id'] = url
        # TODO: change url to the original url (perhaps minus our goo.gl shortened values)
        device_data['url'] = finalUrl
        # TODO: change displayUrl to the "most applicable" url (resolve shorteners, but perhaps not all redirects)
        device_data['displayUrl'] = finalUrl
        if siteInfo.title is not None:
            device_data['title'] = siteInfo.title
        if siteInfo.description is not None:
            device_data['description'] = siteInfo.description
        if siteInfo.favicon_url is not None:
            device_data['icon'] = urljoin(BASE_URL, '/favicon?url=' + quote_plus(siteInfo.favicon_url))
        if siteInfo.jsonlds is not None:
            device_data['json-ld'] = json.loads(siteInfo.jsonlds)
        device_data['distance'] = distance
        try:
            device_data['groupid'] = ComputeGroupId(siteInfo.url, siteInfo.title, siteInfo.description)
        except Exception as e:
            logging.error('ComputeGroupId url:{0}, title:{1}, description:{2}'.format(siteInfo.url, siteInfo.title, siteInfo.description))

        metadata_output.append(device_data)


    metadata_output = map(ReplaceDistanceWithRank, RankedResponse(metadata_output))

    ret = {
        "metadata": metadata_output,
    }

    if unresolved_output:
        ret["unresolved"] = unresolved_output

    return ret

################################################################################

def ComputeDistance(rssi, txpower):
    try:
        rssi = float(rssi)
        txpower = float(txpower)
        if rssi in [127, 128]: # Known invalid rssi values
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

def ComputeGroupId(url, title, description):
    import hashlib
    domain = urlparse(url).netloc
    if title is not None:
        identifier = title
    elif description is not None:
        identifier = description
    else:
        identifier = urlparse(url).path
    seed = domain + '\0' + identifier
    groupid = hashlib.sha1(seed.encode('utf-8')).hexdigest()[:16]
    return groupid

################################################################################

# This is used to recursively look up in cache after each redirection.
# We don't cache the redirection itself, but we always want to cache the final destination.
def GetSiteInfoForUrl(url, distance=None, force_update=False):
    logging.info('GetSiteInfoForUrl url:{0}, distance:{1}'.format(url, distance))

    siteInfo = None

    if force_update:
        siteInfo = FetchAndStoreUrl(siteInfo, url, distance, force_update)
    else:
        siteInfo = models.SiteInformation.get_by_id(url)

        if siteInfo is None:
            siteInfo = FetchAndStoreUrl(siteInfo, url, distance, force_update)
        else:
            # If the cache is older than 5 minutes, queue a refresh
            updated_ago = datetime.datetime.now() - siteInfo.updated_on
            if updated_ago > datetime.timedelta(minutes=5):
                logging.info('Queue RefreshUrl for url: {0}, which was updated {1} ago'.format(url, updated_ago))
                # Add request to queue.
                taskqueue.add(url='/refresh-url', params={'url': url})

    return siteInfo

################################################################################

class FailedFetchException(Exception):
    pass

def FetchAndStoreUrl(siteInfo, url, distance=None, force_update=False):
    # Index the page
    try:
        headers = {'User-Agent': PHYSICAL_WEB_USER_AGENT}
        if ENABLE_EXPERIMENTAL and distance is not None:
            headers['X-PhysicalWeb-Distance'] = distance

        result = urlfetch.fetch(url,
                                follow_redirects=False,
                                validate_certificate=True,
                                headers=headers)
    except:
        logging.info('FetchAndStoreUrl FailedFetch url:{0}'.format(url))
        raise FailedFetchException()

    logging.info('FetchAndStoreUrl url:{0}, status_code:{1}'.format(url, result.status_code))
    if result.status_code == 200 and result.content: # OK
        encoding = GetContentEncoding(result.content)
        assert result.final_url is None
        # TODO: Use the cache-content headers for storeUrl!
        return StoreUrl(siteInfo, url, result.content, encoding)
    elif result.status_code == 204: # No Content
        return None
    elif result.status_code in [301, 302, 303, 307, 308]: # Moved Permanently, Found, See Other, Temporary Redirect, Permanent Redirect
        final_url = urljoin(url, result.headers['location'])

        scheme, netloc, path, query, fragment = urlsplit(final_url)
        if fragment == '':
            fragment = urlparse(url).fragment
        final_url = urlunsplit((scheme, netloc, path, query, fragment))

        logging.info('FetchAndStoreUrl url:{0}, redirects_to:{1}'.format(url, final_url))
        if siteInfo is not None:
            logging.info('Removing Stale Cache for url:{0}'.format(url))
            siteInfo.key.delete()
        # TODO: Most redirects should not be cached, but we should still check!
        return GetSiteInfoForUrl(final_url, distance, force_update)
    elif 500 <= result.status_code <= 599:
        return None
    else:
        raise FailedFetchException()

################################################################################

def GetContentEncoding(content):
    try:
        # Don't assume server return proper charset and always try UTF-8 first.
        u_value = unicode(content, 'utf-8')
        return 'utf-8'
    except UnicodeDecodeError:
        pass

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

    # TODO(mmocny): Removing until we test for URL == manifestData['start_url']
    # ..will need to adjust to site root.
    #
    ## Try to find web manifest <link rel="manifest" href="...">
    #value = htmltree.xpath("//link[@rel='manifest']/attribute::href")
    #if (len(value) > 0):
    #    # Fetch web manifest.
    #    manifestUrl = value[0]
    #    if "://" not in manifestUrl:
    #        manifestUrl = urljoin(url, manifestUrl)
    #    try:
    #        result = urlfetch.fetch(manifestUrl)
    #        if result.status_code == 200:
    #            manifestData = json.loads(result.content)
    #            if 'short_name' in manifestData:
    #                title = manifestData['short_name']
    #            else:
    #                title = manifestData['name']
    #    except:
    #        pass

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

    # Icon
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

    # json-lds
    jsonlds = []
    value = htmltree.xpath("//head//script[@type='application/ld+json']/text()");
    for jsonldtext in value:
        jsonldobject = None
        try:
            jsonldobject = json.loads(jsonldtext) # Data is not sanitised.
        except (ValueError, UnicodeDecodeError):
            jsonldobject = None
        if jsonldobject is not None:
            jsonlds.append(jsonldobject)

    if (len(jsonlds) > 0):
        jsonlds_data = json.dumps(jsonlds);
    else:
        jsonlds_data = None

    # Add to cache
    if siteInfo is None:
        # Add a new value
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

def FaviconUrl(url):
    # Fetch only favicons for sites we've already added to our database.
    if models.SiteInformation.query(models.SiteInformation.favicon_url==url).count(limit=1):
        try:
            headers = {'User-Agent': PHYSICAL_WEB_USER_AGENT}
            return urlfetch.fetch(url, headers=headers)
        except:
            return None
    return None

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

    try:
        FetchAndStoreUrl(siteInfo, url, force_update=True)
    except FailedFetchException:
        pass


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
