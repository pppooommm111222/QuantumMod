import os
import json
import random
import botpy
from botpy import Client, Intents, Token
from botpy.message import GroupMessage, C2CMessage

APPID = os.environ.get("QQ_BOT_APPID", "")
TOKEN = os.environ.get("QQ_BOT_TOKEN", "")
DATA = os.path.expanduser("~/adminbot/activity.json")
NAMES = os.path.expanduser("~/adminbot/names.json")
TOP_N = 5


def load():
    if os.path.exists(DATA):
        try:
            return json.load(open(DATA, encoding="utf-8"))
        except Exception:
            return {}
    return {}

def save(d):
    json.dump(d, open(DATA, "w", encoding="utf-8"), ensure_ascii=False, indent=2)

def load_names():
    if os.path.exists(NAMES):
        try:
            return json.load(open(NAMES, encoding="utf-8"))
        except Exception:
            return {}
    return {}

def show_name(uid):
    nm = load_names()
    if uid in nm:
        return nm[uid]
    return "成员_" + uid[:6]


def build_text(message):
    return (getattr(message, "content", "") or "").strip()


class AdminBot(Client):
    async def on_group_at_message_create(self, message: GroupMessage):
        uid = getattr(message.author, "member_openid", "") or ""
        cmd = build_text(message)

        d = load()
        d.setdefault(uid, {"name": show_name(uid), "count": 0})
        d[uid]["name"] = show_name(uid)
        d[uid]["count"] += 1
        save(d)

        if cmd == "/我的ID":
            await self.api.post_group_message(
                group_openid=message.group_openid, msg_type=0,
                content=f"你的群内ID是：\n{uid}", msg_id=message.id)
        elif cmd == "/活跃":
            rank = sorted(d.items(), key=lambda x: x[1]["count"], reverse=True)[:10]
            if not rank:
                text = "还没有数据～"
            else:
                lines = ["📊 活跃度 TOP10："]
                for i, (uid_, v) in enumerate(rank, 1):
                    lines.append(f"{i}. {v['name']}（{uid_[:6]}） —— {v['count']} 条")
                text = "\n".join(lines)
            await self.api.post_group_message(
                group_openid=message.group_openid, msg_type=0, content=text, msg_id=message.id)
        elif cmd == "/选管理":
            if not d:
                text = "还没有数据，选不了～"
            else:
                top = sorted(d.items(), key=lambda x: x[1]["count"], reverse=True)[:TOP_N]
                pick = random.choice(top)
                text = (f"🎉 候选池（前{TOP_N}）：\n" +
                        "\n".join(f"· {v['name']}（{uid_[:6]}）" for uid_, v in top) +
                        f"\n\n抽到：{pick[1]['name']} 🏆")
                save({})
            await self.api.post_group_message(
                group_openid=message.group_openid, msg_type=0, content=text, msg_id=message.id)


intents = Intents(public_messages=True)
import asyncio
loop = asyncio.new_event_loop()
asyncio.set_event_loop(loop)
client = AdminBot(intents=intents)
loop.run_until_complete(client.run(APPID, TOKEN))
