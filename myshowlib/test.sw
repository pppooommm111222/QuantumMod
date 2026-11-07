x_5
y_5
fx_10
fy_10

loop infinite {
    clear
    show"=== 极简贪吃蛇 ==="
    show"按 W A S D 移动，按 Q 退出"
    show"当前 X 坐标："
    show"x"
    show"当前 Y 坐标："
    show"y"
    
    canvas 15 15
    paint fx fy "*"
    paint x y "@"
    render
    
    key
    
    # 注意：这里全部是多行写法，绝对不会解析错！
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
