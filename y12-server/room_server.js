const WebSocket = require('ws');
const wss = new WebSocket.Server({ port: 3001 });

console.log('Y12 房间服务器已启动，端口 3001');

wss.on('connection', function connection(ws) {
    console.log('有客户端加入了！');

    ws.on('message', function incoming(message) {
        // 收到任何消息，立刻广播给所有连着的客户端
        console.log('收到消息: ' + message.toString());
        wss.clients.forEach(function each(client) {
            if (client.readyState === WebSocket.OPEN) {
                client.send(message.toString());
            }
        });
    });

    ws.on('close', function() {
        console.log('有客户端断开连接');
    });
});
