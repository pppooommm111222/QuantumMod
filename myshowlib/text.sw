x_5
y_5
fx_10
fy_10

loop infinite {
    clear
    show"=== 极简贪吃蛇 (画面版) ==="
    show"按 W A S D 移动，按 Q 退出"
    
    # 使用刚刚的魔法：@x 和 @y 会直接变成数字！
    show"当前坐标: X=@x, Y=@y"
    
    canvas 15 15
    paint fx fy "*"
    paint x y "@"
    render
    
    key
    
    # 注意：这里一定要用多行的大括号，单行 {} 会导致解析Bug！
    if key == "w" {
        calc_y_y+1
    }
    if key == "s" {
        calc_y_y-1
    }
    if key == "a" {
        calc_x_x-1
    }
    if key == "d" {
        calc_x_x+1
    }
    
    if key == "q" {
        break
    }
    
    wait 0.1
}
clear
show"游戏结束！"
