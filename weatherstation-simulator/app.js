var net = require('net');

var HOST = '0.0.0.0';
var PORT = 3000;

var socket;

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
        socket.write('start_' + Math.floor((Math.random() * 30) + 1) + ' ' + Math.floor((Math.random() * 30) + 1) + '_end');
        sendWeatherData();
    }, 1000);
}

console.log('Server listening on ' + HOST + ':' + PORT);
