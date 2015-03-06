## UPnP/SSDP Support

[UPnP/SSDP][upnp-ssdp] Support in Physical Web addresses the same use cases as for [mDNS Support](mDNS_Support.md),
but uses a different protocol to advertise and receive URLs. There are different options to support UPnP or 
SSDP in Physical Web as described in the subsections below. The current Android implementation supports the [SSDP Only](#ssdp-only) option (please refer to the
[Discussion](#discussion) section for more details about this decision). The [Physical Web Advertiser](https://github.com/fraunhoferfokus/phyweb)
is a cross platform command line tool based on [Node.js](http://nodejs.org/) that can be used to advertise URLs over SSDP. The new [Physical Web Chrome App](../chrome) supports also SSDP. 

> SSDP support for iOS is work in progress

### SSDP Only

* Ideas
    * define new device type for Physical Web devices e.g. `urn:physical-web-org:device:Basic:1`
    * Use `LOCATION` property in SSDP messages to advertise the URL of the web page instead 
      of URL of the XML device description
    * Search targets properties `NT` or `ST` are always `urn:physical-web-org:device:Basic:1`

* New Physical Web device appears in the network

    ```
    NOTIFY * HTTP/1.1
    HOST: 239.255.255.250:1900
    CACHE-CONTROL: max-age = seconds until advertisement expires
    LOCATION: URL of the web page to advertise
    NT: urn:physical-web-org:device:Basic:1
    NTS: ssdp:alive
    SERVER: OS/version UPnP/1.0 product/version
    USN: advertisement UUID 
    ```

* Physical Web device disappears from the network

    ```
    NOTIFY * HTTP/1.1
    HOST: 239.255.255.250:1900
    NT: urn:physical-web-org:device:Basic:1
    NTS: ssdp:byebye
    USN: uuid:advertisement UUID 
    ```

* Physical Web Browser searches for Physical Web devices in the network

    ```
    M-SEARCH * HTTP/1.1
    HOST: 239.255.255.250:1900
    MAN: "ssdp:discover"
    MX: seconds to delay response
    ST: urn:physical-web-org:device:Basic:1
    ```

* Physical Web device responds to search request

    ```
    HTTP/1.1 200 OK
    CACHE-CONTROL: max-age = seconds until advertisement expires
    DATE: when response was generated
    EXT:
    LOCATION: URL of the web page to advertise
    SERVER: OS/version UPnP/1.0 product/version
    ST: urn:physical-web-org:device:Basic:1
    USN: advertisement UUID
    ```

### UPnP Basic Device
* Ideas 
    * Use UPnP Basic Device Profile. `<deviceType>` is `urn:schemas-upnp-org:device:Basic:1`
    * Use `<presentationURL>` element of the XML device description for the 
      URL of the web page to advertise. If `<presentationURL>` element is not
      set, device will be ignored
    * Additionaly the `<friendlyName>` and `<icon><url>` elements of the device 
      description can be used for title and icon location

* UPnP Device Description

    ```
    <?xml version="1.0"?>
    <root xmlns="urn:schemas-upnp-org:device-1-0">
    	<specVersion>
    		<major>1</major>
    		<minor>0</minor>
    	</specVersion>
    	<URLBase>base URL for all relative URLs</URLBase>
    	<device>
    		<deviceType>urn:schemas-upnp-org:device:Basic:1</deviceType>
    		<!-- The friendlyName element is the best place to put 
    		     the title to display in the Physical Web Browser -->
    		<friendlyName>short user-friendly title</friendlyName>
    		<manufacturer>manufacturer name</manufacturer>
    		<manufacturerURL>URL to manufacturer site</manufacturerURL>
    		<modelDescription>long user-friendly title</modelDescription>
    		<modelName>model name</modelName>
    		<modelNumber>model number</modelNumber>
    		<modelURL>URL to model site</modelURL>
    		<serialNumber>manufacturer's serial number</serialNumber>
    		<UDN>uuid:UUID</UDN>
    		<UPC>Universal Product Code</UPC>
    		<iconList>
    			<icon> 
    				<mimetype>image/format</mimetype>
    				<width>horizontal pixels</width>
    				<height>vertical pixels</height>
    				<depth>color depth</depth>
    				<!-- The icon -> url element is the best place 
    				     to put the location of the icon to display 
    				     in the Physical Web Browser -->
    				<url>URL to icon</url>
    			</icon>
    		</iconList>
    		<!-- The presentationURL element is the best place to 
    		     put the URL to broadcast -->
    		<presentationURL>URL for presentation</presentationURL>
    	</device>
    </root> 
    ```

### Any UPnP Device with `<presentationUrl>`

* Ideas 
    * Same usage as for UPnP Basic device but consider all UPnP 
      devices in the network that have a `<presentationURL>` element. 
      If `<presentationURL>` element is not set, device will be ignored
    * Use `upnp:rootdevice` to search for all UPnP devices in the network
    * UPnP Basic Devices will be also considered since they are also UPnP root devices

## Discussion

* [SSDP Only](#ssdp-only) option is better in term of saving computing resources 
  on the mobile device. The Physical Web Browser needs to send only one SSDP 
  message that contains the search request and fetch the value of the `LOCATION`
  property from each received SSDP response message. Disadvantage is that the 
  `LOCATION` property is not used as specified in SSDP for the URL of the 
  device description XML.
* Options [UPnP Basic Device](#upnp-basic-device) and 
  [Any UPnP Device](#any-upnp-device-with-lt-presentationurl-gt) use `LOCATION`
  property of the SSDP messages as specified in UPnP for the URL of the device 
  description xml. This means for each found device the Physical Web Browser 
  needs to send and additional request (HTTP GET) to get and parse the XML
  device description and then fetch the `<presentationUrl>` element

[upnp-ssdp]: http://upnp.org/sdcps-and-certification/standards/device-architecture-documents/ 