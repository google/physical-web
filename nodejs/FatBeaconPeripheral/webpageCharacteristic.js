var util = require('util');
var bleno = require('bleno');
var CHARACTERISTIC_UUID = 'd1a517f0-2499-46ca-9ccc-809bc1c966fa';

var WebpageCharacteristic = function() {
  WebpageCharacteristic.super_.call(this, {
    uuid: CHARACTERISTIC_UUID,
    properties: ['read'],
    value: null
  });

  this._value = Buffer.alloc(0);
  this._updateValueCallback = null;
};

util.inherits(WebpageCharacteristic, bleno.Characteristic);

WebpageCharacteristic.prototype.onReadRequest = function(offset, callback) {
  console.log(`Reading - ${this._value}`);
  callback(this.RESULT_SUCCESS, this._value);
};

WebpageCharacteristic.prototype.onWriteRequest = function(data, offset, withoutResponse, callback) {
  this._value = data;

  console.log(`Writing - ${this._value}`);

  if (this._updateValueCallback) {
    console.log('WebpageCharacteristic - onWriteRequest: notifying');

    this._updateValueCallback(this._value);
  }

  if(callback) {
    callback(this.RESULT_SUCCESS);
  }
};

WebpageCharacteristic.prototype.onSubscribe = function(maxValueSize, updateValueCallback) {
  console.log('WebpageCharacteristic - onSubscribe');
  this._updateValueCallback = updateValueCallback;
};

WebpageCharacteristic.prototype.onUnsubscribe = function() {
  console.log('WebpageCharacteristic - onUnsubscribe');
  this._updateValueCallback = null;
};

module.exports = WebpageCharacteristic;
