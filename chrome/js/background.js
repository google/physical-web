/*
 * Copyright 2014 Fraunhofer FOKUS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * AUTHOR: Louay Bassbouss <louay.bassbouss@fokus.fraunhofer.de>
 *
 */
var peer = null;
var phyweb = {};
phyweb.startScan = function(timeout){
	var MX = parseInt(timeout/1000);
	peer && peer.search({
		ST: "urn:physical-web-org:device:Basic:1",
		MX: MX+""
	},function(info){
		console.log("search callback",info);
	});
};
phyweb.stopScan = function(){
	// nothing to do for now
};

chrome.contextMenus.create({
	id: 'refresh',
	title: "Refresh",
	contexts: ['all']
});
chrome.contextMenus.onClicked.addListener(function(info){
	var win = chrome.app.window.get("index");
	if(win && info.menuItemId == "refresh"){
		var contentWin = win.contentWindow;
		var doc = contentWin.document;
		var evt = new contentWin.CustomEvent("requestRefresh");
		doc.dispatchEvent(evt);
	}
});

var openWin = function(settings){
	chrome.app.window.create('index.html', {
		id: "index",
		state: "normal",
		alwaysOnTop: true,
		resizable: true,
		focused: true,
		frame: "none",
		innerBounds: {
			left: 100,
			top: 100,
			width: 400,
			height: 500,
			minWidth: 300,
			minHeight: 400,
			maxWidth: 500,
			maxHeight: 600
		}
	},function(win){
		var contentWin = win.contentWindow;
		contentWin.phyweb = phyweb;
	});
};

var setupSSDP = function(){
	if(peer){
		peer.close();
		peer = null;
	}
	peer = ssdp.createPeer();
	peer.on("ready",function(){
		console.log("peer ready");
	});
	peer.on("error",function(info){
		console.log("error ", info);
	});
	peer.on("found",function(headers,sender){
		console.log("receive response ",headers," from ",sender);
		var win = chrome.app.window.get("index");
		if(win){
			var contentWin = win.contentWindow;
			var doc = contentWin.document;
			var evt = new contentWin.CustomEvent("receiveUrl",{
				detail: {
					url: headers["LOCATION"]
				}
			});
			doc.dispatchEvent(evt);
		}
	});
	peer.on("notify",function(headers,sender){
		console.log("receive notification ",headers," from ",sender);
	});
	peer.on("close",function(){
		console.log("close peer");
	});
	peer.start(true);
};
setupSSDP();
chrome.app.runtime.onLaunched.addListener(function() {
	openWin();
});