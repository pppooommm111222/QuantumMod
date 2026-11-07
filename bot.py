import os
import botpy
from botpy import Client, Intents, Token
from botpy.message import GroupMessage, C2CMessage
from sandbox import run_code

APPID = os.environ.get("QQ_BOT_APPID", "")
TOKEN = os.environ.get("QQ_BOT_TOKEN", "")


def build_text(message) -> str:
    return (getattr(message, "content", "") or "").strip()




class MyClient(Client):
    async def on_group_at_message_create(self, message: GroupMessage):
        code = build_text(message)
        reply = str(run_code(code))
        await self.api.post_group_message(
            group_openid=message.group_openid,
            msg_type=0,
            content=reply,
            msg_id=message.id,
        )

    async def on_c2c_message_create(self, message: C2CMessage):
        code = build_text(message)
        reply = str(run_code(code))
        await self.api.post_c2c_message(
            openid=message.author.user_openid,
            msg_type=0,
            content=reply,
            msg_id=message.id,
        )


intents = Intents(public_messages=True)

import asyncio
loop = asyncio.new_event_loop()
asyncio.set_event_loop(loop)
client = MyClient(intents=intents)
loop.run_until_complete(client.run(APPID, TOKEN))
