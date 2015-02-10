/*******************************************************************************
 * 
 * Copyright (c) 2013 Louay Bassbouss, Fraunhofer FOKUS, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library. If not, see <http://www.gnu.org/licenses/>. 
 * 
 * AUTHORS: Louay Bassbouss (louay.bassbouss@fokus.fraunhofer.de)
 *
 ******************************************************************************/
(function(exports){
	var SSDP_ADDRESS = "239.255.255.250";
	var SSDP_PORT = 1900;
	var SSDP_HOST = SSDP_ADDRESS + ":" + SSDP_PORT;
	var MAX_AGE = "max-age=1800";
	var TTL = 128;
	var MX = 2;
	var ALIVE = "ssdp:alive";
	var BYEBYE = "ssdp:byebye";
	var UPDATE = "ssdp:update";
	var TYPE_M_SEARCH = "M-SEARCH";
	var TYPE_NOTIFY = "NOTIFY";
	var TYPE_200_OK = "200 OK";
	var TEXT_ENCODER = new TextEncoder();
	var Peer = function (options) {
		EventEmitter.call(this);
		var self = this;
		var mcSocketId = null;
		var ucSocketId = null;
		var onReceive = null;
		var onReceiveError = null;
		var checkSockets = function(){
			if(mcSocketId && ucSocketId){
				self.emit("ready");
			}
			else if(!mcSocketId && !ucSocketId){
				self.emit("close");
			}
		};
		Object.defineProperty(this, "mcSocketId", {
			get: function () {
				return mcSocketId;
			},
			set: function(value){
				mcSocketId = value;
				checkSockets();
			}
		});
		Object.defineProperty(this, "ucSocketId", {
			get: function () {
				return ucSocketId;
			},
			set: function(value){
				ucSocketId = value;
				checkSockets();
			}
		});
		Object.defineProperty(this, "onReceive", {
			get: function () {
				return onReceive;
			},
			set: function(value){
				onReceive = value;
			}
		});
		Object.defineProperty(this, "onReceiveError", {
			get: function () {
				return onReceiveError;
			},
			set: function(value){
				onReceiveError = value;
			}
		});
	};
	Peer.prototype = new EventEmitter;

	/**
	 * start the SSDP listening
	 */
	Peer.prototype.start = function (onlyUC) {
		var self = this;
		!self.onReceive && chrome.sockets.udp.onReceive.addListener(self.onReceive = function(info) {
			if (info.socketId == self.mcSocketId || info.socketId == self.ucSocketId){
				var data = String.fromCharCode.apply(null, new Uint8Array(info.data));
				var req = deserialize(data);
				self.emit(req.type, req.headers, {
					address: info.remoteAddress,
					port: info.remotePort
				});	
			}
		});
		!self.onReceiveError && chrome.sockets.udp.onReceiveError.addListener(self.onReceiveError = function(info) {
			if (info.socketId == self.mcSocketId || info.socketId == self.ucSocketId){
				self.emit("error", info);	
			}
		});
		if(onlyUC != true){
			// Create mc Socket
			!self.mcSocketId && chrome.sockets.udp.create({}, function(socketInfo) {
				var socketId = socketInfo.socketId;
				// Setup event handler and bind socket.
				chrome.sockets.udp.setMulticastTimeToLive(socketId, TTL, function(resultCode){
					if (resultCode < 0) {
						self.emit("error", {
							socketId: socketId,
							resultCode: resultCode
						});
					}
				});
				chrome.sockets.udp.setMulticastLoopbackMode(socketId, true, function(resultCode){
					if (resultCode < 0) {
						self.emit("error", {
							socketId: socketId,
							resultCode: resultCode
						});
					}
				});
				chrome.sockets.udp.bind(socketId,"0.0.0.0", SSDP_PORT, function(resultCode) {
					if (resultCode < 0) {
						self.emit("error", {
							socketId: socketId,
							resultCode: resultCode
						});
						return;
					}
					chrome.sockets.udp.joinGroup(socketId, SSDP_ADDRESS, function(resultCode){
						if (resultCode < 0) {
							self.emit("error", {
								socketId: socketId,
								resultCode: resultCode
							});
						}
						self.mcSocketId = socketId;
					});
				});
			});
		}
		// Create uc Socket
		!self.ucSocketId && chrome.sockets.udp.create({}, function(socketInfo) {
			var socketId = socketInfo.socketId;
			// Setup event handler and bind socket.
			chrome.sockets.udp.setMulticastTimeToLive(socketId, TTL, function(resultCode){
				if (resultCode < 0) {
					self.emit("error", {
						socketId: socketId,
						resultCode: resultCode
					});
				}
			});
			chrome.sockets.udp.bind(socketId,"0.0.0.0", 0, function(resultCode) {
				if (resultCode < 0) {
					self.emit("error", {
						socketId: socketId,
						resultCode: resultCode
					});
					return;
				}
				self.ucSocketId = socketId;
			});
			
		});
		return this;
	};
	
	/**
	 * pause receive messages on all sockets
	 */
	Peer.prototype.pause = function (callback) {
		var counter = 0;
		this.mcSocketId && chrome.sockets.udp.setPaused(this.mcSocketId, true, function(){
			(++counter>1) && (typeof callback == "function") && callback.call(null);
		}) || (++counter);
		this.ucSocketId && chrome.sockets.udp.setPaused(this.ucSocketId, true, function(){
			(++counter>1) && (typeof callback == "function") && callback.call(null);
		});
	};
	
	/**
	 * resume receive messages on all sockets
	 */
	Peer.prototype.resume = function (callback) {
		var counter = 0;
		this.mcSocketId && chrome.sockets.udp.setPaused(this.mcSocketId, false, function(){
			(++counter>1) && (typeof callback == "function") && callback.call(null);
		}) || (++counter);
		this.ucSocketId && chrome.sockets.udp.setPaused(this.ucSocketId, false, function(){
			(++counter>1) && (typeof callback == "function") && callback.call(null);
		});
	};
	
	/**
	 * close the SSDP listening.
	 */
	Peer.prototype.close = function () {
		this.onReceive && chrome.sockets.udp.onReceive.removeListener(this.onReceive) && (this.onReceive = null);
		this.onReceiveError && chrome.sockets.udp.onReceiveError.removeListener(this.onReceiveError) && (this.onReceiveError = null);
		this.mcSocketId && chrome.sockets.udp.close(this.mcSocketId, function(){
			this.mcSocketId = null;
		});
		this.ucSocketId && chrome.sockets.udp.close(this.ucSocketId, function(){
			this.ucSocketId = null;
		});
	};

	/**
	 * notify a SSDP message
	 * @param headers
	 * @param  callback
	 */
	Peer.prototype.notify = function (headers, callback) {
		headers['HOST'] = headers['HOST'] || SSDP_HOST;
		headers['CACHE-CONTROL'] = headers['CACHE-CONTROL'] || MAX_AGE;
		headers['EXT'] = headers['EXT'] || "";
		headers['DATE'] = headers['DATE'] || new Date().toUTCString();
		var data = encode(serialize(TYPE_NOTIFY + " * HTTP/1.1", headers));
		this.mcSocketId && chrome.sockets.udp.send(this.mcSocketId, data, SSDP_ADDRESS, SSDP_PORT, function(sendInfo){
			(typeof callback == "function") && callback.call(null, sendInfo);
		});
	};

	/**
	 * notify an SSDP alive message
	 */
	Peer.prototype.alive = function (headers, callback) {
		headers['NTS'] = ALIVE;
		this.notify(headers, callback);
	};

	/**
	 * notify an SSDP byebye message
	 */
	Peer.prototype.byebye = function (headers, callback) {
		headers['NTS'] = BYEBYE;
		this.notify(headers, callback);
	};

	/**
	 * notify an SSDP update message
	 */
	Peer.prototype.update = function (headers, callback) {
		headers['NTS'] = UPDATE;
		this.notify(headers, callback);
	};

	/**
	 * 
	 */
	Peer.prototype.search = function (headers, callback) {
		headers['HOST'] = headers['HOST'] || SSDP_HOST;
		headers['MAN'] = '"ssdp:discover"';
		headers['MX'] = headers['MX'] || MX;
		var data = encode(serialize(TYPE_M_SEARCH + " * HTTP/1.1", headers));
		this.ucSocketId && chrome.sockets.udp.send(this.ucSocketId,data,SSDP_ADDRESS, SSDP_PORT, function(sendInfo){
			(typeof callback == "function") && callback.call(null, sendInfo);
		});
	};

	/**
	 * 
	 */
	Peer.prototype.reply = function (headers, address, callback) {
		headers['HOST'] = headers['HOST'] || SSDP_HOST;
		headers['CACHE-CONTROL'] = headers['CACHE-CONTROL'] || MAX_AGE;
		headers['EXT'] = headers['EXT'] || "";
		headers['DATE'] = headers['DATE'] || new Date().toUTCString();
		var data = encode(serialize("HTTP/1.1 " + TYPE_200_OK, headers));
		this.ucSocketId && chrome.sockets.udp.send(this.ucSocketId, data, address.address, address.port, function(sendInfo){
			(typeof callback == "function") && callback.call(null, sendInfo);
		});
	};

	var serialize = function (head, headers) {
		var ret = head + "\r\n";

		Object.keys(headers).forEach(function (n) {
			ret += n + ": " + headers[n] + "\r\n";
		});
		ret += "\r\n"
		return ret;
	};

	var encode = function(str){
		var arr = TEXT_ENCODER.encode(str);
		var data = new ArrayBuffer(arr.length);
		var uint8  = new Uint8Array(data);
		uint8.set(arr);
		return data;
	};
	
	var deserialize = function (msg) {
		var lines = msg.toString().split('\r\n');
		var line = lines.shift();
		var headers = {};
		var type = null;
		if (line.match(/HTTP\/(\d{1})\.(\d{1}) (\d+) (.*)/)) {
			type = "found";
		} else {
			var t = line.split(' ')[0]
			type = (t == TYPE_M_SEARCH) ? "search" : (t == TYPE_NOTIFY ? "notify" : null);
		}
		lines.forEach(function (line) {
			if (line.length) {
				var vv = line.match(/^([^:]+):\s*(.*)$/);
				headers[vv[1].toUpperCase()] = vv[2];
			}
		});
		return {
			type: type,
			headers: headers
		};
	};

	/**
	 * create an new SSDP Peer
	 */
	exports.createPeer = function (options) {
		var peer = new Peer(options);
		return peer;
	};

	exports.ALIVE = ALIVE;
	exports.BYEBYE = BYEBYE;
	exports.UPDATE = UPDATE;
})(window.ssdp = window.ssdp || {});