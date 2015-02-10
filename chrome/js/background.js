var peer = null;
var phyweb = {};
phyweb.startScan = function(){
	peer && peer.search({
		ST: "urn:physical-web-org:device:Basic:1",
		MX: "4"
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
		resizable: false,
		focused: true,
		frame: "none"
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