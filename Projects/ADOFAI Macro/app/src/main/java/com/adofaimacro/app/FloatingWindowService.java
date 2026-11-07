package com.adofaimacro.app;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class FloatingWindowService extends Service {

    public static final String EXTRA_HIT_TIMES = "hit_times";

    private WindowManager windowManager;
    private View floatingBallView;
    private LinearLayout menuPanel;
    private View triggerButtonView;
    private WindowManager.LayoutParams ballParams;
    private WindowManager.LayoutParams menuParams;
    private WindowManager.LayoutParams triggerParams;

    private List<Double> hitTimes = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createFloatingBall();
        createMenuPanel();
    }

    private int getOverlayType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            return WindowManager.LayoutParams.TYPE_PHONE;
        }
    }

    private void createFloatingBall() {
        floatingBallView = LayoutInflater.from(this).inflate(R.layout.floating_view, null);

        ballParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getOverlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        ballParams.gravity = Gravity.TOP | Gravity.START;
        ballParams.x = 100;
        ballParams.y = 300;

        floatingBallView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = ballParams.x;
                        initialY = ballParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - initialTouchX);
                        int dy = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(dx) > 30 || Math.abs(dy) > 30) {
                            isDragging = true;
                        }
                        ballParams.x = initialX + dx;
                        ballParams.y = initialY + dy;
                        windowManager.updateViewLayout(floatingBallView, ballParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            toggleMenu();
                        }
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(floatingBallView, ballParams);
    }

    private void createMenuPanel() {
        menuPanel = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.floating_menu, null);

        menuParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getOverlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        menuParams.gravity = Gravity.TOP | Gravity.START;
        menuParams.x = 100;
        menuParams.y = 380;

        Button btnStart = menuPanel.findViewById(R.id.btnStart);
        Button btnCalibrate = menuPanel.findViewById(R.id.btnCalibrate);
        Button btnStop = menuPanel.findViewById(R.id.btnStop);
        Button btnHide = menuPanel.findViewById(R.id.btnHide);
        Button btnExit = menuPanel.findViewById(R.id.btnExit);

        btnStart.setOnClickListener(v -> { hideMenu(); showTriggerButton(false); });
        btnCalibrate.setOnClickListener(v -> { hideMenu(); showTriggerButton(true); });
        btnStop.setOnClickListener(v -> { hideMenu(); stopMacro(); });
        btnHide.setOnClickListener(v -> { hideMenu(); hideFloatingBall(); });
        btnExit.setOnClickListener(v -> { hideMenu(); stopSelf(); });
    }

    private void showTriggerButton(boolean calibrateMode) {
        if (triggerButtonView != null && triggerButtonView.getParent() != null) {
            windowManager.removeView(triggerButtonView);
        }

        triggerButtonView = LayoutInflater.from(this).inflate(R.layout.trigger_button, null);
        Button btnTrigger = triggerButtonView.findViewById(R.id.btnTrigger);
        btnTrigger.setText(calibrateMode ? "校准点我" : "开始点我");

        triggerParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getOverlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        triggerParams.gravity = Gravity.TOP | Gravity.END;
        triggerParams.x = 50;
        triggerParams.y = 800;

        triggerButtonView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = triggerParams.x;
                        initialY = triggerParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - initialTouchX);
                        int dy = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(dx) > 30 || Math.abs(dy) > 30) {
                            isDragging = true;
                        }
                        triggerParams.x = initialX - dx;
                        triggerParams.y = initialY + dy;
                        windowManager.updateViewLayout(triggerButtonView, triggerParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!isDragging) {
                            Toast.makeText(FloatingWindowService.this, "按钮被点了", Toast.LENGTH_SHORT).show();
                            onTriggerClicked();
                        }
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(triggerButtonView, triggerParams);
    }

    private void onTriggerClicked() {
        // 现在的硬延时方案不靠这里触发，先留空
        Toast.makeText(this, "按钮已响应，但宏由硬延时启动", Toast.LENGTH_SHORT).show();
        removeTriggerButton();
    }

    private void removeTriggerButton() {
        if (triggerButtonView != null && triggerButtonView.getParent() != null) {
            windowManager.removeView(triggerButtonView);
        }
    }

    private void toggleMenu() {
        if (menuPanel.getParent() != null) {
            hideMenu();
        } else {
            menuParams.x = ballParams.x;
            menuParams.y = ballParams.y + 80;
            windowManager.addView(menuPanel, menuParams);
        }
    }

    private void hideMenu() {
        if (menuPanel.getParent() != null) {
            windowManager.removeView(menuPanel);
        }
    }

    private void hideFloatingBall() {
        if (floatingBallView != null && floatingBallView.getParent() != null) {
            windowManager.removeView(floatingBallView);
        }
        Toast.makeText(this, "悬浮球已隐藏", Toast.LENGTH_SHORT).show();
    }

    private void stopMacro() {
        AutoClickService service = AutoClickService.getInstance();
        if (service != null) {
            service.stopMacro();
        }
        Toast.makeText(this, "宏已停止", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            ArrayList<Double> times = (ArrayList<Double>) intent.getSerializableExtra(EXTRA_HIT_TIMES);
            if (times != null) {
                hitTimes = times;
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingBallView != null && floatingBallView.getParent() != null) {
            windowManager.removeView(floatingBallView);
        }
        if (menuPanel != null && menuPanel.getParent() != null) {
            windowManager.removeView(menuPanel);
        }
        if (triggerButtonView != null && triggerButtonView.getParent() != null) {
            windowManager.removeView(triggerButtonView);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}