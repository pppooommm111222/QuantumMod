const express = require('express');
const nodemailer = require('nodemailer');
const cors = require('cors');
const fs = require('fs');
const http = require('http');
const WebSocket = require('ws');

const app = express();
app.use(express.json());
app.use(cors());

let codes = {}; 
const USERS_FILE = '/sdcard/y12_users.json'; 

if (!fs.existsSync(USERS_FILE)) {
    fs.writeFileSync(USERS_FILE, JSON.stringify([]));
}

// 配置发件邮箱 (⚠️ 必须替换下面这三处为你自己的信息！)
const transporter = nodemailer.createTransport({
    host: 'smtp.139.com',
    port: 465,
    secure: true,
    auth: {
        user: '18231248013@139.com', // ⚠️ 换成你的139邮箱
        pass: '55565fdb141e5d116500'          // ⚠️ 换成你的授权码
    }
});

// 接口1：发送验证码
app.post('/send-code', async (req, res) => {
    const { email } = req.body;
    const code = Math.floor(100000 + Math.random() * 900000).toString();
    codes[email] = { code, expire: Date.now() + 5 * 60 * 1000 };

    try {
        await transporter.sendMail({
            from: '18231248013@139.com', // ⚠️ 换成你的139邮箱
            to: email,
            subject: 'Y12工具箱 - 登录验证码',
            text: `您的验证码是：${code}，5分钟内有效。`
        });
        console.log(`[发送成功] ${email} 的验证码是: ${code}`);
        res.json({ success: true, message: '发送成功' });
    } catch (e) {
        console.error(e);
        res.status(500).json({ success: false, message: '发送失败' });
    }
});

// 接口2：校验验证码
app.post('/verify-code', (req, res) => {
    const { email, code } = req.body;
    const record = codes[email];
    if (record && record.code === code && record.expire > Date.now()) {
        delete codes[email];
        res.json({ success: true, message: '验证成功' });
    } else {
        res.json({ success: false, message: '验证码错误或已过期' });
    }
});

// 接口3：检查邮箱是否已注册
app.post('/check-email', (req, res) => {
    const { email } = req.body;
    let users = JSON.parse(fs.readFileSync(USERS_FILE));
    const user = users.find(u => u.email === email);
    res.json({ success: true, exists: !!user });
});

// 接口4：注册
app.post('/register', (req, res) => {
    const { email, nickname, password } = req.body;
    let users = JSON.parse(fs.readFileSync(USERS_FILE));
    if (users.find(u => u.email === email)) {
        return res.json({ success: false, message: '该邮箱已注册' });
    }
    users.push({ email, nickname, password });
    fs.writeFileSync(USERS_FILE, JSON.stringify(users));
    res.json({ success: true, message: '注册成功' });
});

// 接口5：登录
app.post('/login', (req, res) => {
    const { email, password } = req.body;
    let users = JSON.parse(fs.readFileSync(USERS_FILE));
    const user = users.find(u => u.email === email && u.password === password);
    if (user) {
        res.json({ success: true, nickname: user.nickname });
    } else {
        res.json({ success: false, message: '邮箱或密码错误' });
    }
});

// ================== WebSocket 联机部分（多房间隔离 + 防崩版） ==================
const server = http.createServer(app); 
const wss = new WebSocket.Server({ server });

let rooms = {}; 

wss.on('connection', function connection(ws) {
    ws.roomId = null; 
    ws.nickname = "玩家" + Math.floor(Math.random() * 1000);

    sendRoomListTo(ws);

    ws.on('message', function incoming(message) {
        try {
            let data = JSON.parse(message.toString());

            if (data.type === "join") {
                ws.nickname = data.name;
                sendRoomListTo(ws); // 立即发送房间列表
                return;
            }
            if (data.type === "create_room") {
                let roomId = "room_" + Date.now();
                rooms[roomId] = { id: roomId, name: ws.nickname + " 的房间", owner: ws, clients: [ws] };
                ws.roomId = roomId;
                broadcastRoomList();
                return;
                return;
            }
if (data.type === "join_room") {
    let roomId = data.roomId;
    if (rooms[roomId]) {
        // ⚠️ 核心修复：清理已经存在的同名玩家（防止幽灵连接）
        rooms[roomId].clients = rooms[roomId].clients.filter(c => c.nickname !== ws.nickname);
        
        ws.roomId = roomId;
        rooms[roomId].clients.push(ws);
        broadcastToRoom(roomId, JSON.stringify({ type: "system", msg: ws.nickname + " 加入了房间" }));
        broadcastRoomList();
    }
    return;
}
            if (data.type === "message" || data.type === "action") {
                if (ws.roomId && rooms[ws.roomId]) {
                    broadcastToRoom(ws.roomId, message.toString());
                }
            }
        } catch (e) {
            if (ws.roomId && rooms[ws.roomId]) broadcastToRoom(ws.roomId, message.toString());
        }
    });

    // ⚠️ 防止客户端闪退导致服务器崩溃！
    ws.on('error', function(error) {
        console.log('客户端异常断开:', error.message);
    });

    ws.on('close', function() {
        if (ws.roomId && rooms[ws.roomId]) {
            rooms[ws.roomId].clients = rooms[ws.roomId].clients.filter(c => c !== ws);
            if (rooms[ws.roomId].clients.length === 0) delete rooms[ws.roomId];
            broadcastRoomList();
        }
    });
});

function sendRoomListTo(ws) {
    let list = Object.values(rooms).map(r => ({ id: r.id, name: r.name, count: r.clients.length }));
    ws.send(JSON.stringify({ type: "room_list", rooms: list }));
}
function broadcastRoomList() {
    let list = Object.values(rooms).map(r => ({ id: r.id, name: r.name, count: r.clients.length }));
    wss.clients.forEach(function each(client) {
        if (client.readyState === WebSocket.OPEN && client.roomId === null) {
            client.send(JSON.stringify({ type: "room_list", rooms: list }));
        }
    });
}
function broadcastToRoom(roomId, msg) {
    if (rooms[roomId]) {
        rooms[roomId].clients.forEach(function each(client) {
            if (client.readyState === WebSocket.OPEN) client.send(msg);
        });
    }
}

server.listen(3000, '0.0.0.0', () => {
    console.log('Y12 终极服务器已启动，端口 3000（邮件 + 联机大厅）');
});
