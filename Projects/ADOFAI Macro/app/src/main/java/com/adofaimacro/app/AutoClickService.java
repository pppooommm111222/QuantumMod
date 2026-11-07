package com.adofaimacro.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

import java.util.List;

public class AutoClickService extends AccessibilityService {
	
	private static AutoClickService instance;
	private Handler handler = new Handler(Looper.getMainLooper());
	private boolean isRunning = false;
	private double userOffset = 0;
	
	public static AutoClickService getInstance() {
		return instance;
	}
	
	public void setUserOffset(double offset) {
		this.userOffset = offset;
	}
	
	@Override
	public void onServiceConnected() {
		super.onServiceConnected();
		instance = this;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		instance = null;
	}
	
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
	}
	
	@Override
	public void onInterrupt() {
	}
	
	public void performClick(float x, float y) {
		Path path = new Path();
		path.moveTo(x, y);
		GestureDescription.Builder builder = new GestureDescription.Builder();
		builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 50));
		dispatchGesture(builder.build(), null, null);
	}
	
	public void startMacroFrom(final List<Double> hitTimes, final float x, final float y, final long startTime) {
		if (isRunning) return;
		isRunning = true;
		
		new Thread(() -> {
			for (Double time : hitTimes) {
				if (!isRunning) break;
				long targetTime = startTime + time.longValue() + (long) userOffset;
				long waitTime = targetTime - System.currentTimeMillis();
				if (waitTime > 0) {
					try { Thread.sleep(waitTime); } catch (InterruptedException ignored) {}
				}
				handler.post(() -> performClick(x, y));
			}
			isRunning = false;
		}).start();
	}
	
	public void startMacro(final List<Double> hitTimes, final float x, final float y) {
		startMacroFrom(hitTimes, x, y, System.currentTimeMillis());
	}
	
	public void stopMacro() {
		isRunning = false;
	}
}