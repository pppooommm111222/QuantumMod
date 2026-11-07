import sys, io, traceback, contextlib

def run_code(code: str, timeout: float = 5.0) -> str:
    buf = io.StringIO()
    original = {}
    try:
        g = {"__name__": "__sandbox__"}
        with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
            exec(compile(code, "<user>", "exec"), g)
        out = buf.getvalue()
        if not out.strip():
            return "(代码执行成功，但没有输出)"
        return out
    except Exception:
        return "执行出错：\n" + traceback.format_exc()
