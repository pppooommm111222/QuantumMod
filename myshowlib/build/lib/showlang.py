import sys
import os
import time
import tty
import termios
import re

# 辅助工具：尝试把字符串转换成数字
def auto_convert(env):
    for k in list(env.keys()):
        v = env[k]
        if isinstance(v, str):
            try: env[k] = int(v)
            except ValueError:
                try: env[k] = float(v)
                except ValueError: pass

# 辅助工具：收集大括号 {} 里面的代码块
def collect_block(lines, start_idx):
    block_lines = []
    brace_count = 1
    i = start_idx
    while i < len(lines) and brace_count > 0:
        next_line = lines[i]
        if '{' in next_line: brace_count += 1
        if '}' in next_line: brace_count -= 1
        if brace_count > 0:
            block_lines.append(next_line)
        i += 1
    return '\n'.join(block_lines), i

# 辅助工具：无需回车读键，拦截 Ctrl+C 和 EOF，绝对防死锁
def get_single_key():
    fd = sys.stdin.fileno()
    old_settings = termios.tcgetattr(fd)
    try:
        tty.setraw(sys.stdin.fileno())
        # 注意：这里已经删除了 tcflush，保证能读到你按下的键
        ch = sys.stdin.read(1)
    finally:
        termios.tcsetattr(fd, termios.TCSADRAIN, old_settings)
    
    if ch == '\x03': 
        raise KeyboardInterrupt
    if ch in ('\r', '\n', '\x1b', '\x04'): 
        return ""
    return ch.strip()

# 核心解释器逻辑
def run_show_lang(code, env=None):
    if env is None: env = {}
    lines = code.strip().split('\n')
    i = 0
    
    while i < len(lines):
        line = lines[i].strip()
        i += 1
        
        if not line or line.startswith('#'): 
            continue

        # 1. 定义函数
        if line.startswith('def '):
            func_name = line[4:].split('{')[0].strip()
            block, i = collect_block(lines, i)
            env[func_name] = block

        # 2. 运行函数
        elif line.startswith('run '):
            func_name = line[4:].strip()
            if func_name in env and isinstance(env[func_name], str):
                run_show_lang(env[func_name], env)
            else:
                print(f"找不到函数: {func_name}")

        # 3. 接收输入
        elif line.startswith('ask '):
            var_name = line[4:].strip()
            user_input = input(f"[终端等待输入 {var_name}]: ")
            env[var_name] = user_input

        # 4. 列表
        elif line.startswith('list '):
            parts = line[5:].split('|')
            if len(parts) >= 2:
                target = parts[0].strip()
                items = [item.strip().replace('$', ' ') for item in parts[1:]]
                env[target] = items

        # 5. 专属运算
        elif line.startswith('calc_'):
            rest = line[5:]
            parts = rest.split('_', 1)
            if len(parts) == 2:
                target = parts[0].strip()
                expr = parts[1].strip()
                auto_convert(env)
                try:
                    env[target] = eval(expr, {}, env)
                except Exception as e:
                    print(f"计算错误: {e}")

        # 6. 条件判断
        elif line.startswith('if '):
            cond = line[3:].split('{')[0].strip()
            block, i = collect_block(lines, i)
            auto_convert(env)
            try:
                if eval(cond, {}, env):
                    run_show_lang(block, env)
            except Exception as e:
                print(f"判断错误: {e}")

        # 7. 有限循环
        elif line.startswith('loop ') and not line.startswith('loop infinite'):
            times_str = line[5:].split('{')[0].strip()
            block, i = collect_block(lines, i)
            auto_convert(env)
            try:
                times = int(eval(times_str, {}, env))
                for _ in range(times):
                    run_show_lang(block, env)
            except Exception as e:
                print(f"循环错误: {e}")

        # 8. 无限循环
        elif line.startswith('loop infinite'):
            block, i = collect_block(lines, i)
            while True:
                env['__break__'] = False 
                run_show_lang(block, env)
                if env.get('__break__'):
                    break

        # 9. 跳出循环
        elif line == 'break':
            env['__break__'] = True
            return 

        # 10. 清屏
        elif line == 'clear':
            os.system('clear')

        # 11. 等待
        elif line.startswith('wait '):
            try:
                time.sleep(float(line[5:].strip()))
            except ValueError:
                pass

        # 12. 无回车按键监听
        elif line == 'key':
            env['key'] = get_single_key()

        # 13. 初始化画布
        elif line.startswith('canvas '):
            parts = line.split()
            if len(parts) == 3:
                w_val = env.get(parts[1], parts[1])
                h_val = env.get(parts[2], parts[2])
                try:
                    env['__canvas_w__'] = int(float(w_val))
                    env['__canvas_h__'] = int(float(h_val))
                    env['__canvas_items__'] = {}
                except ValueError:
                    print(f"画布大小错误: {w_val}, {h_val}")

        # 14. 绘制像素点
        elif line.startswith('paint '):
            parts = line.split(maxsplit=3)
            if len(parts) == 4 and '__canvas_items__' in env:
                x_val = env.get(parts[1], parts[1])
                y_val = env.get(parts[2], parts[2])
                char = parts[3].strip('"')
                try:
                    x = int(float(x_val))
                    y = int(float(y_val))
                    env['__canvas_items__'][(x, y)] = char
                except ValueError:
                    print(f"绘制错误: 坐标必须是数字，你给的是 {x_val}, {y_val}")

        # 15. 渲染画布
        elif line == 'render':
            if '__canvas_w__' in env and '__canvas_h__' in env:
                w = env['__canvas_w__']
                h = env['__canvas_h__']
                items = env.get('__canvas_items__', {})
                screen = []
                screen.append("+" + "---" * w + "+")
                for y in range(h, 0, -1):
                    row = "|"
                    for x in range(1, w + 1):
                        char = items.get((x, y), " . ")
                        row += f" {char} "
                    row += "|"
                    screen.append(row)
                screen.append("+" + "---" * w + "+")
                print("\n".join(screen))

        # 16. 变量赋值
        elif '_' in line and not line.startswith('show'):
            k, v = line.split('_', 1)
            env[k.strip()] = v.strip().replace('$', ' ')

        # 17. 打印输出 (终极魔法版)
        elif line.startswith('show'):
            target = line[4:].strip().strip('"')
            if target in env:
                if isinstance(env[target], list):
                    print(" ".join(str(x) for x in env[target]))
                else:
                    print(str(env[target]))
            else:
                # 【魔法】：支持用 @变量名 直接替换变量，并且把 $ 替换成空格
                def replace_var(match):
                    var_name = match.group(1)
                    return str(env.get(var_name, match.group(0)))
                
                final_str = re.sub(r'@(\w+)', replace_var, target)
                print(final_str.replace('$', ' '))

        else:
            print(f"未知指令: {line}")

# 命令行入口
def main():
    try:
        if len(sys.argv) < 2:
            print("ShowLang v2.0 交互式终端 (输入 exit 退出)")
            env = {}
            while True:
                line = input("show> ")
                if line.strip() == 'exit': break
                if not line.strip(): continue
                if '{' in line or '}' in line:
                    print("终端暂不支持多行块，请写入 .sw 文件运行")
                    continue
                run_show_lang(line, env)
            return

        file_name = sys.argv[1]
        if not file_name.endswith('.sw'):
            print("错误: ShowLang 只能运行 .sw 结尾的文件")
            sys.exit(1)
        try:
            with open(file_name, 'r', encoding='utf-8') as f:
                code = f.read()
            run_show_lang(code)
        except FileNotFoundError:
            print(f"找不到文件: {file_name}")
            
    except KeyboardInterrupt:
        os.system('stty sane') 
        print("\n\n[系统] 收到强制结束指令 (Ctrl+C)，已安全退出！")
        sys.exit(0)
    except Exception as e:
        os.system('stty sane')
        print(f"\n运行出错: {e}")

if __name__ == '__main__':
    main()
