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
 var SCAN_TIMEOUT = 4000;
(function(){
	var scanning = false;
	var urls = {};
	Object.defineProperty(window, "scanning", {
		get: function () {
			return scanning;
		},
		set: function(value){
			scanning = value;
			if(scanning){
				document.body.dataset.state = "scanning";
				document.body.dataset.result = "";
			}
			else {
				document.body.dataset.state = "";
			}
		}
	});
	
	Object.defineProperty(window, "urls", {
		get: function () {
			return urls;
		},
		set: function(value){
			urls = value;
		}
	});
})();

document.addEventListener("receiveUrl",function(evt){
	console.log("receiveUrl",evt);
	if(scanning){
		var url = evt.detail && evt.detail.url;
		url && (urls[url] = url);
	};
});
document.addEventListener("requestRefresh",function(evt){
	console.log("requestRefresh",evt);
	startScan(SCAN_TIMEOUT);
});
var requestUrlMetaData = function(urls){
	var objects = [];
	urls = typeof urls == "string"?[urls]:urls;
	for(var i in urls){
		objects.push({
			url: urls[i],
			txpower: 0,
			rssi: 0
		})
	};
	return $.ajax({
		dataType: "json",
		type: "POST",
		url: "http://url-caster.appspot.com/resolve-scan",
		contentType: "application/json; charset=utf-8",
		data: JSON.stringify({
			objects: objects
		})
	});
};

var addUrlMetaData = function(array){
	for(var i in array){
		var data = array[i];
		var url = data.url;
		var desc = data.description;
		var title = data.title;
		var id = data.id;
		var icon = data.icon;
		var tmpl = document.querySelector('#listItemTemplate').content;
		//tmpl.querySelector('h3').style["background-image"] = "url("+icon+")";
		tmpl.querySelector('a').href = url;
		tmpl.querySelector('a').onclick = function(){
			console.log(this);
		};
		tmpl.querySelector('a').innerText = title || url || "";
		tmpl.querySelector('cite').innerText = url || "";
		tmpl.querySelector('p').innerHTML = desc || "";
		var copy = document.importNode(tmpl, true);
		copy.querySelector('a').onclick = function(){
			chrome.app.window.current().minimize();
		};
		document.querySelector('#list').appendChild(copy);
	}
};

var addUrls = function(urls){
	for(var i in urls){
		var url = urls[i];
		var tmpl = document.querySelector('#listItemTemplate').content;
		tmpl.querySelector('a').innerText = url || "";
		tmpl.querySelector('cite').innerText = url || "";
		var copy = document.importNode(tmpl, true);
		copy.querySelector('a').onclick = function(){
			chrome.app.window.current().minimize();
		};
		document.querySelector('#list').appendChild(copy);
	}
};

var setScanning = function(val){
	scanning = val;
};

var startScan = function(timeout){
	if(scanning) return;
	document.querySelector('#list').innerHTML = "";
	scanning = true;
	urls = {};
	phyweb && phyweb.startScan(timeout);
	setTimeout(function(){
		phyweb && phyweb.stopScan();
		requestUrlMetaData(urls).done(function(data){
			addUrlMetaData(data.metadata);
			document.body.dataset.result = data.metadata.length;
		}).fail(function(){
			console.error("error on request url meta data",arguments);
			addUrls(urls);
			document.body.dataset.result = Object.keys(urls).length;
		}).always(function(){
			urls = {};
			scanning = false;
		});
	},timeout);
};
$(function(){
	document.getElementById("closeBtn").onclick = function(){
		chrome.app.window.current().close();
	};
	document.getElementById("minimizeBtn").onclick = function(){
		chrome.app.window.current().minimize();
	};
	document.getElementById("refreshBtn").onclick = function(){
		startScan(SCAN_TIMEOUT);
	};
	startScan(SCAN_TIMEOUT);
});