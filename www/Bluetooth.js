var exec = require('cordova/exec');


module.exports.initialize = function(arg0,success,error){
 exec(success, error, 'Bluetooth', 'initialize', []);

};

module.exports.showpairedDevice = function(arg0,success,error){
 exec(success, error, 'Bluetooth', 'showpairedDevice', []);

};


