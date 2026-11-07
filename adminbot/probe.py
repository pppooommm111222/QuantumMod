import os, json, botpy
from botpy import Client, Intents, Token
from botpy.message import GroupMessage
class D(Client):
    async def on_group_at_message_create(self, message: GroupMessage):
        ad = getattr(message.author, "__dict__", {})
        open("/data/data/com.termux/files/home/adminbot/authdict.txt","w").write(json.dumps({k:str(v) for k,v in ad.items()}, ensure_ascii=False, indent=2))
        print("dumped", flush=True)
intents = Intents(public_messages=True)
import asyncio
loop = asyncio.new_event_loop(); asyncio.set_event_loop(loop)
client = D(intents=intents)
loop.run_until_complete(client.run(os.environ["QQ_BOT_APPID"], os.environ["QQ_BOT_TOKEN"]))
