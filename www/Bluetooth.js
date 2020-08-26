var exec = require('cordova/exec');


module.exports.initialize = function(arg0,success,error){
 exec(success, error, 'Bluetooth', 'initialize', []);

};

module.exports.showpairedDevice = function(arg0,success,error){
 exec(success, error, 'Bluetooth', 'showpairedDevice', []);

};

module.exports.findBluetoothDevice = function(arg0,success,error){
 exec(success, error, 'Bluetooth', 'findBluetoothDevice', []);

};

module.exports.pairDevice = function(arg0,success,error){
 exec(success, error, 'Bluetooth', 'pairDevice', [arg0]);

};

module.exports.disconnectBle = function(arg0,success,error){
    exec(success, error, 'Bluetooth', 'disconnectBle', []);
   
   };



