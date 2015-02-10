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
	console.log("receiveUrl",evt);
	startScan(4000);
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
		tmpl.querySelector('a').innerText = title || "";
		tmpl.querySelector('cite').innerText = url || "";
		tmpl.querySelector('p').innerHTML = desc || "";
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
		startScan(4000);
	};
	startScan(4000);
});