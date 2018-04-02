var net = require('net');

var HOST = '0.0.0.0';
var PORT = 3000;

var socket;
var previousTemperatureNode0 = Math.floor((Math.random() * 30) + 1);
var previousTemperatureNode1 = Math.floor((Math.random() * 30) + 1);

net.createServer(function (sock) {

  socket = sock;

  // We have a connection - a socket object is assigned to the connection automatically
  console.log('CONNECTED: ' + sock.remoteAddress + ':' + sock.remotePort);

  // Add a 'data' event handler to this instance of socket
  sock.on('data', function (data) {
    console.log('DATA ' + sock.remoteAddress + ': ' + data);
    // Write the data back to the socket, the client will receive it as data from the server
    sock.write('You said "' + data + '"');

  });

  // Add a 'close' event handler to this instance of socket
  sock.on('close', function (data) {
    console.log('CLOSED: ' + sock.remoteAddress + ' ' + sock.remotePort);
  });

  sendWeatherData();

}).listen(PORT, HOST);

function sendWeatherData() {
  setTimeout(function () {
    var negateRandomForNode0 = Math.random() > 0.5? -1: 1;
    var negateRandomForNode1 = Math.random() > 0.5? -1: 1;

    var temperatureForNode0 = previousTemperatureNode0 + (Math.random() * negateRandomForNode0);
    var temperatureForNode1 = previousTemperatureNode1 + (Math.random() * negateRandomForNode1);

    previousTemperatureNode0 = temperatureForNode0;
    previousTemperatureNode1 = temperatureForNode1;

    var windspeed = Math.floor((Math.random() * 30) + 1);

     var JSONString = `{
       "version": 2,
         "numberOfNodes": 2,
         "measurements": [
           {
               "windSpeed": ${windspeed},
               "temperature": ${temperatureForNode0},
               "nodeId": 0
           },
           {
               "windSpeed": ${windspeed + 2},
               "temperature": ${temperatureForNode0 + 2},
               "nodeId": 1
           }
       ]
     }`;

    // var JSONString = `{
    //   "version": 2,
    //     "numberOfNodes": 1,
    //     "measurements": [
    //       {
    //           "windSpeed": ${windspeed},
    //           "temperature": ${temperatureForNode0},
    //           "nodeId": 0
    //       }
    //   ]
    // }`;

    //var JSONString = `${windspeed} ${temperatureForNode0}`;

    //socket.write('start_' + Math.floor((Math.random() * 30) + 1) + ' ' + Math.floor((Math.random() * 30) + 1) + '_end');
    console.log(JSONString);
    socket.write('start_' + JSONString + '_end');
    sendWeatherData();
  }, 1000);
}

console.log('Server listening on ' + HOST + ':' + PORT);
